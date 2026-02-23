package com.ocr.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.ocr.common",
        "com.ocr.api",
        "com.ocr.business",
        "com.ocr.file",
        "com.ocr.recognition"
})
public class OcrAutoConfiguration {
}
