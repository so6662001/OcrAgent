package com.ocr.business.controller;

import com.ocr.business.entity.OcrDocumentType;
import com.ocr.business.entity.OcrFieldDefinition;
import com.ocr.business.service.DocTypeService;
import com.ocr.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ocr/config")
@RequiredArgsConstructor
public class DocTypeController {

    private final DocTypeService docTypeService;

    @GetMapping("/doc-types")
    public R<List<OcrDocumentType>> list() {
        return R.ok(docTypeService.listAll());
    }

    @PostMapping("/doc-type")
    public R<OcrDocumentType> create(@RequestBody OcrDocumentType docType) {
        return R.ok(docTypeService.create(docType));
    }

    @PutMapping("/doc-type/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody OcrDocumentType docType) {
        docType.setId(id);
        docTypeService.update(docType);
        return R.ok();
    }

    @DeleteMapping("/doc-type/{id}")
    public R<Void> delete(@PathVariable Long id) {
        docTypeService.delete(id);
        return R.ok();
    }

    @GetMapping("/doc-type/{id}/fields")
    public R<List<OcrFieldDefinition>> getFields(@PathVariable Long id) {
        return R.ok(docTypeService.getFields(id));
    }

    @PostMapping("/field")
    public R<OcrFieldDefinition> createField(@RequestBody OcrFieldDefinition field) {
        return R.ok(docTypeService.createField(field));
    }

    @PutMapping("/field/{id}")
    public R<Void> updateField(@PathVariable Long id, @RequestBody OcrFieldDefinition field) {
        field.setId(id);
        docTypeService.updateField(field);
        return R.ok();
    }

    @DeleteMapping("/field/{id}")
    public R<Void> deleteField(@PathVariable Long id) {
        docTypeService.deleteField(id);
        return R.ok();
    }
}
