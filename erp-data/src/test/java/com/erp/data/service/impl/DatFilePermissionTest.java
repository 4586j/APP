package com.erp.data.service.impl;

import com.erp.data.entity.DatFile;
import com.erp.data.mapper.DatFileMapper;
import com.erp.data.mapper.DatFileShareMapper;
import com.erp.data.storage.LocalStorageBackend;
import com.erp.security.user.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatFilePermissionTest {

    @Mock DatFileMapper mapper;
    @Mock DatFileShareMapper shareMapper;
    @Mock JdbcTemplate jdbcTemplate;

    private DatFileServiceImpl service;

    @BeforeEach
    void setUp() {
        // 手动构造，绕过 @Value 注入；注入 LocalStorageBackend
        service = new DatFileServiceImpl(new LocalStorageBackend("./uploads/data"), mapper, shareMapper, jdbcTemplate);
    }

    private LoginUser user(Long id, Long deptId, boolean admin) {
        return LoginUser.builder()
                .id(id).username("u" + id).departmentId(deptId)
                .roles(admin ? List.of("ROLE_ADMIN") : List.of()).build();
    }

    private DatFile file(Long id, Long deptId, Long createdBy, String path) {
        DatFile f = new DatFile();
        f.setId(id); f.setDeptId(deptId); f.setCreatedBy(createdBy);
        f.setPath(path); f.setIsDirectory(0); f.setDeleted(0);
        return f;
    }

    // ==================== canCreate ====================

    @Test
    void canCreate_ownDept_returnsTrue() {
        assertTrue(service.canCreate(10L, user(1L, 10L, false)));
    }

    @Test
    void canCreate_otherDept_returnsFalse() {
        assertFalse(service.canCreate(20L, user(1L, 10L, false)));
    }

    @Test
    void canCreate_nullTarget_returnsFalse() {
        assertFalse(service.canCreate(null, user(1L, 10L, false)));
    }

    @Test
    void canCreate_admin_returnsTrue() {
        assertTrue(service.canCreate(20L, user(1L, 10L, true)));
    }

    @Test
    void canCreate_nullUser_returnsFalse() {
        assertFalse(service.canCreate(10L, null));
    }

    // ==================== canWrite ====================

    @Test
    void canWrite_admin_returnsTrue() {
        assertTrue(service.canWrite(file(1L, 20L, 99L, "/1/"), user(1L, 10L, true)));
    }

    @Test
    void canWrite_ownDept_returnsTrue() {
        assertTrue(service.canWrite(file(1L, 10L, 99L, "/1/"), user(2L, 10L, false)));
    }

    @Test
    void canWrite_subDept_returnsFalse() {
        DatFile f = file(1L, 11L, 99L, "/1/");
        assertFalse(service.canWrite(f, user(2L, 10L, false)));
    }

    @Test
    void canWrite_sharedToMyDept_returnsTrue() {
        DatFile f = file(1L, 20L, 99L, "/1/");
        when(shareMapper.selectDeptIdsByFileId(1L)).thenReturn(List.of(10L));
        assertTrue(service.canWrite(f, user(2L, 10L, false)));
    }

    // ==================== canAccess ====================

    @Test
    void canAccess_ancestorFolderShared_returnsTrue() {
        DatFile f = file(42L, 20L, 99L, "/3/15/42/");
        when(shareMapper.selectDeptIdsByFileId(42L)).thenReturn(List.of());
        when(mapper.selectSharedFileIdsIn(java.util.List.of(3L, 15L, 42L), 10L))
                .thenReturn(List.of(15L));
        assertTrue(service.canAccess(f, user(2L, 10L, false)));
    }

    @Test
    void canAccess_noRelation_returnsFalse() {
        DatFile f = file(42L, 20L, 99L, "/3/15/42/");
        when(shareMapper.selectDeptIdsByFileId(42L)).thenReturn(List.of());
        when(mapper.selectSharedFileIdsIn(any(), eq(10L))).thenReturn(List.of());
        assertFalse(service.canAccess(f, user(2L, 10L, false)));
    }
}
