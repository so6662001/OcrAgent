package com.ocr.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocr.business.entity.OcrRecognitionFile;
import com.ocr.business.entity.OcrSupplier;
import com.ocr.business.mapper.OcrRecognitionFileMapper;
import com.ocr.business.mapper.OcrSupplierMapper;
import com.ocr.common.context.OcrUserContext;
import com.ocr.common.exception.OcrException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileQueryService {

    private final OcrRecognitionFileMapper fileMapper;
    private final OcrSupplierMapper supplierMapper;

    public Page<OcrRecognitionFile> queryByTask(Long taskId, int page, int size, String status) {
        LambdaQueryWrapper<OcrRecognitionFile> wrapper = new LambdaQueryWrapper<OcrRecognitionFile>()
                .eq(OcrRecognitionFile::getTaskId, taskId)
                .eq(OcrRecognitionFile::getTenantId, OcrUserContext.getTenantId())
                .eq(status != null && !status.isEmpty(), OcrRecognitionFile::getStatus, status)
                .orderByDesc(OcrRecognitionFile::getCreatedAt);

        return fileMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 独立文件查询（跨任务），支持按供应商筛选
     */
    public Page<OcrRecognitionFile> queryFiles(int page, int size, String supplierName,
                                                Boolean supplierMatched, String status,
                                                String fileName, LocalDate startDate, LocalDate endDate) {
        String tenantId = OcrUserContext.getTenantId();

        LambdaQueryWrapper<OcrRecognitionFile> wrapper = new LambdaQueryWrapper<OcrRecognitionFile>()
                .eq(OcrRecognitionFile::getTenantId, tenantId);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(OcrRecognitionFile::getStatus, status);
        }
        if (fileName != null && !fileName.isEmpty()) {
            wrapper.like(OcrRecognitionFile::getFileName, fileName);
        }
        if (supplierMatched != null) {
            wrapper.eq(OcrRecognitionFile::getSupplierMatched, supplierMatched ? 1 : 0);
        }
        if (startDate != null) {
            wrapper.ge(OcrRecognitionFile::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(OcrRecognitionFile::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }

        if (supplierName != null && !supplierName.isEmpty()) {
            List<OcrSupplier> suppliers = supplierMapper.selectList(
                    new LambdaQueryWrapper<OcrSupplier>()
                            .eq(OcrSupplier::getTenantId, tenantId)
                            .like(OcrSupplier::getSupplierName, supplierName));
            if (!suppliers.isEmpty()) {
                List<Long> supplierIds = suppliers.stream().map(OcrSupplier::getId).toList();
                wrapper.in(OcrRecognitionFile::getSupplierId, supplierIds);
            } else {
                wrapper.eq(OcrRecognitionFile::getSupplierId, -1L);
            }
        }

        wrapper.orderByDesc(OcrRecognitionFile::getCreatedAt);
        return fileMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public OcrRecognitionFile getById(Long fileId) {
        OcrRecognitionFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new OcrException(404, "文件不存在: " + fileId);
        }
        return file;
    }

    public void update(OcrRecognitionFile file) {
        fileMapper.updateById(file);
    }
}
