package com.ocragent.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CorrectionRequest {
    private String taskId;
    private List<Map<String, Object>> fields;
    private List<Map<String, Object>> tableData;
}
