package com.ocr.api.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CallbackPayload {
    private Long taskId;
    private String taskNo;
    private String docTypeCode;
    private String docTypeName;
    private Long fileId;
    private String fileName;
    private boolean autoApproved;
    private String reviewedBy;
    private SupplierInfo supplier;
    private RecognitionData data;
    private ConfidenceInfo confidence;
    private LocalDateTime timestamp;
    private String sign;

    @Data
    public static class SupplierInfo {
        private String extSupplierId;
        private String supplierCode;
        private String supplierName;
    }

    @Data
    public static class RecognitionData {
        private Map<String, String> header;
        private List<Map<String, String>> body;
    }

    @Data
    public static class ConfidenceInfo {
        private double overall;
        private double threshold;
    }
}
