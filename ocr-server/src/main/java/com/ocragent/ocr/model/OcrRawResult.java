package com.ocragent.ocr.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OcrRawResult {
    private boolean success;
    private String errorMessage;
    private String apiUsed;
    private List<TextLine> lines = new ArrayList<>();
    private List<List<TextLine>> tableRows;
    private String rawJson;

    public static OcrRawResult fail(String error) {
        OcrRawResult r = new OcrRawResult();
        r.setSuccess(false);
        r.setErrorMessage(error);
        return r;
    }
}
