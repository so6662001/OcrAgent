package com.ocr.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocr.business.entity.OcrRecognitionTask;
import com.ocr.business.mapper.OcrRecognitionTaskMapper;
import com.ocr.common.context.OcrUserContext;
import com.ocr.common.exception.OcrException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final OcrRecognitionTaskMapper taskMapper;

    public Page<OcrRecognitionTask> queryTasks(int page, int size, String status,
                                               String docTypeCode, LocalDate startDate, LocalDate endDate) {
        String tenantId = OcrUserContext.getTenantId();
        LambdaQueryWrapper<OcrRecognitionTask> wrapper = new LambdaQueryWrapper<OcrRecognitionTask>()
                .eq(OcrRecognitionTask::getTenantId, tenantId)
                .eq(status != null && !status.isEmpty(), OcrRecognitionTask::getStatus, status)
                .ge(startDate != null, OcrRecognitionTask::getCreatedAt, startDate != null ? startDate.atStartOfDay() : null)
                .le(endDate != null, OcrRecognitionTask::getCreatedAt, endDate != null ? endDate.plusDays(1).atStartOfDay() : null)
                .orderByDesc(OcrRecognitionTask::getCreatedAt);

        return taskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public OcrRecognitionTask getById(Long taskId) {
        OcrRecognitionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new OcrException(404, "任务不存在: " + taskId);
        }
        return task;
    }

    public OcrRecognitionTask create(OcrRecognitionTask task) {
        taskMapper.insert(task);
        return task;
    }

    public void update(OcrRecognitionTask task) {
        taskMapper.updateById(task);
    }
}
