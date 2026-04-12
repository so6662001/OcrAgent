package com.ocr.recognition.matcher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocr.business.entity.OcrSupplier;
import com.ocr.business.mapper.OcrSupplierMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierMatcher {

    private final OcrSupplierMapper supplierMapper;
    private final ObjectMapper objectMapper;

    @Data
    public static class MatchResult {
        private boolean matched;
        private Long supplierId;
        private String supplierName;
        private String extSupplierId;
        private String supplierCode;
        private String matchType;
        private int matchConfidence;
        private List<Candidate> candidates = new ArrayList<>();

        @Data
        public static class Candidate {
            private Long supplierId;
            private String name;
            private int similarity;
        }
    }

    public MatchResult match(String ocrSupplierText, String tenantId) {
        MatchResult result = new MatchResult();
        if (ocrSupplierText == null || ocrSupplierText.isBlank()) {
            result.setMatched(false);
            return result;
        }

        List<OcrSupplier> suppliers = supplierMapper.selectList(
                new LambdaQueryWrapper<OcrSupplier>()
                        .eq(OcrSupplier::getTenantId, tenantId)
                        .eq(OcrSupplier::getStatus, 1));

        String cleaned = ocrSupplierText.replaceAll("\\s+", "").trim();

        for (OcrSupplier supplier : suppliers) {
            if (supplier.getSupplierName() == null) continue;
            String name = supplier.getSupplierName().replaceAll("\\s+", "").trim();
            if (name.equals(cleaned)) {
                fillResult(result, supplier, "EXACT", 100);
                return result;
            }
        }

        for (OcrSupplier supplier : suppliers) {
            List<String> aliases = parseAliases(supplier.getSupplierAlias());
            for (String alias : aliases) {
                if (alias.replaceAll("\\s+", "").equals(cleaned)) {
                    fillResult(result, supplier, "ALIAS_EXACT", 100);
                    return result;
                }
            }
        }

        for (OcrSupplier supplier : suppliers) {
            String name = supplier.getSupplierName().replaceAll("\\s+", "");
            int similarity = calculateSimilarity(cleaned, name);
            if (similarity >= 85) {
                fillResult(result, supplier, "FUZZY", similarity);
                return result;
            }

            MatchResult.Candidate candidate = new MatchResult.Candidate();
            candidate.setSupplierId(supplier.getId());
            candidate.setName(supplier.getSupplierName());
            candidate.setSimilarity(similarity);
            result.getCandidates().add(candidate);
        }

        result.getCandidates().sort((a, b) -> b.getSimilarity() - a.getSimilarity());
        if (result.getCandidates().size() > 5) {
            result.setCandidates(result.getCandidates().subList(0, 5));
        }

        result.setMatched(false);
        return result;
    }

    private void fillResult(MatchResult result, OcrSupplier supplier, String type, int confidence) {
        result.setMatched(true);
        result.setSupplierId(supplier.getId());
        result.setSupplierName(supplier.getSupplierName());
        result.setExtSupplierId(supplier.getExtSupplierId());
        result.setSupplierCode(supplier.getSupplierCode());
        result.setMatchType(type);
        result.setMatchConfidence(confidence);
    }

    private List<String> parseAliases(String aliasJson) {
        if (aliasJson == null || aliasJson.isEmpty()) return List.of();
        try {
            return objectMapper.readValue(aliasJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Levenshtein距离 → 相似度百分比
     */
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
