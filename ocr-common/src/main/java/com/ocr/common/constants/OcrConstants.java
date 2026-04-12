package com.ocr.common.constants;

public final class OcrConstants {

    private OcrConstants() {}

    public static final double DEFAULT_THRESHOLD = 95.0;

    public static final int MAX_UPLOAD_FILES = 50;

    public static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    public static final int DEFAULT_RETAIN_DAYS = 365;

    public static final String MQ_EXCHANGE = "ocr.exchange";
    public static final String MQ_QUEUE_RECOGNIZE = "ocr.queue.recognize";
    public static final String MQ_ROUTING_KEY_RECOGNIZE = "ocr.recognize";
    public static final String MQ_QUEUE_CALLBACK = "ocr.queue.callback";
    public static final String MQ_ROUTING_KEY_CALLBACK = "ocr.callback";

    public static final String[] ALLOWED_FILE_TYPES = {"pdf", "jpg", "jpeg", "png"};
}
