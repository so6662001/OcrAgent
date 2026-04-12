package com.ocragent.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(413, "文件大小超出限制（最大20MB）");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public Result<Void> handleSecurity(SecurityException e) {
        log.warn("安全异常: {}", e.getMessage());
        return Result.fail(403, "操作被拒绝");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleBadRequest(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("服务器内部错误，请稍后重试");
    }
}
