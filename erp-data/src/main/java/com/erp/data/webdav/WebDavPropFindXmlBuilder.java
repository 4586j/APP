package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 生成 WebDAV PROPFIND multistatus XML（Windows 资源管理器靠此显示文件列表）。 */
@Component
public class WebDavPropFindXmlBuilder {

    public record VirtualDept(Long deptId, String name) {}

    public String build(String basePath, List<DatFile> children, List<VirtualDept> depts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<D:multistatus xmlns:D=\"DAV:\">");
        appendResponse(sb, basePath, ".", 0, true);
        for (VirtualDept d : depts) {
            appendResponse(sb, basePath + encode(d.name()) + "/", d.name(), 0, true);
        }
        for (DatFile f : children) {
            String name = f.getDisplayName() != null ? f.getDisplayName() : f.getName();
            boolean dir = f.getIsDirectory() != null && f.getIsDirectory() == 1;
            String href = basePath + encode(name) + (dir ? "/" : "");
            appendResponse(sb, href, name, f.getFileSize() == null ? 0 : f.getFileSize(), dir);
        }
        sb.append("</D:multistatus>");
        return sb.toString();
    }

    public String buildFile(String href, DatFile file) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<D:multistatus xmlns:D=\"DAV:\">");
        String name = file.getDisplayName() != null ? file.getDisplayName() : file.getName();
        appendResponse(sb, href, name, file.getFileSize() == null ? 0 : file.getFileSize(), false);
        sb.append("</D:multistatus>");
        return sb.toString();
    }

    private void appendResponse(StringBuilder sb, String href, String displayName, long size, boolean isCollection) {
        sb.append("<D:response>");
        sb.append("<D:href>").append(escapeXml(href)).append("</D:href>");
        sb.append("<D:propstat><D:prop>");
        sb.append("<D:displayname>").append(escapeXml(displayName)).append("</D:displayname>");
        sb.append("<D:iscollection>").append(isCollection ? 1 : 0).append("</D:iscollection>");
        if (!isCollection) {
            sb.append("<D:getcontentlength>").append(size).append("</D:getcontentlength>");
        }
        sb.append("<D:resourcetype>").append(isCollection ? "<D:collection/>" : "").append("</D:resourcetype>");
        sb.append("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat>");
        sb.append("</D:response>");
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
