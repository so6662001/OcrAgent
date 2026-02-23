package com.ocr.business.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocr.business.entity.OcrRecognitionTask;
import com.ocr.business.service.TaskService;
import com.ocr.common.result.PageResult;
import com.ocr.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/tasks")
    public R<PageResult<OcrRecognitionTask>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String docTypeCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Page<OcrRecognitionTask> result = taskService.queryTasks(page, size, status, docTypeCode, startDate, endDate);
        return R.ok(PageResult.of(result.getTotal(), result.getPages(), result.getRecords()));
    }

    @GetMapping("/task/{taskId}")
    public R<OcrRecognitionTask> get(@PathVariable Long taskId) {
        return R.ok(taskService.getById(taskId));
    }
}
