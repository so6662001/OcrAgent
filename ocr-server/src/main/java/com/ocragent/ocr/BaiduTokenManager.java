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
        String url = String.format(
                "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                config.getApiKey(), config.getSecretKey()
        );
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
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
                String error = json.has("error_description")
                        ? json.get("error_description").getAsString()
                        : response.body();
                throw new RuntimeException("获取Token失败: " + error);
            }
        } catch (Exception e) {
            log.error("获取百度OCR Token异常", e);
            throw new RuntimeException("获取百度OCR Token失败", e);
        }
    }
}
