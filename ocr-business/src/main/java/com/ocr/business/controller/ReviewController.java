package com.ocr.business.controller;

import com.ocr.business.entity.OcrRecognitionFile;
import com.ocr.business.service.FileQueryService;
import com.ocr.common.context.OcrUserContext;
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

    @GetMapping("/file/{fileId}/review")
    public R<OcrRecognitionFile> getReview(@PathVariable Long fileId) {
        return R.ok(fileQueryService.getById(fileId));
    }

    @PostMapping("/file/{fileId}/confirm")
    public R<String> confirm(@PathVariable Long fileId, @RequestBody ConfirmRequest request) {
        OcrRecognitionFile file = fileQueryService.getById(fileId);
        file.setReviewJson(request);
        file.setStatus("CONFIRMED");
        file.setReviewedBy(OcrUserContext.getUserName());
        file.setReviewedAt(LocalDateTime.now());
        if (request.getSupplierId() != null) {
            file.setSupplierId(request.getSupplierId());
            file.setSupplierMatched(1);
        }
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
