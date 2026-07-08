package com.erp.data.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 物理存储后端抽象。
 *
 * <p>dat_file.storage_path 存的是后端返回的存储 key（本地相对路径 / OSS objectKey /
 * 网盘 itemId）。业务层（DatFileService）只依赖此接口，不感知本地磁盘 / 对象存储 /
 * 网盘的差异。后续接入 OSS/S3/OneDrive 只需新增一个实现类。
 */
public interface StorageBackend {

    /**
     * 上传：返回存储 key（写入 dat_file.storage_path）。
     *
     * @param in            输入流（调用方负责关闭）
     * @param size          已知字节数，未知传 -1
     * @param suggestedName  建议名（仅用于扩展名推断 / 调试，不保证作为最终 key）
     * @param deptId        归属部门（用于分片目录），可为 null
     */
    String store(InputStream in, long size, String suggestedName, Long deptId) throws IOException;

    /** 下载 / 读取流。调用方负责关闭。 */
    InputStream open(String storageKey) throws IOException;

    /**
     * 覆盖写入（WebDAV PUT 保存）。
     *
     * @return 实际写入字节数（供调用方更新 fileSize）。
     */
    long overwrite(String storageKey, InputStream in, long size) throws IOException;

    /** 删除物理对象（逻辑删除时不一定调，物理清理时调）。 */
    void delete(String storageKey) throws IOException;

    /** 是否存在该对象（迁移回退判断用）。 */
    boolean exists(String storageKey);

    /**
     * 流式下载到 OutputStream，避免大文件撑内存。
     * 默认实现基于 {@link #open}，后端有更优实现可覆盖。
     */
    default void writeTo(String storageKey, OutputStream out) throws IOException {
        try (InputStream in = open(storageKey)) {
            in.transferTo(out);
        }
    }
}