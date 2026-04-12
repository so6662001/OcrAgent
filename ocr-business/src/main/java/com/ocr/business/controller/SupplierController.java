package com.ocr.business.controller;

import com.ocr.api.dto.SupplierSyncRequest;
import com.ocr.business.entity.OcrSupplier;
import com.ocr.business.service.SupplierService;
import com.ocr.common.context.OcrUserContext;
import com.ocr.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/supplier/sync")
    public R<Map<String, Integer>> sync(@RequestBody SupplierSyncRequest request) {
        return R.ok(supplierService.syncSuppliers(request));
    }

    @GetMapping("/suppliers")
    public R<List<OcrSupplier>> search(@RequestParam(required = false) String keyword) {
        return R.ok(supplierService.search(OcrUserContext.getTenantId(), keyword));
    }
}
