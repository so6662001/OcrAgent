package com.ocragent.service;

import com.ocragent.model.vo.RecognizeTaskVO;
import com.ocragent.ocr.model.OcrRawResult;
import com.ocragent.ocr.model.TextLine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FieldMappingService {

    @Data
    public static class StandardField {
        private String key;
        private String displayName;
        private String valueType;
        private Set<String> synonyms = new LinkedHashSet<>();
    }

    private final Map<String, StandardField> standardFields = new LinkedHashMap<>();
    private final Map<String, String> synonymIndex = new ConcurrentHashMap<>();

    private static final Pattern KV_SEPARATOR = Pattern.compile("[：:]+");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[\\d,]+\\.?\\d*$");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\d{4}[年/\\-.]\\d{1,2}[月/\\-.]\\d{1,2}日?"
    );

    @PostConstruct
    public void init() {
        registerField("document_no", "单据编号", "TEXT",
                "单据编号", "单号", "编号", "No.", "No", "流水号", "凭证号", "票号");
        registerField("date", "日期", "DATE",
                "日期", "开票日期", "单据日期", "出库日期", "入库日期", "开单日期", "Date");
        registerField("product_name", "品名", "TEXT",
                "品名", "品类", "商品名称", "商品名", "货品名称", "物品名", "产品名称",
                "物料名称", "货品", "货物名", "商品", "名称", "品种");
        registerField("quantity", "数量", "NUMBER",
                "数量", "数目", "件数", "个数", "QTY", "qty", "Qty");
        registerField("unit", "单位", "TEXT",
                "单位", "计量单位", "Unit");
        registerField("unit_price", "单价", "DECIMAL",
                "单价", "价格", "售价", "定价", "Price", "含税单价");
        registerField("total_amount", "金额", "DECIMAL",
                "金额", "总额", "合计", "总价", "总计金额", "Amount", "合计金额", "总计");
        registerField("supplier", "供应商", "TEXT",
                "供应商", "供货方", "供货商", "卖方", "供方", "往来单位");
        registerField("buyer", "采购方", "TEXT",
                "采购方", "买方", "购买方", "客户", "收货方");
        registerField("remark", "备注", "TEXT",
                "备注", "说明", "摘要", "Remark");
        registerField("spec", "规格", "TEXT",
                "规格", "规格型号", "型号", "Spec");
    }

    private void registerField(String key, String displayName, String valueType, String... synonyms) {
        StandardField field = new StandardField();
        field.setKey(key);
        field.setDisplayName(displayName);
        field.setValueType(valueType);
        field.getSynonyms().addAll(Arrays.asList(synonyms));
        standardFields.put(key, field);
        for (String s : synonyms) {
            synonymIndex.put(s.toLowerCase().trim(), key);
        }
    }

    private static final int MAX_SYNONYMS_PER_FIELD = 100;
    private static final int MAX_SYNONYM_LENGTH = 128;

    public void addSynonym(String standardKey, String newSynonym) {
        if (standardKey == null || newSynonym == null) return;
        newSynonym = newSynonym.trim();
        if (newSynonym.isEmpty() || newSynonym.length() > MAX_SYNONYM_LENGTH) return;
        StandardField field = standardFields.get(standardKey);
        if (field == null) return;
        if (field.getSynonyms().size() >= MAX_SYNONYMS_PER_FIELD) {
            log.warn("字段 {} 的同义词数已达上限({})", standardKey, MAX_SYNONYMS_PER_FIELD);
            return;
        }
        field.getSynonyms().add(newSynonym);
        synonymIndex.put(newSynonym.toLowerCase().trim(), standardKey);
        String safeSynonym = newSynonym.replaceAll("[\\r\\n\\t]", " ");
        log.info("学习新同义词: {} -> {}", safeSynonym, field.getDisplayName());
    }

    public List<RecognizeTaskVO.FieldVO> parseToFields(OcrRawResult rawResult) {
        List<RecognizeTaskVO.FieldVO> fields = new ArrayList<>();
        if (rawResult.getLines() == null) return fields;

        for (TextLine line : rawResult.getLines()) {
            String text = line.getText().trim();
            if (text.isEmpty()) continue;

            String[] parts = KV_SEPARATOR.split(text, 2);
            if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                String rawKey = parts[0].trim();
                String rawValue = parts[1].trim();

                RecognizeTaskVO.FieldVO fieldVO = new RecognizeTaskVO.FieldVO();
                fieldVO.setOriginalKey(rawKey);

                String matchedKey = matchStandardField(rawKey);
                if (matchedKey != null) {
                    StandardField sf = standardFields.get(matchedKey);
                    fieldVO.setStandardKey(matchedKey);
                    fieldVO.setDisplayName(sf.getDisplayName());
                    fieldVO.setValueType(sf.getValueType());
                    fieldVO.setValue(formatValue(rawValue, sf.getValueType()));
                } else {
                    fieldVO.setStandardKey(rawKey);
                    fieldVO.setDisplayName(rawKey);
                    fieldVO.setValueType("TEXT");
                    fieldVO.setValue(rawValue);
                }
                fieldVO.setConfidence(line.getConfidence());
                fields.add(fieldVO);
            }
        }
        return fields;
    }

    public List<Map<String, RecognizeTaskVO.FieldVO>> parseTableData(OcrRawResult rawResult) {
        List<Map<String, RecognizeTaskVO.FieldVO>> table = new ArrayList<>();
        if (rawResult.getTableRows() == null || rawResult.getTableRows().isEmpty()) return table;

        List<List<TextLine>> rows = rawResult.getTableRows();
        if (rows.size() < 2) return table;

        List<TextLine> headerRow = rows.get(0);
        List<String> headers = new ArrayList<>();
        for (TextLine tl : headerRow) {
            headers.add(tl.getText().trim());
        }

        for (int i = 1; i < rows.size(); i++) {
            List<TextLine> dataRow = rows.get(i);
            Map<String, RecognizeTaskVO.FieldVO> rowMap = new LinkedHashMap<>();
            for (int j = 0; j < Math.min(headers.size(), dataRow.size()); j++) {
                String rawKey = headers.get(j);
                String rawValue = dataRow.get(j).getText().trim();

                RecognizeTaskVO.FieldVO fieldVO = new RecognizeTaskVO.FieldVO();
                fieldVO.setOriginalKey(rawKey);

                String matchedKey = matchStandardField(rawKey);
                if (matchedKey != null) {
                    StandardField sf = standardFields.get(matchedKey);
                    fieldVO.setStandardKey(matchedKey);
                    fieldVO.setDisplayName(sf.getDisplayName());
                    fieldVO.setValueType(sf.getValueType());
                    fieldVO.setValue(formatValue(rawValue, sf.getValueType()));
                } else {
                    fieldVO.setStandardKey(rawKey);
                    fieldVO.setDisplayName(rawKey);
                    fieldVO.setValueType("TEXT");
                    fieldVO.setValue(rawValue);
                }
                fieldVO.setConfidence(dataRow.get(j).getConfidence());
                rowMap.put(fieldVO.getStandardKey(), fieldVO);
            }
            if (!rowMap.isEmpty()) {
                table.add(rowMap);
            }
        }
        return table;
    }

    private String matchStandardField(String rawKey) {
        String normalized = rawKey.toLowerCase().replaceAll("[\\s　]+", "").trim();

        String exact = synonymIndex.get(normalized);
        if (exact != null) return exact;

        exact = synonymIndex.get(rawKey.toLowerCase().trim());
        if (exact != null) return exact;

        for (Map.Entry<String, String> entry : synonymIndex.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry : synonymIndex.entrySet()) {
            if (editDistance(normalized, entry.getKey()) <= 1) {
                return entry.getValue();
            }
        }

        return null;
    }

    private Object formatValue(String rawValue, String valueType) {
        if (rawValue == null) return null;
        return switch (valueType) {
            case "NUMBER" -> {
                String cleaned = rawValue.replaceAll("[,，\\s]", "");
                try {
                    yield Long.parseLong(cleaned);
                } catch (NumberFormatException e) {
                    yield rawValue;
                }
            }
            case "DECIMAL" -> {
                String cleaned = rawValue.replaceAll("[,，\\s¥￥$]", "");
                try {
                    yield Double.parseDouble(cleaned);
                } catch (NumberFormatException e) {
                    yield rawValue;
                }
            }
            default -> rawValue;
        };
    }

    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1,
                        Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
            }
        }
        return dp[a.length()][b.length()];
    }

    public Map<String, StandardField> getStandardFields() {
        return Collections.unmodifiableMap(standardFields);
    }
}
