package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.data.dto.DatFileQuery;
import com.erp.data.dto.DatFileVO;
import com.erp.data.entity.DatFile;
import com.erp.data.service.DatFileService;
import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WebDAV 协议处理器（OPTIONS/PROPFIND/GET/PUT/MKCOL/DELETE/MOVE/LOCK/UNLOCK）。
 *
 * <p>继承 {@link HttpServlet} 并由 {@link WebDavServletRegistration} 注册到 /webdav/*。
 * <b>必须用原生 Servlet 而非 Spring MVC 的 @RequestMapping / HttpRequestHandler</b>：
 * Spring 的 DispatcherServlet 继承 FrameworkServlet，其 service() 只对标准 HTTP 方法
 * （GET/POST/PUT/DELETE/OPTIONS/TRACE/PATCH/HEAD）分发，PROPFIND/MKCOL/LOCK 等非标准
 * 方法会落到 HttpServlet.service() 默认实现，返回 400 Bad Request，请求根本到不了
 * HandlerMapping/Controller。原生 Servlet 重写 service() 自行按 request.getMethod()
 * 分发，绕开此限制。
 *
 * <p>仍受 Spring Security 的 webdav FilterChain 保护（filter 在 servlet 之前执行），
 * Basic Auth 鉴权不受影响。当前用户从 SecurityContext 取 username 后调
 * {@link UserDetailsLoader} 重加载（复刻 CurrentUserArgumentResolver 逻辑）。
 */
@Component
@RequiredArgsConstructor
public class WebDavController extends HttpServlet {

    private final WebDavPathResolver resolver;
    private final DatFileService fileService;
    private final WebDavPropFindXmlBuilder xmlBuilder;
    private final WebDavLockStore lockStore;
    private final JdbcTemplate jdbcTemplate;
    private final UserDetailsLoader userDetailsLoader;

    // ========== 单入口分发：重写 service() 接收所有 HTTP 方法（含 PROPFIND 等） ==========
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LoginUser user = currentUser();
        switch (request.getMethod()) {
            case "OPTIONS" -> options(response);
            case "PROPFIND" -> propfind(request, response, user);
            case "GET", "HEAD" -> get(request, response, user);
            case "PUT" -> put(request, response, user);
            case "MKCOL" -> mkcol(request, response, user);
            case "DELETE" -> delete(request, response, user);
            case "MOVE" -> move(request, response, user);
            case "LOCK" -> lock(request, response, user);
            case "UNLOCK" -> unlock(request, response, user);
            default -> response.setStatus(405);
        }
    }

    // ========== OPTIONS ==========
    private void options(HttpServletResponse response) {
        response.setHeader("DAV", "1,2");
        String methods = "OPTIONS, PROPFIND, GET, PUT, MKCOL, DELETE, MOVE, LOCK, UNLOCK";
        response.setHeader("Allow", methods);
        response.setHeader("Public", methods);
        response.setHeader("MS-Author-Via", "DAV");
        response.setStatus(200);
    }

    // ========== PROPFIND ==========
    private void propfind(HttpServletRequest request, HttpServletResponse response,
                          LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            boolean depthZero = "0".equals(request.getHeader("Depth"));
            List<DatFile> children = List.of();
            List<WebDavPropFindXmlBuilder.VirtualDept> depts = List.of();
            switch (rp.getType()) {
                case ROOT -> {
                    children = List.of();
                    depts = depthZero ? List.of() : listVisibleDepts(user);
                }
                case DEPT_ROOT -> {
                    children = depthZero ? List.of()
                            : filterVisible(fileService.listFiles(deptRootQuery(rp.getDeptId()), user));
                }
                case FOLDER -> {
                    children = depthZero ? List.of()
                            : filterVisible(fileService.listFiles(folderQuery(rp.getDatFile().getId()), user));
                }
                case FILE -> { children = List.of(rp.getDatFile()); }
                case NOT_FOUND -> { response.setStatus(404); return; }
            }
            String basePath = encodedBasePath(request.getRequestURI());
            response.setStatus(207);
            response.setContentType("application/xml; charset=utf-8");
            response.getWriter().write(xmlBuilder.build(basePath, children, depts));
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== GET ==========
    private void get(HttpServletRequest request, HttpServletResponse response,
                     LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            if (rp.getType() == ResolvedPath.Type.NOT_FOUND) { response.setStatus(404); return; }
            if (rp.getType() == ResolvedPath.Type.FILE) {
                if (lockStore.isLockedByOther(rp.getDatFile().getId(), user.getId())) {
                    WebDavErrors.write(response, new BusinessException(R.CODE_LOCKED, "文件已被他人锁定"));
                    return;
                }
                fileService.download(rp.getDatFile().getId(), response);
                return;
            }
            // 目录 GET → 客户端应改用 PROPFIND
            response.setStatus(405);
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== PUT ==========
    private void put(HttpServletRequest request, HttpServletResponse response,
                     LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            String lockToken = lockTokenHeader(request);
            if (rp.getType() == ResolvedPath.Type.FILE) {
                // 覆盖已存在文件
                lockStore.assertLockHeld(rp.getDatFile().getId(), lockToken);
                fileService.writeContent(rp.getDatFile().getId(), request.getInputStream(), user);
                response.setStatus(204);
                return;
            }
            // 新文件：父必须是 FOLDER 或 DEPT_ROOT
            ResolvedPath parent = resolver.resolve(parentPath(request.getRequestURI()), user);
            Long parentDeptId = parentDeptId(parent);
            if (!fileService.canCreate(parentDeptId, user)) {
                WebDavErrors.write(response, new BusinessException(R.CODE_FORBIDDEN, "无权在此目录上传"));
                return;
            }
            String fileName = lastSegment(request.getRequestURI());
            fileService.uploadFile(
                    new StreamMultipartFile(fileName, request), parentFolderId(parent), null,
                    parentDeptId, null, user);
            response.setStatus(201);
            response.setHeader("Location", request.getRequestURI());
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== MKCOL ==========
    private void mkcol(HttpServletRequest request, HttpServletResponse response,
                       LoginUser user) throws IOException {
        try {
            ResolvedPath parent = resolver.resolve(parentPath(request.getRequestURI()), user);
            Long parentDeptId = parentDeptId(parent);
            if (!fileService.canCreate(parentDeptId, user)) {
                WebDavErrors.write(response, new BusinessException(R.CODE_FORBIDDEN, "无权创建文件夹"));
                return;
            }
            fileService.createFolder(parentFolderId(parent), lastSegment(request.getRequestURI()), parentDeptId(parent), null, user);
            response.setStatus(201);
        } catch (BusinessException ex) {
            int sc = WebDavErrors.statusCode(ex);
            // 同名/参数错误 → 409 Conflict
            int mapped = sc == 400 ? 409 : sc;
            response.setStatus(mapped);
            response.setContentType("application/xml; charset=utf-8");
            response.getWriter().write(WebDavErrors.errorXml(mapped));
        }
    }

    // ========== DELETE ==========
    private void delete(HttpServletRequest request, HttpServletResponse response,
                        LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            if (rp.getDatFile() == null) { response.setStatus(404); return; }
            lockStore.assertLockHeld(rp.getDatFile().getId(), lockTokenHeader(request));
            fileService.delete(rp.getDatFile().getId(), user);
            response.setStatus(204);
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== MOVE ==========
    private void move(HttpServletRequest request, HttpServletResponse response,
                      LoginUser user) throws IOException {
        try {
            ResolvedPath src = resolver.resolve(request.getRequestURI(), user);
            if (src.getDatFile() == null) { response.setStatus(404); return; }
            String dest = request.getHeader("Destination");
            if (dest == null) { response.setStatus(400); return; }
            String destPath = stripHost(dest);
            ResolvedPath destParent = resolver.resolve(parentPath(destPath), user);
            Long destDeptId = parentDeptId(destParent);
            // 跨部门移动被拒：源与目标必须同部门
            if (!src.getDatFile().getDeptId().equals(destDeptId)) {
                WebDavErrors.write(response, new BusinessException(R.CODE_FORBIDDEN, "不能跨部门移动"));
                return;
            }
            fileService.move(src.getDatFile().getId(), parentFolderId(destParent), user);
            // WebDAV MOVE 含重命名语义：Destination 末段为新名字，与原名不同则改名
            String newName = lastSegment(destPath);
            String oldName = src.getDatFile().getDisplayName() != null
                    ? src.getDatFile().getDisplayName() : src.getDatFile().getName();
            if (newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
                fileService.rename(src.getDatFile().getId(), newName, user);
            }
            response.setStatus(201);
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== LOCK / UNLOCK ==========
    private void lock(HttpServletRequest request, HttpServletResponse response,
                      LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            if (rp.getDatFile() == null) { response.setStatus(404); return; }
            if (!fileService.canWrite(rp.getDatFile(), user)) {
                WebDavErrors.write(response, new BusinessException(R.CODE_FORBIDDEN, "无写权限不能加锁"));
                return;
            }
            String token = lockStore.tryLock(rp.getDatFile().getId(), user.getId(), 1800);
            if (token == null) {
                WebDavErrors.write(response, new BusinessException(R.CODE_LOCKED, "已被他人锁定"));
                return;
            }
            response.setStatus(200);
            response.setContentType("application/xml; charset=utf-8");
            response.setHeader("Lock-Token", "<" + token + ">");
            response.getWriter().write(lockXml(token));
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    private void unlock(HttpServletRequest request, HttpServletResponse response,
                        LoginUser user) throws IOException {
        ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
        if (rp.getDatFile() == null) { response.setStatus(404); return; }
        String token = lockTokenHeader(request);
        if (lockStore.unlock(rp.getDatFile().getId(), token)) {
            response.setStatus(204);
        } else {
            response.setStatus(409);
        }
    }

    // ========== 辅助 ==========
    /** 从 SecurityContext 取 username 后重加载 LoginUser（复刻 CurrentUserArgumentResolver）。 */
    private LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        String username = principal instanceof String s ? s : auth.getName();
        try {
            return userDetailsLoader.loadByUsername(username);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            return null;
        }
    }

    /**
     * 提取锁 token。
     * <p>UNLOCK 用 Lock-Token 头（{@code Lock-Token: <token>}）；
     * 覆盖写入（PUT）/删除（DELETE）/移动（MOVE）时客户端把 token 放在 If 头
     * （{@code If: (<token>)}，可能带资源 tag {@code <uri> (<token>)}）。
     * 两种头都解析，任一命中即返回。
     */
    private String lockTokenHeader(HttpServletRequest req) {
        String lockToken = req.getHeader("Lock-Token");
        if (lockToken != null) {
            String t = stripAngles(lockToken).trim();
            if (!t.isEmpty()) return t;
        }
        String ifHeader = req.getHeader("If");
        if (ifHeader != null) {
            int parenStart = ifHeader.indexOf('(');
            if (parenStart >= 0) {
                int parenEnd = ifHeader.indexOf(')', parenStart);
                if (parenEnd > parenStart) {
                    String t = stripAngles(ifHeader.substring(parenStart + 1, parenEnd)).trim();
                    if (!t.isEmpty()) return t;
                }
            }
        }
        return null;
    }

    private String stripAngles(String s) {
        return s.replace("<", "").replace(">", "");
    }

    /** 把 URI 每段重新 URL 编码，作为 PROPFIND basePath（XmlBuilder 拼接 href 时不再编码 basePath）。 */
    private String encodedBasePath(String uri) {
        String after = uri.indexOf("/webdav") >= 0 ? uri.substring(uri.indexOf("/webdav")) : uri;
        StringBuilder sb = new StringBuilder();
        for (String seg : after.split("/")) {
            if (seg.isEmpty()) continue;
            sb.append("/")
                    .append(URLEncoder.encode(URLDecoder.decode(seg, StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                            .replace("+", "%20"));
        }
        sb.append("/");
        return sb.length() == 0 ? "/webdav/" : sb.toString();
    }

    private String lastSegment(String uri) {
        String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        String[] parts = decoded.split("/");
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    private String parentPath(String uri) {
        String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        int i = decoded.lastIndexOf('/');
        if (i <= 0) return "/webdav/";
        String parent = decoded.substring(0, i);
        if (parent.equals("/webdav") || parent.isEmpty()) return "/webdav/";
        return parent;
    }

    private Long parentFolderId(ResolvedPath parent) {
        if (parent.getType() == ResolvedPath.Type.FOLDER) return parent.getDatFile().getId();
        return null; // DEPT_ROOT → parentId=null
    }

    private Long parentDeptId(ResolvedPath parent) {
        if (parent.getType() == ResolvedPath.Type.DEPT_ROOT) return parent.getDeptId();
        if (parent.getDatFile() != null) return parent.getDatFile().getDeptId();
        return null;
    }

    private String stripHost(String url) {
        int idx = url.indexOf("/webdav");
        return idx >= 0 ? url.substring(idx) : url;
    }

    private String lockXml(String token) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<D:prop xmlns:D=\"DAV:\"><D:lockdiscovery><D:activelock>" +
                "<D:locktype><D:write/></D:locktype>" +
                "<D:lockscope><D:exclusive/></D:lockscope>" +
                "<D:depth>0</D:depth>" +
                "<D:timeout>Second-1800</D:timeout>" +
                "<D:locktoken><D:href>" + token + "</D:href></D:locktoken>" +
                "</D:activelock></D:lockdiscovery></D:prop>";
    }

    /** 列出用户可见部门（根目录用）。 */
    private List<WebDavPropFindXmlBuilder.VirtualDept> listVisibleDepts(LoginUser user) {
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean admin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
        if (admin) {
            rows.addAll(jdbcTemplate.queryForList("SELECT id, dept_name FROM sys_department WHERE deleted = 0"));
        } else if (user.getDepartmentId() != null) {
            rows.addAll(jdbcTemplate.queryForList(
                    "SELECT id, dept_name FROM sys_department WHERE deleted = 0 AND " +
                    "(dept_path LIKE CONCAT((SELECT dept_path FROM sys_department WHERE id = ?), '%') " +
                    "OR id IN (SELECT DISTINCT f.dept_id FROM dat_file f JOIN dat_file_share s ON s.file_id = f.id WHERE s.dept_id = ?))",
                    user.getDepartmentId(), user.getDepartmentId()));
        }
        List<WebDavPropFindXmlBuilder.VirtualDept> depts = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            depts.add(new WebDavPropFindXmlBuilder.VirtualDept(
                    ((Number) r.get("id")).longValue(), (String) r.get("dept_name")));
        }
        return depts;
    }

    private DatFileQuery deptRootQuery(Long deptId) {
        DatFileQuery q = new DatFileQuery();
        q.setDeptId(deptId);
        return q;
    }

    private DatFileQuery folderQuery(Long parentId) {
        DatFileQuery q = new DatFileQuery();
        q.setParentId(parentId);
        return q;
    }

    /** listFiles 已在 service 层完成可见性过滤（含共享后代文件夹保留），此处仅 VO→Entity。 */
    private List<DatFile> filterVisible(List<DatFileVO> vos) {
        List<DatFile> result = new ArrayList<>();
        for (DatFileVO vo : vos) result.add(toEntity(vo));
        return result;
    }

    private DatFile toEntity(DatFileVO vo) {
        DatFile f = new DatFile();
        f.setId(vo.getId());
        f.setName(vo.getName());
        f.setDisplayName(vo.getDisplayName());
        f.setIsDirectory(vo.getIsDirectory());
        f.setFileSize(vo.getFileSize());
        f.setExtension(vo.getExtension());
        f.setMimeType(vo.getMimeType());
        f.setDeptId(vo.getDeptId());
        return f;
    }
}
