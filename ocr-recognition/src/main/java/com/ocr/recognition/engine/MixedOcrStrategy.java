package com.ocr.recognition.engine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MixedOcrStrategy {

    private final BaiduOcrClient baiduOcrClient;

    /**
     * 混合调用策略：先用文档结构化，再根据需要补充手写识别
     */
    public Map<String, JsonNode> recognize(byte[] imageData) {
        Map<String, JsonNode> results = new LinkedHashMap<>();

        try {
            JsonNode docResult = baiduOcrClient.docAnalysis(imageData);
            results.put("doc_analysis", docResult);

            if (!hasError(docResult)) {
                log.debug("文档结构化识别成功");
            }
        } catch (Exception e) {
            log.warn("文档结构化识别失败，降级到通用识别", e);
        }

        try {
            JsonNode accurateResult = baiduOcrClient.accurateOcr(imageData);
            results.put("accurate", accurateResult);
        } catch (Exception e) {
            log.warn("通用文字识别失败", e);
        }

        return results;
    }

    /**
     * 专门针对手写内容的识别
     */
    public JsonNode recognizeHandwriting(byte[] imageData) {
        try {
            return baiduOcrClient.handwritingOcr(imageData);
        } catch (Exception e) {
            log.error("手写识别失败", e);
            return null;
        }
    }

    private boolean hasError(JsonNode result) {
        return result != null && result.has("error_code");
    }
}
