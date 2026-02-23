package com.ocr.business.controller;

import com.ocr.business.entity.OcrTenantThreshold;
import com.ocr.business.service.ThresholdService;
import com.ocr.common.context.OcrUserContext;
import com.ocr.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr/config/threshold")
@RequiredArgsConstructor
public class ThresholdController {

    private final ThresholdService thresholdService;

    @GetMapping
    public R<List<OcrTenantThreshold>> list() {
        return R.ok(thresholdService.listByTenant(OcrUserContext.getTenantId()));
    }

    @PutMapping("/{docTypeId}")
    public R<Void> set(@PathVariable Long docTypeId, @RequestBody Map<String, BigDecimal> body) {
        thresholdService.setThreshold(docTypeId, body.get("threshold"));
        return R.ok();
    }

    @DeleteMapping("/{docTypeId}")
    public R<Void> reset(@PathVariable Long docTypeId) {
        thresholdService.resetThreshold(docTypeId);
        return R.ok();
    }
}
