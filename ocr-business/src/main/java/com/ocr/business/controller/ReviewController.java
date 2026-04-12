package com.ocr.business.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocr.business.entity.OcrRecognitionFile;
import com.ocr.business.entity.OcrSupplier;
import com.ocr.business.mapper.OcrSupplierMapper;
import com.ocr.business.service.FileQueryService;
import com.ocr.common.context.OcrUserContext;
import com.ocr.common.exception.OcrException;
import com.ocr.common.result.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class ReviewController {

    private final FileQueryService fileQueryService;
    private final OcrSupplierMapper supplierMapper;

    @GetMapping("/file/{fileId}/review")
    public R<OcrRecognitionFile> getReview(@PathVariable Long fileId) {
        return R.ok(fileQueryService.getById(fileId));
    }

    @PostMapping("/file/{fileId}/confirm")
    public R<String> confirm(@PathVariable Long fileId, @RequestBody ConfirmRequest request) {
        OcrRecognitionFile file = fileQueryService.getById(fileId);

        if (request.getSupplierId() != null) {
            OcrSupplier supplier = supplierMapper.selectById(request.getSupplierId());
            if (supplier == null || !supplier.getTenantId().equals(OcrUserContext.getTenantId())) {
                throw new OcrException(403, "无权使用该供应商");
            }
            file.setSupplierId(request.getSupplierId());
            file.setSupplierMatched(1);
        }

        file.setReviewJson(request);
        file.setStatus("CONFIRMED");
        file.setReviewedBy(OcrUserContext.getUserName());
        file.setReviewedAt(LocalDateTime.now());
        fileQueryService.update(file);
        return R.ok("审核确认成功，数据已推送至外部系统");
    }

    @Data
    public static class ConfirmRequest {
        private Long supplierId;
        private List<Map<String, String>> header;
        private List<BodyRow> body;

        @Data
        public static class BodyRow {
            private Integer rowIndex;
            private List<Map<String, String>> cells;
        }
    }
}
