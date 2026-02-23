package com.ocr.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocr.business.entity.OcrDocumentType;
import com.ocr.business.entity.OcrFieldDefinition;
import com.ocr.business.mapper.OcrDocumentTypeMapper;
import com.ocr.business.mapper.OcrFieldDefinitionMapper;
import com.ocr.common.exception.OcrException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocTypeService {

    private final OcrDocumentTypeMapper docTypeMapper;
    private final OcrFieldDefinitionMapper fieldDefMapper;

    public List<OcrDocumentType> listAll() {
        return docTypeMapper.selectList(
                new LambdaQueryWrapper<OcrDocumentType>().eq(OcrDocumentType::getStatus, 1));
    }

    public OcrDocumentType getById(Long id) {
        OcrDocumentType type = docTypeMapper.selectById(id);
        if (type == null) {
            throw new OcrException(404, "单据类型不存在: " + id);
        }
        return type;
    }

    public OcrDocumentType getByCode(String typeCode) {
        return docTypeMapper.selectOne(
                new LambdaQueryWrapper<OcrDocumentType>()
                        .eq(OcrDocumentType::getTypeCode, typeCode)
                        .eq(OcrDocumentType::getStatus, 1));
    }

    public OcrDocumentType create(OcrDocumentType docType) {
        if (docType.getDefaultThreshold() == null) {
            docType.setDefaultThreshold(BigDecimal.valueOf(95.00));
        }
        docType.setStatus(1);
        docTypeMapper.insert(docType);
        return docType;
    }

    public void update(OcrDocumentType docType) {
        docTypeMapper.updateById(docType);
    }

    public void delete(Long id) {
        docTypeMapper.deleteById(id);
        fieldDefMapper.delete(
                new LambdaQueryWrapper<OcrFieldDefinition>().eq(OcrFieldDefinition::getDocTypeId, id));
    }

    public List<OcrFieldDefinition> getFields(Long docTypeId) {
        return fieldDefMapper.selectList(
                new LambdaQueryWrapper<OcrFieldDefinition>()
                        .eq(OcrFieldDefinition::getDocTypeId, docTypeId)
                        .eq(OcrFieldDefinition::getStatus, 1)
                        .orderByAsc(OcrFieldDefinition::getSortOrder));
    }

    public OcrFieldDefinition createField(OcrFieldDefinition field) {
        field.setStatus(1);
        fieldDefMapper.insert(field);
        return field;
    }

    public void updateField(OcrFieldDefinition field) {
        fieldDefMapper.updateById(field);
    }

    public void deleteField(Long id) {
        fieldDefMapper.deleteById(id);
    }
}
