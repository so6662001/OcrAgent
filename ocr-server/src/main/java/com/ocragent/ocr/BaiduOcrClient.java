package com.ocragent.ocr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ocragent.ocr.model.OcrRawResult;
import com.ocragent.ocr.model.TextLine;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduOcrClient {

    private final BaiduTokenManager tokenManager;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final String BASE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1";

    /**
     * 通用文字识别（高精度）
     */
    public OcrRawResult accurateBasic(byte[] imageData) {
        return callOcrApi(BASE_URL + "/accurate_basic", imageData, "accurate_basic");
    }

    /**
     * 通用文字识别（含位置信息）
     */
    public OcrRawResult accurate(byte[] imageData) {
        return callOcrApi(BASE_URL + "/accurate", imageData, "accurate");
    }

    /**
     * 手写文字识别
     */
    public OcrRawResult handwriting(byte[] imageData) {
        return callOcrApi(BASE_URL + "/handwriting", imageData, "handwriting");
    }

    /**
     * 表格文字识别（同步）
     */
    public OcrRawResult tableRecognize(byte[] imageData) {
        String url = BASE_URL + "/table";
        String body = "image=" + URLEncoder.encode(
                Base64.getEncoder().encodeToString(imageData), StandardCharsets.UTF_8
        ) + "&is_sync=true&result_type=json";
        return doPost(url, body, "table");
    }

    private OcrRawResult callOcrApi(String url, byte[] imageData, String apiName) {
        String body = "image=" + URLEncoder.encode(
                Base64.getEncoder().encodeToString(imageData), StandardCharsets.UTF_8
        ) + "&language_type=CHN_ENG&detect_direction=true&paragraph=true";
        return doPost(url, body, apiName);
    }

    private OcrRawResult doPost(String url, String body, String apiName) {
        try {
            String token = tokenManager.getAccessToken();
            String fullUrl = url + "?access_token=" + token;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            return parseResponse(responseBody, apiName);
        } catch (Exception e) {
            log.error("调用百度OCR API [{}] 异常", apiName, e);
            return OcrRawResult.fail("OCR调用失败: " + e.getMessage());
        }
    }

    private OcrRawResult parseResponse(String responseBody, String apiName) {
        OcrRawResult result = new OcrRawResult();
        result.setRawJson(responseBody);
        result.setApiUsed(apiName);

        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("error_code")) {
                result.setSuccess(false);
                result.setErrorMessage(json.get("error_msg").getAsString());
                return result;
            }

            if ("table".equals(apiName)) {
                parseTableResult(json, result);
            } else {
                parseGeneralResult(json, result);
            }

            result.setSuccess(true);
        } catch (Exception e) {
            log.error("解析OCR响应异常", e);
            result.setSuccess(false);
            result.setErrorMessage("解析响应失败: " + e.getMessage());
        }
        return result;
    }

    private void parseGeneralResult(JsonObject json, OcrRawResult result) {
        if (!json.has("words_result")) return;
        JsonArray wordsResult = json.getAsJsonArray("words_result");
        List<TextLine> lines = new ArrayList<>();

        for (JsonElement elem : wordsResult) {
            JsonObject item = elem.getAsJsonObject();
            TextLine line = new TextLine();
            line.setText(item.get("words").getAsString());

            if (item.has("probability")) {
                JsonObject prob = item.getAsJsonObject("probability");
                line.setConfidence(prob.has("average") ? prob.get("average").getAsDouble() : 0.9);
            } else {
                line.setConfidence(0.9);
            }

            if (item.has("location")) {
                JsonObject loc = item.getAsJsonObject("location");
                line.setLeft(loc.get("left").getAsInt());
                line.setTop(loc.get("top").getAsInt());
                line.setWidth(loc.get("width").getAsInt());
                line.setHeight(loc.get("height").getAsInt());
            }
            lines.add(line);
        }
        result.setLines(lines);
    }

    private void parseTableResult(JsonObject json, OcrRawResult result) {
        if (!json.has("tables_result")) {
            parseGeneralResult(json, result);
            return;
        }
        JsonArray tables = json.getAsJsonArray("tables_result");
        List<List<TextLine>> tableRows = new ArrayList<>();

        for (JsonElement tableElem : tables) {
            JsonObject table = tableElem.getAsJsonObject();
            if (!table.has("body")) continue;
            JsonArray body = table.getAsJsonArray("body");
            for (JsonElement cellElem : body) {
                JsonObject cell = cellElem.getAsJsonObject();
                int row = cell.get("row_start").getAsInt();
                while (tableRows.size() <= row) {
                    tableRows.add(new ArrayList<>());
                }
                TextLine tl = new TextLine();
                tl.setText(cell.get("words").getAsString());
                tl.setConfidence(0.9);
                tableRows.get(row).add(tl);
            }
        }
        result.setTableRows(tableRows);

        List<TextLine> allLines = new ArrayList<>();
        for (List<TextLine> row : tableRows) {
            allLines.addAll(row);
        }
        result.setLines(allLines);
    }
}
