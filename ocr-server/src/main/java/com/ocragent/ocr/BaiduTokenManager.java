package com.ocragent.ocr;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ocragent.config.BaiduOcrConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduTokenManager {

    private final BaiduOcrConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String cachedToken;
    private Instant tokenExpiry;

    public synchronized String getAccessToken() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        return refreshToken();
    }

    private String refreshToken() {
        String apiKey = config.getApiKey();
        String secretKey = config.getSecretKey();
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new RuntimeException("百度OCR API Key/Secret Key未配置，请设置环境变量 BAIDU_OCR_API_KEY 和 BAIDU_OCR_SECRET_KEY");
        }

        String url = "https://aip.baidubce.com/oauth/2.0/token";
        String body = "grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (json.has("access_token")) {
                cachedToken = json.get("access_token").getAsString();
                int expiresIn = json.get("expires_in").getAsInt();
                tokenExpiry = Instant.now().plusSeconds(expiresIn - 3600);
                log.info("百度OCR Token刷新成功, 有效期{}秒", expiresIn);
                return cachedToken;
            } else {
                log.error("获取Token失败, HTTP状态={}", response.statusCode());
                throw new RuntimeException("获取百度OCR Token失败，请检查API Key配置");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取百度OCR Token网络异常", e);
            throw new RuntimeException("获取百度OCR Token失败（网络异常）", e);
        }
    }
}
