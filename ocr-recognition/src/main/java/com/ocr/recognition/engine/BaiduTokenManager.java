package com.ocr.recognition.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduTokenManager {

    private static final String REDIS_KEY = "ocr:baidu:access_token";
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    @Value("${ocr.baidu.api-key:}")
    private String apiKey;

    @Value("${ocr.baidu.secret-key:}")
    private String secretKey;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(REDIS_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        return refreshToken();
    }

    public synchronized String refreshToken() {
        String cached = redisTemplate.opsForValue().get(REDIS_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("百度OCR API Key未配置");
            return null;
        }

        try {
            String url = TOKEN_URL + "?grant_type=client_credentials"
                    + "&client_id=" + apiKey
                    + "&client_secret=" + secretKey;

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            String accessToken = json.get("access_token").asText();
            long expiresIn = json.get("expires_in").asLong();

            redisTemplate.opsForValue().set(REDIS_KEY, accessToken,
                    expiresIn - 600, TimeUnit.SECONDS);

            log.info("百度OCR Token刷新成功，有效期: {}秒", expiresIn);
            return accessToken;
        } catch (Exception e) {
            log.error("百度OCR Token获取失败", e);
            return null;
        }
    }
}
