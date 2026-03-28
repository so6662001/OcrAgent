package com.ocragent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "baidu.ocr")
public class BaiduOcrConfig {
    private String apiKey;
    private String secretKey;
}
