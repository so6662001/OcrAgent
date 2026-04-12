package com.ocragent.model.vo;

import com.ocragent.model.enums.TaskStatus;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RecognizeTaskVO {
    private String taskId;
    private String fileName;
    private TaskStatus status;
    private String errorMessage;
    private double confidence;
    private String ocrApi;

    private List<FieldVO> fields;
    private List<Map<String, FieldVO>> tableData;
    private String rawText;
    private long processTimeMs;

    @Data
    public static class FieldVO {
        private String standardKey;
        private String displayName;
        private String originalKey;
        private Object value;
        private double confidence;
        private String valueType;
    }
}
