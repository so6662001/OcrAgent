package com.ocr.api.dto;

import lombok.Data;

@Data
public class RecognizeRequest {
    private String docTypeCode;
    private String callbackUrl;
}
