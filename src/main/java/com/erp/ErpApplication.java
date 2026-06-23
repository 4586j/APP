package com.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 外贸 ERP 系统启动入口。
 * B0.2 阶段：单模块过渡。
 * B0.3 拆 14 子模块后，本类会迁移到 erp-web 模块。
 */
@SpringBootApplication
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
