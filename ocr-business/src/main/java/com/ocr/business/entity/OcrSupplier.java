package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ocr_supplier")
public class OcrSupplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String extSupplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierAlias;
    private String contactPerson;
    private String phone;
    private Integer status;
    private LocalDateTime syncedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
