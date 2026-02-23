package com.ocr.recognition.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduOcrClient {

    private static final String BASE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1";

    private final BaiduTokenManager tokenManager;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 通用文字识别（高精度版） - 机打+扭曲/模糊
     */
    public JsonNode accurateOcr(byte[] imageData) throws Exception {
        String body = "image=" + URLEncoder.encode(Base64.getEncoder().encodeToString(imageData), StandardCharsets.UTF_8)
                + "&detect_direction=true"
                + "&paragraph=true"
                + "&probability=true"
                + "&language_type=CHN_ENG";

        return callApi(BASE_URL + "/accurate", body);
    }

    /**
     * 手写文字识别
     */
    public JsonNode handwritingOcr(byte[] imageData) throws Exception {
        String body = "image=" + URLEncoder.encode(Base64.getEncoder().encodeToString(imageData), StandardCharsets.UTF_8)
                + "&recognize_granularity=big"
                + "&words_type=handprint_mix";

        return callApi(BASE_URL + "/handwriting", body);
    }

    /**
     * 文档结构化识别 - 表格/结构化
     */
    public JsonNode docAnalysis(byte[] imageData) throws Exception {
        String body = "image=" + URLEncoder.encode(Base64.getEncoder().encodeToString(imageData), StandardCharsets.UTF_8)
                + "&detect_direction=true"
                + "&result_type=big"
                + "&language_type=CHN_ENG";

        return callApi("https://aip.baidubce.com/rest/2.0/ocr/v1/doc_analysis", body);
    }

    private JsonNode callApi(String url, String body) throws Exception {
        String token = tokenManager.getAccessToken();
        if (token == null) {
            throw new RuntimeException("百度OCR Token不可用，请检查API Key配置");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "?access_token=" + token))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode result = objectMapper.readTree(response.body());

        if (result.has("error_code")) {
            log.error("百度OCR调用失败: error_code={}, error_msg={}",
                    result.get("error_code"), result.get("error_msg"));
            if (result.get("error_code").asInt() == 110 || result.get("error_code").asInt() == 111) {
                tokenManager.refreshToken();
            }
        }

        return result;
    }
}
