package com.ocr.demo.mock;

import com.ocr.business.entity.*;
import com.ocr.business.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataInitializer implements CommandLineRunner {

    private final OcrDocumentTypeMapper docTypeMapper;
    private final OcrFieldDefinitionMapper fieldDefMapper;
    private final OcrFieldAliasMapper fieldAliasMapper;
    private final OcrSupplierMapper supplierMapper;

    @Override
    public void run(String... args) {
        if (docTypeMapper.selectCount(null) > 0) {
            log.info("测试数据已存在，跳过初始化");
            return;
        }
        log.info("开始初始化测试数据...");
        initDocTypes();
        initSuppliers();
        log.info("测试数据初始化完成");
    }

    private void initDocTypes() {
        // 采购进货单
        OcrDocumentType purchaseOrder = new OcrDocumentType();
        purchaseOrder.setTypeCode("PURCHASE_ORDER");
        purchaseOrder.setTypeName("采购进货单");
        purchaseOrder.setDescription("采购进货单据");
        purchaseOrder.setDefaultThreshold(BigDecimal.valueOf(95.00));
        purchaseOrder.setStatus(1);
        docTypeMapper.insert(purchaseOrder);

        addField(purchaseOrder.getId(), "order_no", "订单编号", "STRING", "HEADER", 1, 1);
        addField(purchaseOrder.getId(), "order_date", "订单日期", "DATE", "HEADER", 1, 2);
        addField(purchaseOrder.getId(), "supplier_name", "供应商名称", "STRING", "HEADER", 1, 3);
        addField(purchaseOrder.getId(), "total_amount", "合计金额", "AMOUNT", "HEADER", 0, 4);
        addField(purchaseOrder.getId(), "product_name", "商品名称", "STRING", "BODY", 1, 1);
        addField(purchaseOrder.getId(), "specification", "规格型号", "STRING", "BODY", 0, 2);
        addField(purchaseOrder.getId(), "quantity", "数量", "NUMBER", "BODY", 1, 3);
        addField(purchaseOrder.getId(), "unit_price", "单价", "AMOUNT", "BODY", 1, 4);
        addField(purchaseOrder.getId(), "amount", "金额", "AMOUNT", "BODY", 1, 5);

        addAlias("supplier_name", "供货商", "zh");
        addAlias("supplier_name", "供方", "zh");
        addAlias("supplier_name", "Vendor", "en");
        addAlias("supplier_name", "Supplier", "en");
        addAlias("total_amount", "合计", "zh");
        addAlias("total_amount", "Total", "en");
        addAlias("product_name", "品名", "zh");
        addAlias("product_name", "Product", "en");

        // 进项发票
        OcrDocumentType invoice = new OcrDocumentType();
        invoice.setTypeCode("INPUT_INVOICE");
        invoice.setTypeName("进项发票");
        invoice.setDescription("增值税进项发票");
        invoice.setDefaultThreshold(BigDecimal.valueOf(95.00));
        invoice.setStatus(1);
        docTypeMapper.insert(invoice);

        addField(invoice.getId(), "invoice_no", "发票号码", "STRING", "HEADER", 1, 1);
        addField(invoice.getId(), "invoice_date", "开票日期", "DATE", "HEADER", 1, 2);
        addField(invoice.getId(), "seller_name", "销售方名称", "STRING", "HEADER", 1, 3);
        addField(invoice.getId(), "buyer_name", "购买方名称", "STRING", "HEADER", 1, 4);
        addField(invoice.getId(), "tax_amount", "税额", "AMOUNT", "HEADER", 1, 5);
        addField(invoice.getId(), "total_amount", "价税合计", "AMOUNT", "HEADER", 1, 6);
        addField(invoice.getId(), "product_name", "货物名称", "STRING", "BODY", 1, 1);
        addField(invoice.getId(), "quantity", "数量", "NUMBER", "BODY", 0, 2);
        addField(invoice.getId(), "unit_price", "单价", "AMOUNT", "BODY", 0, 3);
        addField(invoice.getId(), "amount", "金额", "AMOUNT", "BODY", 1, 4);

        // 询价清单
        OcrDocumentType inquiry = new OcrDocumentType();
        inquiry.setTypeCode("INQUIRY_LIST");
        inquiry.setTypeName("询价清单");
        inquiry.setDescription("供应商询价清单");
        inquiry.setDefaultThreshold(BigDecimal.valueOf(95.00));
        inquiry.setStatus(1);
        docTypeMapper.insert(inquiry);

        addField(inquiry.getId(), "inquiry_no", "询价单号", "STRING", "HEADER", 1, 1);
        addField(inquiry.getId(), "inquiry_date", "询价日期", "DATE", "HEADER", 1, 2);
        addField(inquiry.getId(), "supplier_name", "供应商", "STRING", "HEADER", 1, 3);
        addField(inquiry.getId(), "product_name", "品名", "STRING", "BODY", 1, 1);
        addField(inquiry.getId(), "specification", "规格", "STRING", "BODY", 0, 2);
        addField(inquiry.getId(), "quantity", "数量", "NUMBER", "BODY", 1, 3);
        addField(inquiry.getId(), "quoted_price", "报价", "AMOUNT", "BODY", 1, 4);
    }

    private Long lastFieldId;

    private void addField(Long docTypeId, String code, String name, String type,
                          String position, int required, int sortOrder) {
        OcrFieldDefinition field = new OcrFieldDefinition();
        field.setDocTypeId(docTypeId);
        field.setFieldCode(code);
        field.setFieldName(name);
        field.setFieldType(type);
        field.setPosition(position);
        field.setIsRequired(required);
        field.setSortOrder(sortOrder);
        field.setStatus(1);
        fieldDefMapper.insert(field);
        lastFieldId = field.getId();
    }

    private void addAlias(String fieldCode, String alias, String lang) {
        OcrFieldAlias a = new OcrFieldAlias();
        a.setFieldId(lastFieldId);
        a.setAliasName(alias);
        a.setAliasLang(lang);
        a.setPriority(0);
        fieldAliasMapper.insert(a);
    }

    private void initSuppliers() {
        addSupplier("T001", "SUP001", "GYS001", "深圳市XX科技有限公司",
                "[\"深圳XX科技\",\"XX科技\"]", "张三", "13800138001");
        addSupplier("T001", "SUP002", "GYS002", "广州YY电子有限公司",
                "[\"广州YY电子\",\"YY电子\"]", "李四", "13800138002");
        addSupplier("T001", "SUP003", "GYS003", "东莞ZZ贸易有限公司",
                "[\"东莞ZZ贸易\"]", "王五", "13800138003");
        addSupplier("T002", "SUP010", "GYS010", "上海AA实业有限公司",
                "[\"上海AA实业\"]", "赵六", "13800138010");
    }

    private void addSupplier(String tenantId, String extId, String code, String name,
                             String alias, String contact, String phone) {
        OcrSupplier s = new OcrSupplier();
        s.setTenantId(tenantId);
        s.setExtSupplierId(extId);
        s.setSupplierCode(code);
        s.setSupplierName(name);
        s.setSupplierAlias(alias);
        s.setContactPerson(contact);
        s.setPhone(phone);
        s.setStatus(1);
        s.setSyncedAt(LocalDateTime.now());
        supplierMapper.insert(s);
    }
}
