package com.ocragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OcrAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(OcrAgentApplication.class, args);
    }
}
