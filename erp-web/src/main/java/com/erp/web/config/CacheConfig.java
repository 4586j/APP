package com.erp.web.config;

import com.erp.common.cache.CacheTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CacheConfig {

    @Bean
    public CacheTemplate cacheTemplate(StringRedisTemplate redisTemplate) {
        return new CacheTemplate(redisTemplate);
    }
}
