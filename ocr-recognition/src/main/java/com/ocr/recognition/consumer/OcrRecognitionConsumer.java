package com.ocr.recognition.consumer;

import com.ocr.business.entity.OcrRecognitionFile;
import com.ocr.business.entity.OcrRecognitionTask;
import com.ocr.business.mapper.OcrRecognitionFileMapper;
import com.ocr.business.mapper.OcrRecognitionTaskMapper;
import com.ocr.common.constants.OcrConstants;
import com.ocr.recognition.engine.MixedOcrStrategy;
import com.ocr.recognition.matcher.SupplierMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrRecognitionConsumer {

    private final OcrRecognitionFileMapper fileMapper;
    private final OcrRecognitionTaskMapper taskMapper;
    private final MixedOcrStrategy mixedOcrStrategy;
    private final SupplierMatcher supplierMatcher;

    @RabbitListener(queues = OcrConstants.MQ_QUEUE_RECOGNIZE, concurrency = "5-20")
    public void onMessage(Map<String, Object> message) {
        Long fileId = ((Number) message.get("fileId")).longValue();
        Long taskId = ((Number) message.get("taskId")).longValue();
        String tenantId = (String) message.get("tenantId");
        double threshold = ((Number) message.get("threshold")).doubleValue();

        log.info("开始OCR识别: fileId={}, taskId={}", fileId, taskId);
        long startTime = System.currentTimeMillis();

        OcrRecognitionFile file = fileMapper.selectById(fileId);
        if (file == null) {
            log.warn("文件记录不存在: {}", fileId);
            return;
        }

        try {
            file.setStatus("RECOGNIZING");
            fileMapper.updateById(file);

            // TODO: 从MinIO获取文件数据并调用OCR
            // 目前先标记为需要审核，待百度OCR Key配置后完整集成
            file.setStatus("NEED_REVIEW");
            file.setOverallConf(BigDecimal.valueOf(85.0));
            file.setProcessTime((int) (System.currentTimeMillis() - startTime));
            file.setOcrApiType("baidu_mixed");
            fileMapper.updateById(file);

            updateTaskStats(taskId);

        } catch (Exception e) {
            log.error("OCR识别失败: fileId={}", fileId, e);
            file.setStatus("FAILED");
            file.setErrorMessage(e.getMessage());
            file.setProcessTime((int) (System.currentTimeMillis() - startTime));
            fileMapper.updateById(file);

            updateTaskStats(taskId);
        }
    }

    private void updateTaskStats(Long taskId) {
        OcrRecognitionTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        long success = fileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OcrRecognitionFile>()
                        .eq(OcrRecognitionFile::getTaskId, taskId)
                        .eq(OcrRecognitionFile::getStatus, "SUCCESS"));
        long review = fileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OcrRecognitionFile>()
                        .eq(OcrRecognitionFile::getTaskId, taskId)
                        .eq(OcrRecognitionFile::getStatus, "NEED_REVIEW"));
        long failed = fileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OcrRecognitionFile>()
                        .eq(OcrRecognitionFile::getTaskId, taskId)
                        .eq(OcrRecognitionFile::getStatus, "FAILED"));
        long confirmed = fileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OcrRecognitionFile>()
                        .eq(OcrRecognitionFile::getTaskId, taskId)
                        .eq(OcrRecognitionFile::getStatus, "CONFIRMED"));

        task.setSuccessCount((int) success);
        task.setReviewCount((int) review);
        task.setFailedCount((int) failed);
        task.setConfirmedCount((int) confirmed);

        long total = success + review + failed + confirmed;
        long pending = fileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OcrRecognitionFile>()
                        .eq(OcrRecognitionFile::getTaskId, taskId)
                        .in(OcrRecognitionFile::getStatus, "PENDING", "PREPROCESSING", "RECOGNIZING"));

        if (pending == 0) {
            if (failed == task.getTotalFiles()) {
                task.setStatus("DONE");
            } else if (review > 0) {
                task.setStatus("PARTIAL_DONE");
            } else {
                task.setStatus("DONE");
            }
        }

        taskMapper.updateById(task);
    }
}
