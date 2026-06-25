package com.erp.common.storage;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.BucketExistsArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 操作模板（B6 收尾）。
 *
 * <p>封装连接 / 上传 / 下载 / 删除 / 预签名 URL，业务模块（单证、报表）统一依赖此类，
 * 不直接接触 MinioClient SDK。
 *
 * <p>放在 erp-common 是因为 erp-document 与 erp-report 都需要，且它只依赖 minio SDK，
 * 不引入任何 Web/Servlet 依赖，符合 PITFALLS §10 的分层约束。
 */
@Component
public class MinioTemplate {

    private static final Logger log = LoggerFactory.getLogger(MinioTemplate.class);

    private final MinioProperties props;
    private MinioClient client;

    public MinioTemplate(MinioProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        this.client = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
        ensureBucket(props.getBucket());
    }

    /** 确保桶存在，不存在则创建。MinIO 不可达时仅告警，不阻断应用启动。 */
    public void ensureBucket(String bucket) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 桶已创建: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO 桶检查/创建失败（endpoint={}, bucket={}）：{}", props.getEndpoint(), bucket, e.getMessage());
        }
    }

    /**
     * 上传字节数组到默认桶。
     *
     * @param objectKey   对象 key（含前缀，如 document/2026/xxx.pdf）
     * @param data        文件内容
     * @param contentType MIME 类型
     * @return 上传成功的 objectKey
     */
    public String upload(String objectKey, byte[] data, String contentType) {
        try (InputStream in = new ByteArrayInputStream(data)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, data.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败: " + objectKey + " — " + e.getMessage(), e);
        }
    }

    /** 下载对象为字节数组。 */
    public byte[] download(String objectKey) {
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(props.getBucket())
                .object(objectKey)
                .build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("MinIO 下载失败: " + objectKey + " — " + e.getMessage(), e);
        }
    }

    /** 删除对象。 */
    public void remove(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 删除失败: " + objectKey + " — " + e.getMessage(), e);
        }
    }

    /** 获取对象大小（字节），用于回填 file_size。 */
    public long statSize(String objectKey) {
        try {
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
            return stat.size();
        } catch (Exception e) {
            throw new RuntimeException("MinIO 元信息读取失败: " + objectKey + " — " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名下载 URL（GET），前端直接用此地址下载，不经过后端转发。
     *
     * @param objectKey 对象 key
     * @return 带签名的临时下载地址
     */
    public String presignedDownloadUrl(String objectKey) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .expiry(props.getPresignedExpirySeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 预签名 URL 生成失败: " + objectKey + " — " + e.getMessage(), e);
        }
    }
}
