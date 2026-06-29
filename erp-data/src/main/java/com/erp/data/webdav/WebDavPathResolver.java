package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import com.erp.data.mapper.DatFileMapper;
import com.erp.security.user.LoginUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /webdav/销售部/报表/x.xlsx → ResolvedPath。 */
@Component
public class WebDavPathResolver {

    private static final String PREFIX = "/webdav";

    private final DatFileMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public WebDavPathResolver(DatFileMapper mapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResolvedPath resolve(String rawPath, LoginUser user) {
        String basePath = normalizeBase(rawPath);
        // 去掉 /webdav 前缀
        String p = rawPath == null ? "" : rawPath;
        // 定位 /webdav 之后的内容
        int idx = p.indexOf(PREFIX);
        if (idx >= 0) p = p.substring(idx + PREFIX.length());
        p = URLDecoder.decode(p, StandardCharsets.UTF_8);
        // 分段
        String[] segs = p.split("/");
        List<String> parts = new ArrayList<>();
        for (String s : segs) if (!s.isEmpty()) parts.add(s);

        if (parts.isEmpty()) {
            return ResolvedPath.root(basePath);
        }
        // 第一段 = 部门名
        String deptName = parts.get(0);
        Long deptId = lookupDeptId(deptName);
        if (deptId == null) return ResolvedPath.notFound(basePath);

        if (parts.size() == 1) {
            return ResolvedPath.deptRoot(deptId, basePath);
        }
        // 从部门根逐级下钻
        List<DatFile> current = mapper.selectRootFilesByDeptId(deptId);
        DatFile matched = null;
        for (int i = 1; i < parts.size(); i++) {
            String seg = parts.get(i);
            matched = null;
            for (DatFile f : current) {
                String name = f.getDisplayName() != null ? f.getDisplayName() : f.getName();
                if (seg.equals(name)) { matched = f; break; }
            }
            if (matched == null) return ResolvedPath.notFound(basePath);
            if (i < parts.size() - 1) {
                if (matched.getIsDirectory() == null || matched.getIsDirectory() != 1) {
                    return ResolvedPath.notFound(basePath);
                }
                current = mapper.selectByParentId(matched.getId());
            }
        }
        return ResolvedPath.of(matched, basePath);
    }

    /** /webdav/销售部/报表/ → 末尾补 /，保留原始路径作 basePath 前缀（href 编码留给 XmlBuilder）。 */
    private String normalizeBase(String rawPath) {
        String p = rawPath == null ? "/webdav/" : rawPath;
        if (!p.endsWith("/")) p = p + "/";
        return p;
    }

    private Long lookupDeptId(String deptName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM sys_department WHERE dept_name = ? AND deleted = 0", deptName);
        if (rows.isEmpty()) return null;
        return ((Number) rows.get(0).get("id")).longValue();
    }
}
