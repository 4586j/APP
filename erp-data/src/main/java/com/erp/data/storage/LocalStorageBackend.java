package com.erp.data.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地磁盘实现。行为等价于重构前 {@code DatFileServiceImpl} 的磁盘 IO：
 * 按部门 / 日期分片，文件名用 UUID。
 *
 * <p>store 返回相对 key {@code {deptId}/{yyyyMMdd}/{uuid}.ext}（为后续对象存储铺路）；
 * open / overwrite / exists / delete 兼容老的绝对路径 storage_path（重构前 uploadFile 存的是
 * {@code target.toString()} 绝对路径），老数据零迁移可读。
 *
 * <p>默认装配：未配置 {@code app.storage.type} 时即生效（{@code matchIfMissing = true}）。
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageBackend implements StorageBackend {

    private final Path uploadRoot;

    public LocalStorageBackend(
            @Value("${app.storage.local.data-root:${app.upload.data-root:./uploads/data}}") String dataRoot) {
        this.uploadRoot = Path.of(dataRoot).toAbsolutePath().normalize();
    }

    @Override
    public String store(InputStream in, long size, String suggestedName, Long deptId) throws IOException {
        String suffix = "";
        if (suggestedName != null) {
            int dot = suggestedName.lastIndexOf('.');
            if (dot >= 0) {
                suffix = suggestedName.substring(dot);
            }
        }
        String deptSeg = deptId != null ? String.valueOf(deptId) : "_common";
        String daySeg = LocalDate.now().toString().replace("-", "");
        String storedName = UUID.randomUUID() + suffix;
        String relKey = deptSeg + "/" + daySeg + "/" + storedName;

        Path target = uploadRoot.resolve(relKey).normalize();
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(target,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
        return relKey;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public long overwrite(String storageKey, InputStream in, long size) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(target,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            return in.transferTo(out);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolve(storageKey));
    }

    /**
     * 兼容老绝对路径与新相对 key：绝对路径直接用，相对 key 基于 uploadRoot 解析。
     */
    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey is blank");
        }
        Path p = Path.of(storageKey);
        return p.isAbsolute() ? p : uploadRoot.resolve(storageKey).normalize();
    }
}
