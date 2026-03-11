package com.ocr.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocr.api.dto.SupplierSyncRequest;
import com.ocr.business.entity.OcrSupplier;
import com.ocr.business.mapper.OcrSupplierMapper;
import com.ocr.common.context.OcrUserContext;
import com.ocr.common.exception.OcrException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final OcrSupplierMapper supplierMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Integer> syncSuppliers(SupplierSyncRequest request) {
        int insertCount = 0;
        int updateCount = 0;
        String currentTenant = OcrUserContext.getTenantId();
        String tenantId = (currentTenant != null) ? currentTenant : request.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            throw new OcrException(400, "租户ID不能为空");
        }

        for (SupplierSyncRequest.SupplierItem item : request.getSuppliers()) {
            OcrSupplier existing = supplierMapper.selectOne(
                    new LambdaQueryWrapper<OcrSupplier>()
                            .eq(OcrSupplier::getTenantId, tenantId)
                            .eq(OcrSupplier::getExtSupplierId, item.getExtSupplierId()));

            if (existing != null) {
                existing.setSupplierCode(item.getSupplierCode());
                existing.setSupplierName(item.getSupplierName());
                existing.setSupplierAlias(toJson(item.getSupplierAlias()));
                existing.setContactPerson(item.getContactPerson());
                existing.setPhone(item.getPhone());
                existing.setSyncedAt(LocalDateTime.now());
                supplierMapper.updateById(existing);
                updateCount++;
            } else {
                OcrSupplier supplier = new OcrSupplier();
                supplier.setTenantId(tenantId);
                supplier.setExtSupplierId(item.getExtSupplierId());
                supplier.setSupplierCode(item.getSupplierCode());
                supplier.setSupplierName(item.getSupplierName());
                supplier.setSupplierAlias(toJson(item.getSupplierAlias()));
                supplier.setContactPerson(item.getContactPerson());
                supplier.setPhone(item.getPhone());
                supplier.setStatus(1);
                supplier.setSyncedAt(LocalDateTime.now());
                supplierMapper.insert(supplier);
                insertCount++;
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("syncCount", insertCount + updateCount);
        result.put("insertCount", insertCount);
        result.put("updateCount", updateCount);
        return result;
    }

    public List<OcrSupplier> search(String tenantId, String keyword) {
        LambdaQueryWrapper<OcrSupplier> wrapper = new LambdaQueryWrapper<OcrSupplier>()
                .eq(OcrSupplier::getTenantId, tenantId)
                .eq(OcrSupplier::getStatus, 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(OcrSupplier::getSupplierName, keyword)
                    .or().like(OcrSupplier::getSupplierCode, keyword));
        }
        return supplierMapper.selectList(wrapper);
    }

    public List<OcrSupplier> listByTenant(String tenantId) {
        return supplierMapper.selectList(
                new LambdaQueryWrapper<OcrSupplier>()
                        .eq(OcrSupplier::getTenantId, tenantId)
                        .eq(OcrSupplier::getStatus, 1));
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
