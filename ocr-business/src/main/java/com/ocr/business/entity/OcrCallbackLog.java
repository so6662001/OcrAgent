package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ocr_callback_log")
public class OcrCallbackLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long fileId;
    private String callbackUrl;
    private String requestBody;
    private String responseBody;
    private Integer httpStatus;
    private String status;
    private Integer retryCount;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
