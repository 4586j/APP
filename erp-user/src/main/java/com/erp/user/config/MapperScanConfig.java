package com.erp.user.config;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
@ConditionalOnBean(DataSource.class)
@MapperScan("com.erp.**.mapper")
public class MapperScanConfig {
}
