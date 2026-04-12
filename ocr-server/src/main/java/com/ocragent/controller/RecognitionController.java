package com.ocragent.controller;

import com.ocragent.common.Result;
import com.ocragent.model.dto.CorrectionRequest;
import com.ocragent.model.enums.FileType;
import com.ocragent.model.enums.RecognizeMode;
import com.ocragent.model.vo.BatchUploadVO;
import com.ocragent.model.vo.RecognizeTaskVO;
import com.ocragent.service.FieldMappingService;
import com.ocragent.service.FileProcessingService;
import com.ocragent.service.RecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecognitionController {

    private final RecognitionService recognitionService;
    private final FieldMappingService fieldMappingService;
    private final FileProcessingService fileProcessingService;

    @PostMapping("/recognize/upload")
    public Result<BatchUploadVO> upload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "mode", defaultValue = "AUTO") RecognizeMode mode
    ) {
        if (files == null || files.length == 0) {
            return Result.fail(400, "请选择至少一个文件");
        }

        fileProcessingService.validateBatchSize(files.length);

        byte[][] filesData = new byte[files.length][];
        String[] fileNames = new String[files.length];
        FileType[] fileTypes = new FileType[files.length];

        for (int i = 0; i < files.length; i++) {
            fileProcessingService.validateFile(files[i]);
            try {
                filesData[i] = files[i].getBytes();
                fileNames[i] = files[i].getOriginalFilename();
                fileTypes[i] = fileProcessingService.detectFileType(files[i]);
                fileProcessingService.saveFile(files[i]);
            } catch (IOException e) {
                return Result.fail("文件读取失败: " + files[i].getOriginalFilename());
            }
        }

        BatchUploadVO vo = recognitionService.batchUpload(filesData, fileNames, fileTypes, mode);
        return Result.ok("文件已提交识别队列", vo);
    }

    @GetMapping("/recognize/task/{taskId}")
    public Result<RecognizeTaskVO> getTask(@PathVariable String taskId) {
        RecognizeTaskVO task = recognitionService.getTask(taskId);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }
        return Result.ok(task);
    }

    @GetMapping("/recognize/tasks")
    public Result<List<RecognizeTaskVO>> getAllTasks() {
        return Result.ok(recognitionService.getAllTasks());
    }

    @PostMapping("/recognize/confirm")
    public Result<RecognizeTaskVO> confirm(@RequestBody CorrectionRequest request) {
        RecognizeTaskVO result = recognitionService.confirmCorrection(request);
        return Result.ok("确认成功，修正数据已学习", result);
    }

    @GetMapping("/field-mapping/config")
    public Result<Map<String, FieldMappingService.StandardField>> getFieldConfig() {
        return Result.ok(fieldMappingService.getStandardFields());
    }

    @PostMapping("/field-mapping/synonym")
    public Result<Void> addSynonym(
            @RequestParam String standardKey,
            @RequestParam String synonym
    ) {
        if (standardKey == null || standardKey.length() > 64 || synonym == null || synonym.length() > 128) {
            return Result.fail(400, "参数长度不合法");
        }
        fieldMappingService.addSynonym(standardKey, synonym);
        return Result.ok("同义词添加成功", null);
    }
}
