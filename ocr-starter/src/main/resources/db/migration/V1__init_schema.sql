-- OCR智能识别系统 初始化建表脚本

CREATE TABLE IF NOT EXISTS ocr_document_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    default_threshold DECIMAL(5,2) DEFAULT 95.00,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ocr_field_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_type_id BIGINT NOT NULL,
    field_code VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_type VARCHAR(20) DEFAULT 'STRING',
    position VARCHAR(10) NOT NULL,
    is_required TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    validation_rule VARCHAR(200),
    default_value VARCHAR(200),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_doc_type (doc_type_id)
);

CREATE TABLE IF NOT EXISTS ocr_field_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_id BIGINT NOT NULL,
    supplier_id BIGINT,
    alias_name VARCHAR(100) NOT NULL,
    alias_lang VARCHAR(10) DEFAULT 'zh',
    priority INT DEFAULT 0,
    INDEX idx_field (field_id)
);

CREATE TABLE IF NOT EXISTS ocr_tenant_threshold (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    doc_type_id BIGINT NOT NULL,
    threshold DECIMAL(5,2) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_doctype (tenant_id, doc_type_id)
);

CREATE TABLE IF NOT EXISTS ocr_supplier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    ext_supplier_id VARCHAR(100) NOT NULL,
    supplier_code VARCHAR(50),
    supplier_name VARCHAR(200) NOT NULL,
    supplier_alias VARCHAR(500) DEFAULT '[]',
    contact_person VARCHAR(100),
    phone VARCHAR(50),
    status TINYINT DEFAULT 1,
    synced_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_ext (tenant_id, ext_supplier_id),
    INDEX idx_tenant_name (tenant_id, supplier_name)
);

CREATE TABLE IF NOT EXISTS ocr_supplier_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    supplier_id BIGINT NOT NULL,
    doc_type_id BIGINT NOT NULL,
    template_name VARCHAR(100),
    layout_feature JSON,
    field_mapping JSON,
    sample_file_url VARCHAR(500),
    match_count INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_supplier (tenant_id, supplier_id, doc_type_id)
);

CREATE TABLE IF NOT EXISTS ocr_recognition_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    task_no VARCHAR(50) NOT NULL UNIQUE,
    doc_type_id BIGINT NOT NULL,
    total_files INT DEFAULT 0,
    success_count INT DEFAULT 0,
    review_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    confirmed_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'UPLOADING',
    callback_url VARCHAR(500),
    created_by VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expire_at DATETIME,
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS ocr_recognition_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    task_id BIGINT NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_url VARCHAR(500),
    file_type VARCHAR(20),
    file_size BIGINT,
    page_count INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'PENDING',
    overall_conf DECIMAL(5,2),
    threshold_used DECIMAL(5,2),
    supplier_id BIGINT,
    supplier_matched TINYINT DEFAULT 0,
    result_json JSON,
    review_json JSON,
    ocr_raw_json JSON,
    error_message VARCHAR(500),
    ocr_api_type VARCHAR(50),
    process_time INT,
    baidu_cost DECIMAL(10,4),
    reviewed_by VARCHAR(50),
    reviewed_at DATETIME,
    callback_status VARCHAR(20) DEFAULT 'PENDING',
    callback_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expire_at DATETIME,
    INDEX idx_task (task_id),
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_supplier (supplier_id),
    INDEX idx_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS ocr_recognition_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    field_def_id BIGINT,
    position VARCHAR(10),
    row_index INT,
    ocr_raw_text VARCHAR(500),
    parsed_value VARCHAR(500),
    confidence DECIMAL(5,2),
    status VARCHAR(10),
    is_modified TINYINT DEFAULT 0,
    modified_value VARCHAR(500),
    bounding_box VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_file (file_id)
);

CREATE TABLE IF NOT EXISTS ocr_callback_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    file_id BIGINT NOT NULL,
    callback_url VARCHAR(500),
    request_body TEXT,
    response_body TEXT,
    http_status INT,
    status VARCHAR(20),
    retry_count INT DEFAULT 0,
    error_message VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file (file_id),
    INDEX idx_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS ocr_tenant_storage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL UNIQUE,
    total_space_mb BIGINT DEFAULT 10240,
    used_space_mb BIGINT DEFAULT 0,
    default_retain_days INT DEFAULT 365,
    is_unlimited TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ocr_storage_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    space_mb BIGINT,
    retain_days INT DEFAULT 0,
    amount DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'PAID',
    paid_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
);
