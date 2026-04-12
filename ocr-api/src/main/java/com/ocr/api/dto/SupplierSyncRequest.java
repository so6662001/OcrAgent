package com.ocr.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class SupplierSyncRequest {
    private String tenantId;
    private List<SupplierItem> suppliers;

    @Data
    public static class SupplierItem {
        private String extSupplierId;
        private String supplierCode;
        private String supplierName;
        private List<String> supplierAlias;
        private String contactPerson;
        private String phone;
    }
}
