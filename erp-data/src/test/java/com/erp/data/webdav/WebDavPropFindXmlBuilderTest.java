package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebDavPropFindXmlBuilderTest {

    private final WebDavPropFindXmlBuilder builder = new WebDavPropFindXmlBuilder();

    private DatFile file(Long id, String name, boolean dir, long size) {
        DatFile f = new DatFile();
        f.setId(id); f.setName(name); f.setDisplayName(name);
        f.setIsDirectory(dir ? 1 : 0); f.setFileSize(size);
        f.setExtension(".xlsx"); f.setMimeType("application/vnd.openxmlformats");
        return f;
    }

    @Test
    void build_hasMultistatusRoot() {
        String xml = builder.build("/webdav/sales/", List.of(file(1L, "a.xlsx", false, 100)), List.of());
        assertTrue(xml.contains("<D:multistatus"));
        assertTrue(xml.contains("</D:multistatus>"));
    }

    @Test
    void build_fileHasHrefAndProps() {
        String xml = builder.build("/webdav/sales/", List.of(file(1L, "a.xlsx", false, 100)), List.of());
        assertTrue(xml.contains("<D:href>/webdav/sales/a.xlsx</D:href>"));
        assertTrue(xml.contains("100</D:getcontentlength>"));
        assertTrue(xml.contains("<D:iscollection>0</D:iscollection>"));
    }

    @Test
    void buildFile_returnsFileItselfWithoutAppendingNameAgain() {
        String xml = builder.buildFile("/webdav/data/tree/c45.py", file(1L, "c45.py", false, 100));
        assertTrue(xml.contains("<D:href>/webdav/data/tree/c45.py</D:href>"));
        assertFalse(xml.contains("/webdav/data/tree/c45.py/c45.py"));
        assertTrue(xml.contains("<D:iscollection>0</D:iscollection>"));
    }

    @Test
    void build_directoryIsCollection1() {
        String xml = builder.build("/webdav/sales/", List.of(file(2L, "sub", true, 0)), List.of());
        assertTrue(xml.contains("/webdav/sales/sub/</D:href>"));
        assertTrue(xml.contains("<D:iscollection>1</D:iscollection>"));
    }

    @Test
    void build_chineseNameEncodedInHref_displaynameRaw() {
        String xml = builder.build("/webdav/sales/", List.of(file(3L, "数据.xlsx", false, 50)), List.of());
        assertFalse(xml.contains("/webdav/sales/数据.xlsx</D:href>"));
        assertTrue(xml.contains("<D:displayname>数据.xlsx</D:displayname>"));
    }

    @Test
    void build_rootListsVirtualDepts() {
        var dept = new WebDavPropFindXmlBuilder.VirtualDept(10L, "销售部");
        String xml = builder.build("/webdav/", List.of(), List.of(dept));
        assertTrue(xml.contains("/webdav/%E9%94%80%E5%94%AE%E9%83%A8/</D:href>"));
        assertTrue(xml.contains("<D:displayname>销售部</D:displayname>"));
        assertTrue(xml.contains("<D:iscollection>1</D:iscollection>"));
    }
}
