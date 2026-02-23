package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ocr_field_definition")
public class OcrFieldDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docTypeId;
    private String fieldCode;
    private String fieldName;
    private String fieldType;
    private String position;
    private Integer isRequired;
    private Integer sortOrder;
    private String validationRule;
    private String defaultValue;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
