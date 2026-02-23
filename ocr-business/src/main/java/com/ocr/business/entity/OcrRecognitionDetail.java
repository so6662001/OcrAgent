package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ocr_recognition_detail")
public class OcrRecognitionDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long fieldDefId;
    private String position;
    private Integer rowIndex;
    private String ocrRawText;
    private String parsedValue;
    private BigDecimal confidence;
    private String status;
    private Integer isModified;
    private String modifiedValue;
    private String boundingBox;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
