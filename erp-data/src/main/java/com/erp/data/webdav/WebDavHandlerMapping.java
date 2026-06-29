package com.erp.data.webdav;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.Properties;

/**
 * 把 /webdav/** 注册到 {@link WebDavController}（HttpRequestHandler）。
 *
 * <p>用 {@link SimpleUrlHandlerMapping} 而非 @RequestMapping：Spring MVC 的
 * RequestMappingHandlerMapping 只识别标准 HttpMethod，会以 400 拒绝 PROPFIND 等
 * WebDAV 动词。原生 handler 不经过 method 解析，可自行按 request.getMethod() 分发。
 *
 * <p>优先级设为高于 RequestMappingHandlerMapping（其 order=0），避免 /webdav/** 被抢占。
 */
@Configuration
public class WebDavHandlerMapping {

    @Bean
    public HandlerMapping webdavHandlerMapping(WebDavController controller) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1); // 优先于 RequestMappingHandlerMapping(0)
        Properties urlProperties = new Properties();
        urlProperties.setProperty("/webdav", "webDavController");
        urlProperties.setProperty("/webdav/**", "webDavController");
        mapping.setMappings(urlProperties);
        mapping.setUrlDecode(false); // 中文路径段自行解码，避免容器预先解码
        return mapping;
    }
}
