package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import com.erp.data.mapper.DatFileMapper;
import com.erp.security.user.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebDavPathResolverTest {

    @Mock DatFileMapper mapper;
    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks WebDavPathResolver resolver;

    private LoginUser user(Long id, Long deptId) {
        return LoginUser.builder().id(id).username("u").departmentId(deptId).build();
    }

    private DatFile dir(Long id, String name, Long deptId, Long parent) {
        DatFile f = new DatFile();
        f.setId(id); f.setName(name); f.setIsDirectory(1); f.setDeptId(deptId); f.setParentId(parent); f.setDeleted(0);
        return f;
    }
    private DatFile file(Long id, String name, Long deptId, Long parent) {
        DatFile f = new DatFile();
        f.setId(id); f.setName(name); f.setIsDirectory(0); f.setDeptId(deptId); f.setParentId(parent); f.setDeleted(0);
        return f;
    }

    @Test
    void resolve_root_returnsROOT() {
        assertEquals(ResolvedPath.Type.ROOT, resolver.resolve("/webdav/", user(1L, 10L)).getType());
        assertEquals(ResolvedPath.Type.ROOT, resolver.resolve("/webdav", user(1L, 10L)).getType());
    }

    @Test
    void resolve_deptRoot_returnsDEPT_ROOT() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        ResolvedPath r = resolver.resolve("/webdav/销售部/", user(1L, 10L));
        assertEquals(ResolvedPath.Type.DEPT_ROOT, r.getType());
        assertEquals(20L, r.getDeptId());
    }

    @Test
    void resolve_unknownDept_returnsNOT_FOUND() {
        when(jdbcTemplate.queryForList(any(String.class), eq("不存在")))
                .thenReturn(List.of());
        ResolvedPath r = resolver.resolve("/webdav/不存在/", user(1L, 10L));
        assertEquals(ResolvedPath.Type.NOT_FOUND, r.getType());
    }

    @Test
    void resolve_folderInDept_returnsFOLDER() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        when(mapper.selectRootFilesByDeptId(20L)).thenReturn(List.of(dir(30L, "报表", 20L, null)));
        ResolvedPath r = resolver.resolve("/webdav/销售部/报表", user(1L, 10L));
        assertEquals(ResolvedPath.Type.FOLDER, r.getType());
        assertEquals(30L, r.getDatFile().getId());
    }

    @Test
    void resolve_fileInFolder_returnsFILE() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        when(mapper.selectRootFilesByDeptId(20L)).thenReturn(List.of(dir(30L, "报表", 20L, null)));
        when(mapper.selectByParentId(30L)).thenReturn(List.of(file(40L, "数据.xlsx", 20L, 30L)));
        ResolvedPath r = resolver.resolve("/webdav/销售部/报表/数据.xlsx", user(1L, 10L));
        assertEquals(ResolvedPath.Type.FILE, r.getType());
        assertEquals(40L, r.getDatFile().getId());
    }

    @Test
    void resolve_missingSegment_returnsNOT_FOUND() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        when(mapper.selectRootFilesByDeptId(20L)).thenReturn(List.of());
        ResolvedPath r = resolver.resolve("/webdav/销售部/不存在", user(1L, 10L));
        assertEquals(ResolvedPath.Type.NOT_FOUND, r.getType());
    }

    @Test
    void resolve_urlDecodedChinese() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        ResolvedPath r = resolver.resolve("/webdav/%E9%94%80%E5%94%AE%E9%83%A8/", user(1L, 10L));
        assertEquals(ResolvedPath.Type.DEPT_ROOT, r.getType());
    }
}
