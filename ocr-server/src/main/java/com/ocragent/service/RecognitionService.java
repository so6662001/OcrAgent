package com.ocragent.service;

import com.ocragent.model.dto.CorrectionRequest;
import com.ocragent.model.enums.FileType;
import com.ocragent.model.enums.RecognizeMode;
import com.ocragent.model.enums.TaskStatus;
import com.ocragent.model.vo.BatchUploadVO;
import com.ocragent.model.vo.RecognizeTaskVO;
import com.ocragent.ocr.model.OcrRawResult;
import com.ocragent.ocr.model.TextLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecognitionService {

    private final FileProcessingService fileProcessingService;
    private final OcrDispatchService ocrDispatchService;
    private final FieldMappingService fieldMappingService;

    private final Map<String, RecognizeTaskVO> taskStore = new ConcurrentHashMap<>();

    public BatchUploadVO batchUpload(MultipartFile[] files, RecognizeMode mode) {
        BatchUploadVO vo = new BatchUploadVO();
        vo.setTotalFiles(files.length);
        List<String> taskIds = new ArrayList<>();

        for (MultipartFile file : files) {
            fileProcessingService.validateFile(file);
            String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            RecognizeTaskVO task = new RecognizeTaskVO();
            task.setTaskId(taskId);
            task.setFileName(file.getOriginalFilename());
            task.setStatus(TaskStatus.QUEUED);
            taskStore.put(taskId, task);
            taskIds.add(taskId);

            processFileAsync(taskId, file, mode);
        }

        vo.setTaskIds(taskIds);
        return vo;
    }

    @Async("ocrTaskExecutor")
    public void processFileAsync(String taskId, MultipartFile file, RecognizeMode mode) {
        RecognizeTaskVO task = taskStore.get(taskId);
        if (task == null) return;

        task.setStatus(TaskStatus.PROCESSING);
        long startTime = System.currentTimeMillis();

        try {
            byte[] fileData = file.getBytes();
            FileType fileType = fileProcessingService.detectFileType(file);
            fileProcessingService.saveFile(file);

            OcrRawResult rawResult = ocrDispatchService.dispatch(fileData, fileType, mode);

            if (!rawResult.isSuccess()) {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(rawResult.getErrorMessage());
                return;
            }

            task.setOcrApi(rawResult.getApiUsed());
            task.setFields(fieldMappingService.parseToFields(rawResult));
            task.setTableData(fieldMappingService.parseTableData(rawResult));

            StringBuilder sb = new StringBuilder();
            if (rawResult.getLines() != null) {
                for (TextLine line : rawResult.getLines()) {
                    sb.append(line.getText()).append("\n");
                }
            }
            task.setRawText(sb.toString().trim());

            double avgConf = 0;
            int count = 0;
            if (task.getFields() != null) {
                for (var f : task.getFields()) {
                    avgConf += f.getConfidence();
                    count++;
                }
            }
            task.setConfidence(count > 0 ? avgConf / count : 0);
            task.setProcessTimeMs(System.currentTimeMillis() - startTime);
            task.setStatus(TaskStatus.COMPLETED);

            log.info("文件 [{}] 识别完成, 耗时 {}ms, 置信度 {}", task.getFileName(), task.getProcessTimeMs(), task.getConfidence());

        } catch (Exception e) {
            log.error("处理文件 [{}] 异常", task.getFileName(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("处理失败: " + e.getMessage());
            task.setProcessTimeMs(System.currentTimeMillis() - startTime);
        }
    }

    public RecognizeTaskVO getTask(String taskId) {
        return taskStore.get(taskId);
    }

    public List<RecognizeTaskVO> getAllTasks() {
        return new ArrayList<>(taskStore.values());
    }

    public RecognizeTaskVO confirmCorrection(CorrectionRequest request) {
        RecognizeTaskVO task = taskStore.get(request.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + request.getTaskId());
        }

        if (request.getFields() != null) {
            List<RecognizeTaskVO.FieldVO> newFields = new ArrayList<>();
            for (Map<String, Object> fieldMap : request.getFields()) {
                RecognizeTaskVO.FieldVO fv = new RecognizeTaskVO.FieldVO();
                fv.setStandardKey(str(fieldMap.get("standardKey")));
                fv.setDisplayName(str(fieldMap.get("displayName")));
                fv.setOriginalKey(str(fieldMap.get("originalKey")));
                fv.setValue(fieldMap.get("value"));
                fv.setValueType(str(fieldMap.get("valueType")));
                fv.setConfidence(fieldMap.containsKey("confidence")
                        ? ((Number) fieldMap.get("confidence")).doubleValue() : 1.0);

                learnFieldMapping(fv);
                newFields.add(fv);
            }
            task.setFields(newFields);
        }

        if (request.getTableData() != null) {
            List<Map<String, RecognizeTaskVO.FieldVO>> newTable = new ArrayList<>();
            for (Map<String, Object> rowMap : request.getTableData()) {
                Map<String, RecognizeTaskVO.FieldVO> row = new LinkedHashMap<>();
                for (var entry : rowMap.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> cellMap) {
                        RecognizeTaskVO.FieldVO fv = new RecognizeTaskVO.FieldVO();
                        fv.setStandardKey(str(cellMap.get("standardKey")));
                        fv.setDisplayName(str(cellMap.get("displayName")));
                        fv.setOriginalKey(str(cellMap.get("originalKey")));
                        fv.setValue(cellMap.get("value"));
                        fv.setValueType(str(cellMap.get("valueType")));
                        fv.setConfidence(1.0);
                        row.put(entry.getKey(), fv);
                    }
                }
                newTable.add(row);
            }
            task.setTableData(newTable);
        }

        return task;
    }

    private void learnFieldMapping(RecognizeTaskVO.FieldVO field) {
        if (field.getOriginalKey() != null && field.getStandardKey() != null
                && !field.getOriginalKey().equals(field.getStandardKey())) {
            fieldMappingService.addSynonym(field.getStandardKey(), field.getOriginalKey());
        }
    }

    private String str(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
