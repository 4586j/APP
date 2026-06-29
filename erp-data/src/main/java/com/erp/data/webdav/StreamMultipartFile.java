package com.erp.data.webdav;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** 把 WebDAV PUT 的请求流包装成 MultipartFile，复用 DatFileService.uploadFile。 */
public class StreamMultipartFile implements MultipartFile {

    private final String name;
    private final HttpServletRequest request;

    public StreamMultipartFile(String name, HttpServletRequest request) {
        this.name = name;
        this.request = request;
    }

    @Override public String getName() { return "file"; }
    @Override public String getOriginalFilename() { return name; }
    @Override public String getContentType() { return request.getContentType(); }
    @Override public boolean isEmpty() { return false; }
    @Override public long getSize() {
        long len = request.getContentLengthLong();
        return len < 0 ? 0 : len;
    }
    @Override public byte[] getBytes() throws IOException {
        return request.getInputStream().readAllBytes();
    }
    @Override public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }
    @Override public void transferTo(File dest) throws IOException {
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    @Override public void transferTo(Path dest) throws IOException {
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
