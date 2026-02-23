package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(value = "ocr_recognition_file", autoResultMap = true)
public class OcrRecognitionFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long taskId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer pageCount;
    private String status;
    private BigDecimal overallConf;
    private BigDecimal thresholdUsed;
    private Long supplierId;
    private Integer supplierMatched;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object resultJson;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object reviewJson;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object ocrRawJson;
    private String errorMessage;
    private String ocrApiType;
    private Integer processTime;
    private BigDecimal baiduCost;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String callbackStatus;
    private LocalDateTime callbackTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private LocalDateTime expireAt;
}
