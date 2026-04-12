package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ocr_field_alias")
public class OcrFieldAlias {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fieldId;
    private Long supplierId;
    private String aliasName;
    private String aliasLang;
    private Integer priority;
}
