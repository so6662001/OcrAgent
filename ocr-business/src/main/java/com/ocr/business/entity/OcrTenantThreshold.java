package com.ocr.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ocr_tenant_threshold")
public class OcrTenantThreshold {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long docTypeId;
    private BigDecimal threshold;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
