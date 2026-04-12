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
import com.ocr.file.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
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

    @Autowired(required = false)
    private MinioStorageService minioStorageService;

    @PostMapping("/recognize")
    public R<Map<String, Object>> recognize(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("docTypeCode") String docTypeCode,
            @RequestParam("callbackUrl") String callbackUrl) {

        String tenantId = OcrUserContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            throw new OcrException(401, "未登录或租户信息缺失");
        }
        if (files == null || files.length == 0) {
            throw new OcrException(400, "请至少上传一个文件");
        }
        if (files.length > OcrConstants.MAX_UPLOAD_FILES) {
            throw new OcrException(400, "单次最多上传" + OcrConstants.MAX_UPLOAD_FILES + "个文件");
        }
        validateCallbackUrl(callbackUrl);

        OcrDocumentType docType = docTypeService.getByCode(docTypeCode);
        if (docType == null) {
            throw new OcrException(400, "单据类型不存在: " + docTypeCode);
        }

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

            String fileUrl = null;
            if (minioStorageService != null) {
                try {
                    fileUrl = minioStorageService.upload(file, tenantId);
                } catch (Exception e) {
                    log.error("文件上传MinIO失败: {}", file.getOriginalFilename(), e);
                }
            }

            OcrRecognitionFile fileRecord = new OcrRecognitionFile();
            fileRecord.setTenantId(tenantId);
            fileRecord.setTaskId(task.getId());
            fileRecord.setFileName(file.getOriginalFilename());
            fileRecord.setFileUrl(fileUrl);
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
            message.put("fileUrl", fileUrl);

            try {
                rabbitTemplate.convertAndSend(OcrConstants.MQ_EXCHANGE,
                        OcrConstants.MQ_ROUTING_KEY_RECOGNIZE, message);
            } catch (Exception e) {
                log.error("MQ发送失败，文件ID: {}，标记为失败", fileRecord.getId(), e);
                fileRecord.setStatus("FAILED");
                fileRecord.setErrorMessage("消息队列发送失败: " + e.getMessage());
                fileMapper.updateById(fileRecord);
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

    private void validateCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new OcrException(400, "回调地址不能为空");
        }
        try {
            URL url = new URL(callbackUrl);
            String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new OcrException(400, "回调地址协议必须为http或https");
            }
            String host = url.getHost();
            if (host == null || host.isEmpty()) {
                throw new OcrException(400, "回调地址主机名无效");
            }
            if ("127.0.0.1".equals(host) || "0.0.0.0".equals(host) || host.startsWith("169.254.") || host.startsWith("10.")) {
                if (!callbackUrl.contains("localhost") && !callbackUrl.contains("demo")) {
                    log.warn("回调地址指向内网IP: {}", callbackUrl);
                }
            }
        } catch (MalformedURLException e) {
            throw new OcrException(400, "回调地址格式无效: " + callbackUrl);
        }
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
