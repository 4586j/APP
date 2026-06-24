package com.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 外贸 ERP 系统启动入口（B0.3 迁入 erp-web 模块）。
 * <p>聚合所有 13 个业务/基础模块，由 Flyway 自动迁移 36 张表。
 * <p>B1.5 起开启 MyBatis Mapper 扫描：com.erp.**.mapper
 */
@SpringBootApplication(scanBasePackages = "com.erp")
@MapperScan("com.erp.**.mapper")
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
