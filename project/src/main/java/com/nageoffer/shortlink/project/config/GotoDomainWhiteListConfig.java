package com.nageoffer.shortlink.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 短链接跳转域名白名单配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "shortlink.goto-domain.white-list")
public class GotoDomainWhiteListConfig {

    /**
     * 是否启用跳转域名白名单
     */
    private Boolean enable;

    /**
     * 跳转原始域名白名单网站名称集合
     */
    private String names;

    /**
     * 可跳转的原始连接域名
     */
    private List<String> details;
}
