package com.erp.user.config;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
@MapperScan("com.erp.**.mapper")
public class MapperScanConfig {
}
