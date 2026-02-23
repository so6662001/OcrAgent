package com.ocr.file.controller;

import com.ocr.business.entity.OcrDocumentType;
import com.ocr.business.entity.OcrRecognitionFile;
import com.ocr.business.entity.OcrRecognitionTask;
import com.ocr.business.mapper.OcrRecognitionFileMapper;
import com.ocr.business.service.DocTypeService;
import com.ocr.business.service.TaskService;
import com.ocr.business.service.ThresholdService;
import com.ocr.common.constants.OcrConstants;
import com.ocr.common.context.OcrUserContext;
import com.ocr.common.exception.OcrException;
import com.ocr.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class FileUploadController {

    private final DocTypeService docTypeService;
    private final TaskService taskService;
    private final ThresholdService thresholdService;
    private final OcrRecognitionFileMapper fileMapper;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/recognize")
    public R<Map<String, Object>> recognize(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("docTypeCode") String docTypeCode,
            @RequestParam("callbackUrl") String callbackUrl) {

        if (files == null || files.length == 0) {
            throw new OcrException(400, "请至少上传一个文件");
        }
        if (files.length > OcrConstants.MAX_UPLOAD_FILES) {
            throw new OcrException(400, "单次最多上传" + OcrConstants.MAX_UPLOAD_FILES + "个文件");
        }

        OcrDocumentType docType = docTypeService.getByCode(docTypeCode);
        if (docType == null) {
            throw new OcrException(400, "单据类型不存在: " + docTypeCode);
        }

        String tenantId = OcrUserContext.getTenantId();
        double threshold = thresholdService.getThreshold(tenantId, docType.getId());

        String taskNo = "OCR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        OcrRecognitionTask task = new OcrRecognitionTask();
        task.setTenantId(tenantId);
        task.setTaskNo(taskNo);
        task.setDocTypeId(docType.getId());
        task.setTotalFiles(files.length);
        task.setSuccessCount(0);
        task.setReviewCount(0);
        task.setFailedCount(0);
        task.setConfirmedCount(0);
        task.setStatus("PROCESSING");
        task.setCallbackUrl(callbackUrl);
        task.setCreatedBy(OcrUserContext.getUserName());
        task.setExpireAt(LocalDateTime.now().plusDays(OcrConstants.DEFAULT_RETAIN_DAYS));
        taskService.create(task);

        List<Long> fileIds = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);

            OcrRecognitionFile fileRecord = new OcrRecognitionFile();
            fileRecord.setTenantId(tenantId);
            fileRecord.setTaskId(task.getId());
            fileRecord.setFileName(file.getOriginalFilename());
            fileRecord.setFileType(getFileExtension(file.getOriginalFilename()));
            fileRecord.setFileSize(file.getSize());
            fileRecord.setStatus("PENDING");
            fileRecord.setThresholdUsed(BigDecimal.valueOf(threshold));
            fileRecord.setSupplierMatched(0);
            fileRecord.setCallbackStatus("PENDING");
            fileRecord.setExpireAt(task.getExpireAt());
            fileMapper.insert(fileRecord);
            fileIds.add(fileRecord.getId());

            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileRecord.getId());
            message.put("taskId", task.getId());
            message.put("tenantId", tenantId);
            message.put("docTypeId", docType.getId());
            message.put("callbackUrl", callbackUrl);
            message.put("threshold", threshold);

            try {
                rabbitTemplate.convertAndSend(OcrConstants.MQ_EXCHANGE,
                        OcrConstants.MQ_ROUTING_KEY_RECOGNIZE, message);
            } catch (Exception e) {
                log.warn("MQ发送失败，文件ID: {}，将在下次重试", fileRecord.getId(), e);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", task.getId());
        data.put("taskNo", taskNo);
        data.put("totalFiles", files.length);
        data.put("status", "PROCESSING");
        data.put("fileIds", fileIds);

        return R.ok(data);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new OcrException(400, "文件为空: " + file.getOriginalFilename());
        }
        if (file.getSize() > OcrConstants.MAX_FILE_SIZE) {
            throw new OcrException(400, "文件大小超出限制(20MB): " + file.getOriginalFilename());
        }
        String ext = getFileExtension(file.getOriginalFilename());
        boolean valid = Arrays.stream(OcrConstants.ALLOWED_FILE_TYPES)
                .anyMatch(t -> t.equalsIgnoreCase(ext));
        if (!valid) {
            throw new OcrException(400, "不支持的文件类型: " + ext);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1).toLowerCase() : "";
    }
}
