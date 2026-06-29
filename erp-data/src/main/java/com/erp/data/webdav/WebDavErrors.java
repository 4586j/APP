package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/** 业务异常 → WebDAV 状态码 + XML error body。 */
public final class WebDavErrors {

    private WebDavErrors() {}

    public static int statusCode(BusinessException ex) {
        int code = ex.getCode();
        if (code == R.CODE_UNAUTHORIZED) return HttpStatus.UNAUTHORIZED.value();
        if (code == R.CODE_FORBIDDEN) return HttpStatus.FORBIDDEN.value();
        if (code == R.CODE_NOT_FOUND) return HttpStatus.NOT_FOUND.value();
        if (code == R.CODE_LOCKED) return 423;
        if (code == R.CODE_PARAM_INVALID) return HttpStatus.BAD_REQUEST.value();
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public static String errorXml(int statusCode) {
        String reason = HttpStatus.resolve(statusCode) != null
                ? HttpStatus.resolve(statusCode).getReasonPhrase()
                : "Error";
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<D:error xmlns:D=\"DAV:\"><D:status>HTTP/1.1 " + statusCode + " " + reason + "</D:status></D:error>";
    }

    public static void write(HttpServletResponse response, BusinessException ex) throws IOException {
        int sc = statusCode(ex);
        response.setStatus(sc);
        response.setContentType("application/xml; charset=utf-8");
        response.getWriter().write(errorXml(sc));
    }
}
