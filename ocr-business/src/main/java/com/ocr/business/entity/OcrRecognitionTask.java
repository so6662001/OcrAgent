package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ocr_recognition_task")
public class OcrRecognitionTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String taskNo;
    private Long docTypeId;
    private Integer totalFiles;
    private Integer successCount;
    private Integer reviewCount;
    private Integer failedCount;
    private Integer confirmedCount;
    private String status;
    private String callbackUrl;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private LocalDateTime expireAt;
}
