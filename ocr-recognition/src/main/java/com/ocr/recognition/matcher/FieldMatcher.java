package com.ocr.recognition.matcher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocr.business.entity.OcrFieldAlias;
import com.ocr.business.entity.OcrFieldDefinition;
import com.ocr.business.mapper.OcrFieldAliasMapper;
import com.ocr.business.mapper.OcrFieldDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FieldMatcher {

    private final OcrFieldDefinitionMapper fieldDefMapper;
    private final OcrFieldAliasMapper fieldAliasMapper;

    /**
     * 将OCR识别出的标签名映射到系统定义的字段
     */
    public OcrFieldDefinition matchField(String ocrLabel, Long docTypeId) {
        if (ocrLabel == null || ocrLabel.isBlank() || docTypeId == null) {
            return null;
        }

        List<OcrFieldDefinition> fields = fieldDefMapper.selectList(
                new LambdaQueryWrapper<OcrFieldDefinition>()
                        .eq(OcrFieldDefinition::getDocTypeId, docTypeId)
                        .eq(OcrFieldDefinition::getStatus, 1));

        String cleaned = ocrLabel.replaceAll("[：:：\\s]", "").trim().toLowerCase();

        for (OcrFieldDefinition field : fields) {
            if (field.getFieldName() == null) continue;
            if (field.getFieldName().replaceAll("\\s+", "").equalsIgnoreCase(cleaned)) {
                return field;
            }
        }

        for (OcrFieldDefinition field : fields) {
            List<OcrFieldAlias> aliases = fieldAliasMapper.selectList(
                    new LambdaQueryWrapper<OcrFieldAlias>()
                            .eq(OcrFieldAlias::getFieldId, field.getId()));
            for (OcrFieldAlias alias : aliases) {
                if (alias.getAliasName() == null) continue;
                if (alias.getAliasName().replaceAll("\\s+", "").equalsIgnoreCase(cleaned)) {
                    return field;
                }
            }
        }

        for (OcrFieldDefinition field : fields) {
            if (field.getFieldName() == null) continue;
            int similarity = calculateSimilarity(cleaned,
                    field.getFieldName().replaceAll("\\s+", "").toLowerCase());
            if (similarity >= 80) {
                return field;
            }
        }

        return null;
    }

    private int calculateSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 100;
        int distance = levenshtein(s1, s2);
        return (int) ((1.0 - (double) distance / maxLen) * 100);
    }

    private int levenshtein(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
