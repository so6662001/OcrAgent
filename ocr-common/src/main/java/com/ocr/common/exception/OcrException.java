package com.ocr.common.exception;

import lombok.Getter;

@Getter
public class OcrException extends RuntimeException {

    private final int code;

    public OcrException(String message) {
        super(message);
        this.code = 500;
    }

    public OcrException(int code, String message) {
        super(message);
        this.code = code;
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
