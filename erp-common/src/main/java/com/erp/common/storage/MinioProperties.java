package com.erp.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 对象存储配置（B6 收尾）。
 *
 * <p>对应 application.yaml 的 {@code erp.minio.*} 节点。
 */
@Data
@Component
@ConfigurationProperties(prefix = "erp.minio")
public class MinioProperties {

    /** 服务端点，例如 http://127.0.0.1:9000。 */
    private String endpoint = "http://127.0.0.1:9000";

    /** Access Key。 */
    private String accessKey = "minioadmin";

    /** Secret Key。 */
    private String secretKey = "minioadmin";

    /** 默认桶名（单证 / 报表均存此桶，用 object key 前缀区分）。 */
    private String bucket = "erp-demo2";

    /** 预签名下载链接有效期（秒），默认 1 小时。 */
    private int presignedExpirySeconds = 3600;
}
