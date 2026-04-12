package com.ocr.recognition.confidence;

import com.ocr.common.enums.ConfidenceLevel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfidenceCalculator {

    /**
     * 根据阈值判断置信度级别
     */
    public ConfidenceLevel getLevel(double confidence, double threshold) {
        if (confidence >= threshold) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= 60.0) {
            return ConfidenceLevel.MEDIUM;
        } else {
            return ConfidenceLevel.LOW;
        }
    }

    /**
     * 计算总体置信度（所有字段的加权平均）
     */
    public double calculateOverall(List<Double> confidences) {
        if (confidences == null || confidences.isEmpty()) {
            return 0.0;
        }
        return confidences.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 判断是否可以自动通过
     */
    public boolean canAutoApprove(List<Double> confidences, double threshold, boolean supplierMatched) {
        if (!supplierMatched) {
            return false;
        }
        return confidences.stream().allMatch(c -> c >= threshold);
    }
}
