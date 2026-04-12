package com.ocragent.service;

import com.ocragent.model.dto.CorrectionRequest;
import com.ocragent.model.enums.FileType;
import com.ocragent.model.enums.RecognizeMode;
import com.ocragent.model.enums.TaskStatus;
import com.ocragent.model.vo.BatchUploadVO;
import com.ocragent.model.vo.RecognizeTaskVO;
import com.ocragent.ocr.model.OcrRawResult;
import com.ocragent.ocr.model.TextLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class RecognitionService {

    private final FileProcessingService fileProcessingService;
    private final OcrDispatchService ocrDispatchService;
    private final FieldMappingService fieldMappingService;
    private final Executor ocrTaskExecutor;

    private static final int MAX_TASK_STORE_SIZE = 500;
    private static final int MAX_FIELD_VALUE_LENGTH = 2000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, RecognizeTaskVO> taskStore = new ConcurrentHashMap<>();
    private final Deque<String> taskOrder = new ConcurrentLinkedDeque<>();

    public RecognitionService(
            FileProcessingService fileProcessingService,
            OcrDispatchService ocrDispatchService,
            FieldMappingService fieldMappingService,
            @Qualifier("ocrTaskExecutor") Executor ocrTaskExecutor
    ) {
        this.fileProcessingService = fileProcessingService;
        this.ocrDispatchService = ocrDispatchService;
        this.fieldMappingService = fieldMappingService;
        this.ocrTaskExecutor = ocrTaskExecutor;
    }

    public BatchUploadVO batchUpload(byte[][] filesData, String[] fileNames,
                                     FileType[] fileTypes, RecognizeMode mode) {
        fileProcessingService.validateBatchSize(filesData.length);

        BatchUploadVO vo = new BatchUploadVO();
        vo.setTotalFiles(filesData.length);
        List<String> taskIds = new ArrayList<>();

        for (int i = 0; i < filesData.length; i++) {
            String taskId = generateTaskId();
            RecognizeTaskVO task = new RecognizeTaskVO();
            task.setTaskId(taskId);
            task.setFileName(sanitizeFileName(fileNames[i]));
            task.setStatus(TaskStatus.QUEUED);

            evictOldTasks();
            taskStore.put(taskId, task);
            taskOrder.addLast(taskId);
            taskIds.add(taskId);

            final byte[] data = filesData[i];
            final FileType ft = fileTypes[i];
            ocrTaskExecutor.execute(() -> processFile(taskId, data, ft, mode));
        }

        vo.setTaskIds(taskIds);
        return vo;
    }

    private void processFile(String taskId, byte[] fileData, FileType fileType, RecognizeMode mode) {
        RecognizeTaskVO task = taskStore.get(taskId);
        if (task == null) return;

        task.setStatus(TaskStatus.PROCESSING);
        long startTime = System.currentTimeMillis();

        try {
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

            log.info("识别完成 taskId={}, 耗时={}ms, 置信度={}", taskId, task.getProcessTimeMs(), task.getConfidence());

        } catch (Exception e) {
            log.error("处理异常 taskId={}", taskId, e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("处理失败，请重试");
            task.setProcessTimeMs(System.currentTimeMillis() - startTime);
        }
    }

    public RecognizeTaskVO getTask(String taskId) {
        if (!isValidTaskId(taskId)) return null;
        return taskStore.get(taskId);
    }

    public List<RecognizeTaskVO> getAllTasks() {
        return new ArrayList<>(taskStore.values());
    }

    public RecognizeTaskVO confirmCorrection(CorrectionRequest request) {
        if (!isValidTaskId(request.getTaskId())) {
            throw new IllegalArgumentException("无效的任务ID");
        }
        RecognizeTaskVO task = taskStore.get(request.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        if (request.getFields() != null) {
            List<RecognizeTaskVO.FieldVO> newFields = new ArrayList<>();
            for (Map<String, Object> fieldMap : request.getFields()) {
                RecognizeTaskVO.FieldVO fv = new RecognizeTaskVO.FieldVO();
                fv.setStandardKey(sanitizeStr(fieldMap.get("standardKey"), 64));
                fv.setDisplayName(sanitizeStr(fieldMap.get("displayName"), 128));
                fv.setOriginalKey(sanitizeStr(fieldMap.get("originalKey"), 128));
                fv.setValue(sanitizeStr(fieldMap.get("value"), MAX_FIELD_VALUE_LENGTH));
                fv.setValueType(sanitizeStr(fieldMap.get("valueType"), 16));
                fv.setConfidence(safeDouble(fieldMap.get("confidence"), 1.0));

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
                        fv.setStandardKey(sanitizeStr(cellMap.get("standardKey"), 64));
                        fv.setDisplayName(sanitizeStr(cellMap.get("displayName"), 128));
                        fv.setOriginalKey(sanitizeStr(cellMap.get("originalKey"), 128));
                        fv.setValue(sanitizeStr(cellMap.get("value"), MAX_FIELD_VALUE_LENGTH));
                        fv.setValueType(sanitizeStr(cellMap.get("valueType"), 16));
                        fv.setConfidence(1.0);
                        row.put(sanitizeStr(entry.getKey(), 64), fv);
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

    private String generateTaskId() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private boolean isValidTaskId(String taskId) {
        return taskId != null && taskId.matches("^[a-f0-9]{32}$");
    }

    private void evictOldTasks() {
        while (taskStore.size() >= MAX_TASK_STORE_SIZE && !taskOrder.isEmpty()) {
            String oldest = taskOrder.pollFirst();
            if (oldest != null) {
                taskStore.remove(oldest);
            }
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[\\r\\n\\t]", "_").replaceAll("[^\\p{L}\\p{N}._ \\-]", "_");
    }

    private String sanitizeStr(Object obj, int maxLen) {
        if (obj == null) return null;
        String s = obj.toString();
        if (s.length() > maxLen) s = s.substring(0, maxLen);
        return s;
    }

    private double safeDouble(Object obj, double defaultVal) {
        if (obj instanceof Number n) return n.doubleValue();
        return defaultVal;
    }
}
