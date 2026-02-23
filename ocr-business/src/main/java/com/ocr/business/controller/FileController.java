package com.ocr.business.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocr.business.entity.OcrRecognitionFile;
import com.ocr.business.service.FileQueryService;
import com.ocr.common.result.PageResult;
import com.ocr.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class FileController {

    private final FileQueryService fileQueryService;

    /**
     * 独立文件查询（跨任务），支持按供应商筛选
     */
    @GetMapping("/files")
    public R<PageResult<OcrRecognitionFile>> queryFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) Boolean supplierMatched,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Page<OcrRecognitionFile> result = fileQueryService.queryFiles(
                page, size, supplierName, supplierMatched, status, fileName, startDate, endDate);
        return R.ok(PageResult.of(result.getTotal(), result.getPages(), result.getRecords()));
    }

    /**
     * 任务内文件列表查询
     */
    @GetMapping("/task/{taskId}/files")
    public R<PageResult<OcrRecognitionFile>> queryByTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Page<OcrRecognitionFile> result = fileQueryService.queryByTask(taskId, page, size, status);
        return R.ok(PageResult.of(result.getTotal(), result.getPages(), result.getRecords()));
    }

    /**
     * 文件识别结果详情
     */
    @GetMapping("/file/{fileId}")
    public R<OcrRecognitionFile> getFile(@PathVariable Long fileId) {
        return R.ok(fileQueryService.getById(fileId));
    }

    /**
     * 查看OCR原始JSON
     */
    @GetMapping("/file/{fileId}/raw-json")
    public R<Object> getRawJson(@PathVariable Long fileId) {
        OcrRecognitionFile file = fileQueryService.getById(fileId);
        return R.ok(file.getOcrRawJson());
    }
}
