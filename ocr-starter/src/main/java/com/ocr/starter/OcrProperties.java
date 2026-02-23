package com.ocr.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {

    private double defaultThreshold = 95.0;
    private int maxUploadFiles = 50;
    private long maxFileSize = 20 * 1024 * 1024;
    private int defaultRetainDays = 365;

    private Baidu baidu = new Baidu();
    private Minio minio = new Minio();

    @Data
    public static class Baidu {
        private String appId;
        private String apiKey;
        private String secretKey;
        private int qpsLimit = 10;
    }

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "ocr-files";
    }
}
