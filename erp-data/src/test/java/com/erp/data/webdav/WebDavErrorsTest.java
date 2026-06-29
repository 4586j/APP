package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebDavErrorsTest {

    @Test
    void statusCode_forbidden() {
        assertEquals(403, WebDavErrors.statusCode(new BusinessException(R.CODE_FORBIDDEN, "x")));
    }

    @Test
    void statusCode_notFound() {
        assertEquals(404, WebDavErrors.statusCode(new BusinessException(R.CODE_NOT_FOUND, "x")));
    }

    @Test
    void statusCode_locked() {
        assertEquals(423, WebDavErrors.statusCode(new BusinessException(R.CODE_LOCKED, "x")));
    }

    @Test
    void statusCode_paramInvalid_mapsTo400() {
        assertEquals(400, WebDavErrors.statusCode(new BusinessException(R.CODE_PARAM_INVALID, "x")));
    }

    @Test
    void statusCode_unknown_defaults500() {
        assertEquals(500, WebDavErrors.statusCode(new BusinessException(9999, "x")));
    }

    @Test
    void errorXml_containsStatus() {
        String xml = WebDavErrors.errorXml(403);
        assertTrue(xml.contains("<D:error"));
        assertTrue(xml.contains("403 Forbidden"));
    }
}
