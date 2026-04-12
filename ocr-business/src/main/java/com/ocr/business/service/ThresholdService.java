package com.ocr.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocr.business.entity.OcrDocumentType;
import com.ocr.business.entity.OcrTenantThreshold;
import com.ocr.business.mapper.OcrDocumentTypeMapper;
import com.ocr.business.mapper.OcrTenantThresholdMapper;
import com.ocr.common.constants.OcrConstants;
import com.ocr.common.context.OcrUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThresholdService {

    private final OcrTenantThresholdMapper thresholdMapper;
    private final OcrDocumentTypeMapper docTypeMapper;

    public double getThreshold(String tenantId, Long docTypeId) {
        OcrTenantThreshold custom = thresholdMapper.selectOne(
                new LambdaQueryWrapper<OcrTenantThreshold>()
                        .eq(OcrTenantThreshold::getTenantId, tenantId)
                        .eq(OcrTenantThreshold::getDocTypeId, docTypeId));
        if (custom != null) {
            return custom.getThreshold().doubleValue();
        }

        OcrDocumentType docType = docTypeMapper.selectById(docTypeId);
        if (docType != null && docType.getDefaultThreshold() != null) {
            return docType.getDefaultThreshold().doubleValue();
        }

        return OcrConstants.DEFAULT_THRESHOLD;
    }

    public List<OcrTenantThreshold> listByTenant(String tenantId) {
        return thresholdMapper.selectList(
                new LambdaQueryWrapper<OcrTenantThreshold>()
                        .eq(OcrTenantThreshold::getTenantId, tenantId));
    }

    public void setThreshold(Long docTypeId, BigDecimal threshold) {
        String tenantId = OcrUserContext.getTenantId();
        OcrTenantThreshold existing = thresholdMapper.selectOne(
                new LambdaQueryWrapper<OcrTenantThreshold>()
                        .eq(OcrTenantThreshold::getTenantId, tenantId)
                        .eq(OcrTenantThreshold::getDocTypeId, docTypeId));

        if (existing != null) {
            existing.setThreshold(threshold);
            thresholdMapper.updateById(existing);
        } else {
            OcrTenantThreshold t = new OcrTenantThreshold();
            t.setTenantId(tenantId);
            t.setDocTypeId(docTypeId);
            t.setThreshold(threshold);
            thresholdMapper.insert(t);
        }
    }

    public void resetThreshold(Long docTypeId) {
        String tenantId = OcrUserContext.getTenantId();
        thresholdMapper.delete(
                new LambdaQueryWrapper<OcrTenantThreshold>()
                        .eq(OcrTenantThreshold::getTenantId, tenantId)
                        .eq(OcrTenantThreshold::getDocTypeId, docTypeId));
    }
}
