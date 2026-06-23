package com.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 外贸 ERP 系统启动入口（B0.3 迁入 erp-web 模块）。
 * <p>聚合所有 13 个业务/基础模块，由 Flyway 自动迁移 36 张表。
 */
@SpringBootApplication(scanBasePackages = "com.erp")
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
