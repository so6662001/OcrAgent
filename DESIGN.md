# 图片单据智能识别系统 — 系统设计文档

## 一、项目概述

### 1.1 项目背景

企业日常经营中存在大量纸质单据（采购单、出库单、入库单、报销单等），这些单据可能是**机打印刷体**或**手写体**，形式包括拍照图片和PDF文件。需要一套智能识别系统，能够：

- 自动识别单据图片/PDF中的文字内容
- 将识别结果结构化为JSON输出
- 具备字段智能映射能力（如"品类"="品名"="商品名称"）
- 具备机器自动学习能力，随使用而持续优化

### 1.2 核心需求

| 序号 | 需求项 | 说明 |
|------|--------|------|
| 1 | 多格式输入 | 支持拍照图片（JPG/PNG）、扫描件、PDF文件 |
| 2 | 手写+机打 | 同时支持手写体和印刷体单据识别 |
| 3 | JSON结构化输出 | 识别结果以标准JSON格式返回 |
| 4 | 字段智能映射 | 自动识别同义字段（品类↔品名↔商品名称等） |
| 5 | 自动学习能力 | 人工纠错后系统自动学习，持续提升准确率 |
| 6 | 百度OCR集成 | 基于百度AI开放平台的OCR能力 |

### 1.3 技术选型

| 层次 | 技术方案 | 版本/说明 |
|------|----------|-----------|
| 后端框架 | Spring Boot | 3.x |
| 前端框架 | Vue 3 | + Vite + Element Plus |
| OCR引擎 | 百度AI OCR | 通用文字识别(高精度) + 手写文字识别 + iOCR自定义模板 + 文档抽取 |
| 数据库 | MySQL | 8.x，存储模板、映射规则、学习样本 |
| 缓存 | Redis | Token缓存、识别结果缓存 |
| 文件存储 | MinIO / 本地磁盘 | 上传文件和识别结果存储 |
| 构建工具 | Maven + npm | 后端Maven，前端npm |

---

## 二、系统架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         前端 (Vue 3 + Element Plus)                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────────┐   │
│  │ 文件上传  │  │ 识别结果  │  │ 字段映射  │  │ 学习样本管理/纠错 │   │
│  │   组件    │  │ 展示组件  │  │ 配置组件  │  │      组件        │   │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────────┘   │
└─────────────────────────┬───────────────────────────────────────────┘
                          │ HTTP REST API (JSON)
┌─────────────────────────▼───────────────────────────────────────────┐
│                      后端 (Spring Boot 3.x)                         │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     API Gateway 层                           │   │
│  │  FileUploadController / RecognitionController /              │   │
│  │  FieldMappingController / LearningController                 │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                              │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐   │
│  │                     业务服务层 (Service)                      │   │
│  │                                                              │   │
│  │  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌──────────────┐  │   │
│  │  │ 文件处理  │ │  OCR调度  │ │ 字段智能   │ │  自动学习    │  │   │
│  │  │ Service  │ │ Service  │ │ 映射Service│ │  Service     │  │   │
│  │  └──────────┘ └──────────┘ └───────────┘ └──────────────┘  │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                              │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐   │
│  │                     OCR引擎适配层                             │   │
│  │                                                              │   │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌────────────┐  │   │
│  │  │ 通用文字   │ │ 手写文字   │ │  iOCR     │ │  文档抽取   │  │   │
│  │  │ 识别适配器 │ │ 识别适配器 │ │ 模板适配器 │ │  适配器    │  │   │
│  │  └───────────┘ └───────────┘ └───────────┘ └────────────┘  │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                              │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐   │
│  │                     数据持久层                                │   │
│  │  MySQL (模板/映射规则/学习样本)  +  Redis (Token/缓存)        │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │   百度AI开放平台    │
                    │  OCR API Services  │
                    └───────────────────┘
```

### 2.2 核心处理流程

```
用户上传文件
      │
      ▼
┌──────────────┐
│  文件预处理   │ ← 格式检测、PDF拆页、图片压缩/旋转矫正
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌────────────────┐
│  类型判断    │────→│  是否有匹配模板？│
└──────────────┘     └───────┬────────┘
                         │        │
                        有       无
                         │        │
                         ▼        ▼
              ┌──────────────┐  ┌──────────────────┐
              │ iOCR模板识别  │  │ 自动识别策略选择   │
              └──────┬───────┘  │  ┌──────────────┐ │
                     │          │  │ 机打→通用高精度│ │
                     │          │  │ 手写→手写识别  │ │
                     │          │  │ PDF→文档抽取   │ │
                     │          │  └──────────────┘ │
                     │          └────────┬─────────┘
                     │                   │
                     ▼                   ▼
              ┌──────────────────────────────┐
              │      原始OCR结果 (文本行)      │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │    字段智能映射与结构化解析     │
              │  ┌────────────────────────┐  │
              │  │ 1. 基于规则的KV提取     │  │
              │  │ 2. 同义词字段映射        │  │
              │  │ 3. 基于学习模型的字段    │  │
              │  │    自动归类              │  │
              │  └────────────────────────┘  │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │    输出结构化JSON结果          │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │    用户确认/纠错              │
              │    (纠错数据 → 自动学习模块)   │
              └──────────────────────────────┘
```

---

## 三、模块详细设计

### 3.1 文件处理模块 (FileProcessingService)

**职责**：接收上传文件，进行预处理，为OCR识别做准备。

#### 功能清单

| 功能 | 说明 |
|------|------|
| 文件格式校验 | 支持 JPG/JPEG/PNG/BMP/TIFF/PDF，最大 10MB |
| PDF拆页 | 使用 Apache PDFBox 将多页PDF拆分为单页图片 |
| 图片预处理 | 旋转矫正、去噪、压缩（通过百度OCR参数控制） |
| 文件类型检测 | 自动判断是图片还是PDF，路由到不同处理链路 |
| 文件存储 | 保存原始文件和处理后文件，生成唯一文件标识 |

#### 类设计

```
FileProcessingService
├── uploadAndProcess(MultipartFile file): FileInfo
├── detectFileType(byte[] data): FileType          // ENUM: IMAGE, PDF
├── splitPdfToImages(byte[] pdfData): List<byte[]>  // PDF拆分为图片
├── preprocessImage(byte[] imageData): byte[]       // 图片预处理
└── saveFile(byte[] data, String fileName): String  // 存储并返回路径
```

### 3.2 OCR调度模块 (OcrDispatchService)

**职责**：根据文件类型和单据特征，选择最合适的百度OCR API进行识别。

#### 百度OCR API 调用策略

| 场景 | 百度OCR API | 接口地址 |
|------|-------------|----------|
| 机打单据（通用） | 通用文字识别（高精度版） | `/rest/2.0/ocr/v1/accurate_basic` |
| 手写单据 | 手写文字识别 | `/rest/2.0/ocr/v1/handwriting` |
| 有模板的固定版式 | iOCR自定义模板识别 | `/rest/2.0/solution/v1/iocr/recognise` |
| PDF文档 | 文档抽取API | `/rest/2.0/ocr/v1/doc_analysis` |
| 表格型单据 | 表格文字识别 | `/rest/2.0/ocr/v1/table` |
| 混合场景 | 自动分类→调用对应API | 分类器自动路由 |

#### 识别策略选择逻辑

```
OcrDispatchService
├── dispatch(FileInfo fileInfo, RecognizeRequest request): OcrRawResult
│   ├── 若用户指定模板 → 调用iOCR模板识别
│   ├── 若为PDF文件 → 调用文档抽取API
│   ├── 若用户标注手写 → 调用手写识别API
│   ├── 若自动模式 → 先调用通用高精度识别，根据置信度决定是否回退到手写识别
│   └── 若命中已有模板（通过分类器） → 自动使用iOCR识别
│
├── callGeneralOcr(byte[] image): OcrRawResult       // 通用高精度识别
├── callHandwritingOcr(byte[] image): OcrRawResult    // 手写识别
├── callIocrTemplate(byte[] image, String templateId): OcrRawResult  // 模板识别
├── callDocAnalysis(byte[] pdf): OcrRawResult          // PDF文档抽取
└── callTableOcr(byte[] image): OcrRawResult           // 表格识别
```

#### Access Token 管理

```
BaiduTokenManager
├── getAccessToken(): String
│   ├── 优先从Redis读取缓存的Token
│   ├── 若过期则重新获取（OAuth2.0 client_credentials）
│   └── 新Token存入Redis，设置TTL=29天
├── refreshToken(): String
└── 配置项：
    ├── baidu.ocr.app-id
    ├── baidu.ocr.api-key
    └── baidu.ocr.secret-key
```

### 3.3 字段智能映射模块 (FieldMappingService)

**职责**：将OCR返回的原始文本行，解析为结构化的键值对，并进行同义字段归一化映射。

#### 3.3.1 映射流程

```
OCR原始文本行
      │
      ▼
┌──────────────┐
│  KV对提取    │ ← 基于冒号分隔、空格分隔、位置关系等规则提取Key-Value对
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  字段归一化   │ ← 将Key映射到标准字段名（同义词表 + 学习模型）
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  值校验&格式化│ ← 数字格式化、日期解析、单位统一
└──────┬───────┘
       │
       ▼
  结构化JSON
```

#### 3.3.2 字段同义词映射机制

系统维护一个**字段同义词映射表**，支持动态扩展和自动学习：

```json
{
  "standard_fields": {
    "product_name": {
      "display_name": "品名",
      "synonyms": ["品类", "品名", "商品名称", "商品名", "货品名称", "物品名", "产品名称", "物料名称"],
      "value_type": "TEXT"
    },
    "quantity": {
      "display_name": "数量",
      "synonyms": ["数量", "数目", "件数", "个数", "QTY", "qty"],
      "value_type": "NUMBER"
    },
    "unit_price": {
      "display_name": "单价",
      "synonyms": ["单价", "价格", "售价", "定价", "Price"],
      "value_type": "DECIMAL"
    },
    "total_amount": {
      "display_name": "金额",
      "synonyms": ["金额", "总额", "合计", "总价", "总计金额", "Amount"],
      "value_type": "DECIMAL"
    },
    "date": {
      "display_name": "日期",
      "synonyms": ["日期", "开票日期", "单据日期", "Date", "出库日期", "入库日期"],
      "value_type": "DATE"
    },
    "supplier": {
      "display_name": "供应商",
      "synonyms": ["供应商", "供货方", "供货商", "卖方", "供方"],
      "value_type": "TEXT"
    },
    "document_no": {
      "display_name": "单据编号",
      "synonyms": ["单据编号", "单号", "编号", "No.", "No", "流水号"],
      "value_type": "TEXT"
    }
  }
}
```

#### 3.3.3 类设计

```
FieldMappingService
├── parseToStructuredJson(OcrRawResult rawResult): DocumentResult
│   ├── extractKeyValuePairs(List<TextLine> lines): List<KVPair>
│   ├── mapToStandardField(String rawKey): StandardField
│   │   ├── 精确匹配 → 命中同义词表
│   │   ├── 模糊匹配 → 编辑距离/Jaccard相似度
│   │   └── 模型预测 → 基于历史学习数据的分类模型
│   ├── formatValue(String rawValue, ValueType type): Object
│   └── buildJsonResult(List<MappedField> fields): DocumentResult
│
├── addSynonym(String standardField, String newSynonym): void   // 动态添加同义词
├── getSynonymConfig(): SynonymConfig                            // 获取当前映射配置
└── updateSynonymConfig(SynonymConfig config): void              // 更新映射配置
```

### 3.4 自动学习模块 (AutoLearningService)

**职责**：基于用户纠错数据，自动优化识别和映射的准确率。这是系统的**核心差异化能力**。

#### 3.4.1 学习机制总览

```
┌────────────────────────────────────────────────────────────────────┐
│                        自动学习系统                                 │
│                                                                    │
│  ┌─────────────┐   ┌──────────────┐   ┌────────────────────────┐  │
│  │  样本采集    │   │   模型训练    │   │   模型应用 & 反馈      │  │
│  │             │   │              │   │                        │  │
│  │ 用户纠错数据 │──→│ 定时批量训练  │──→│ 实时预测 + 置信度评估   │  │
│  │ 历史识别记录 │   │ 增量更新模型  │   │ 低置信度标记人工审核    │  │
│  └─────────────┘   └──────────────┘   └────────────────────────┘  │
│                                                                    │
│  学习维度：                                                        │
│  ① 字段映射学习 — 新的同义词/字段名自动归类                          │
│  ② OCR后处理纠错 — 常见误识别字符的自动修正                          │
│  ③ 模板布局学习 — 新单据版式自动生成iOCR模板                        │
│  ④ 识别策略学习 — 自动选择最优OCR API的策略优化                      │
└────────────────────────────────────────────────────────────────────┘
```

#### 3.4.2 学习维度详解

**维度一：字段映射自动学习**

当用户修正了一个字段的映射关系时（比如将"货物名"修正映射到"品名"），系统自动将"货物名"添加到 `product_name` 的同义词列表中。

```
用户纠错: "货物名" → 应映射到 "品名(product_name)"
    │
    ▼
系统自动执行:
  1. 将 "货物名" 加入 product_name 的 synonyms 列表
  2. 记录此次纠错样本到学习数据库
  3. 当同类纠错累积 N 次，提升该映射的置信度权重
```

**维度二：OCR后处理纠错学习**

```
用户纠错: OCR识别 "数晕: 1O0" → 用户修正为 "数量: 100"
    │
    ▼
系统学习:
  1. 记录字符级纠错: "晕"→"量", "O"→"0" (在数字上下文中)
  2. 生成纠错规则: 当字段类型为NUMBER时, "O"→"0", "l"→"1", "S"→"5"
  3. 后续自动应用该纠错规则
```

**维度三：模板布局自动学习**

```
当同一版式的单据被识别 ≥ 3 次且用户纠错率 < 10% 时:
    │
    ▼
系统自动:
  1. 提取该版式的布局特征（字段位置、锚点）
  2. 自动调用百度iOCR API创建自定义模板
  3. 后续同版式单据自动使用该模板识别（准确率提升10~20%）
```

**维度四：识别策略自动学习**

```
系统记录每次识别所用的API和最终准确率:
    │
    ▼
统计分析:
  1. 某类单据用 handwriting API 平均准确率 92%，用 accurate_basic 仅 78%
  2. 自动调整策略: 该类单据优先使用 handwriting API
```

#### 3.4.3 类设计

```
AutoLearningService
├── recordCorrection(CorrectionRecord record): void
│   ├── CorrectionRecord 包含:
│   │   ├── originalResult (原始识别结果)
│   │   ├── correctedResult (用户修正后的结果)
│   │   ├── fileId (原始文件标识)
│   │   └── correctionType (FIELD_MAPPING / OCR_TEXT / LAYOUT / STRATEGY)
│   │
│   ├── 存储纠错样本到数据库
│   ├── 实时更新同义词映射（维度一）
│   └── 实时更新字符纠错规则（维度二）
│
├── triggerTraining(): void                  // 触发批量训练（定时任务或手动触发）
│   ├── trainFieldMappingModel()            // 训练字段映射分类模型
│   ├── trainOcrPostProcessor()             // 训练OCR后处理纠错模型
│   ├── generateTemplates()                 // 自动生成iOCR模板
│   └── optimizeDispatchStrategy()          // 优化API调度策略
│
├── getModelMetrics(): ModelMetrics          // 获取模型指标（准确率、召回率等）
├── getLearningHistory(): List<LearningLog> // 学习历史记录
└── exportTrainingData(): byte[]            // 导出训练数据
```

#### 3.4.4 学习数据存储模型

```sql
-- 纠错样本表
CREATE TABLE correction_samples (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id         VARCHAR(64) NOT NULL,
    correction_type ENUM('FIELD_MAPPING', 'OCR_TEXT', 'LAYOUT', 'STRATEGY'),
    original_key    VARCHAR(255),
    original_value  TEXT,
    corrected_key   VARCHAR(255),
    corrected_value TEXT,
    confidence      DECIMAL(5,4),
    applied         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 字段映射学习模型表
CREATE TABLE field_mapping_rules (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    standard_field  VARCHAR(64) NOT NULL,
    synonym         VARCHAR(255) NOT NULL,
    source          ENUM('PRESET', 'USER_CORRECTION', 'MODEL_LEARNED'),
    confidence      DECIMAL(5,4) DEFAULT 1.0,
    hit_count       INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_field_synonym (standard_field, synonym)
);

-- OCR字符纠错规则表
CREATE TABLE ocr_correction_rules (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrong_char      VARCHAR(10) NOT NULL,
    correct_char    VARCHAR(10) NOT NULL,
    context_type    ENUM('NUMBER', 'DATE', 'TEXT', 'ANY'),
    confidence      DECIMAL(5,4) DEFAULT 1.0,
    hit_count       INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 识别策略统计表
CREATE TABLE recognition_strategy_stats (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_type   VARCHAR(64),
    ocr_api_used    VARCHAR(64),
    accuracy_rate   DECIMAL(5,4),
    sample_count    INT DEFAULT 0,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 四、数据模型设计

### 4.1 核心实体

#### 识别记录 (RecognitionRecord)

```sql
CREATE TABLE recognition_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id         VARCHAR(64) NOT NULL UNIQUE,
    file_name       VARCHAR(255),
    file_type       ENUM('IMAGE', 'PDF'),
    file_path       VARCHAR(512),
    file_size       BIGINT,
    ocr_api_used    VARCHAR(64),
    template_id     VARCHAR(64),
    raw_result      JSON,
    structured_result JSON,
    confidence_avg  DECIMAL(5,4),
    status          ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CORRECTED'),
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### iOCR模板 (OcrTemplate)

```sql
CREATE TABLE ocr_templates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name   VARCHAR(128) NOT NULL,
    template_sign   VARCHAR(128),
    classifier_id   VARCHAR(128),
    document_type   VARCHAR(64),
    fields_config   JSON,
    source          ENUM('MANUAL', 'AUTO_LEARNED'),
    sample_count    INT DEFAULT 0,
    accuracy_rate   DECIMAL(5,4),
    status          ENUM('ACTIVE', 'INACTIVE', 'TRAINING'),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.2 JSON输出格式设计

识别结果的标准JSON输出格式：

```json
{
  "code": 200,
  "message": "识别成功",
  "data": {
    "file_id": "f20260328001",
    "file_name": "采购单_20260328.jpg",
    "recognition_type": "GENERAL_ACCURATE",
    "confidence": 0.9523,
    "document_type": "采购单",
    "fields": [
      {
        "standard_key": "document_no",
        "display_name": "单据编号",
        "original_key": "单号",
        "value": "CG-2026-00381",
        "confidence": 0.98,
        "value_type": "TEXT",
        "position": { "left": 120, "top": 50, "width": 200, "height": 30 }
      },
      {
        "standard_key": "date",
        "display_name": "日期",
        "original_key": "开票日期",
        "value": "2026-03-28",
        "confidence": 0.96,
        "value_type": "DATE",
        "position": { "left": 400, "top": 50, "width": 150, "height": 30 }
      },
      {
        "standard_key": "supplier",
        "display_name": "供应商",
        "original_key": "供货方",
        "value": "XX贸易有限公司",
        "confidence": 0.94,
        "value_type": "TEXT",
        "position": { "left": 120, "top": 100, "width": 300, "height": 30 }
      }
    ],
    "table_data": [
      {
        "row_index": 1,
        "columns": {
          "product_name": { "value": "A4打印纸", "original_key": "品名", "confidence": 0.97 },
          "quantity": { "value": 100, "original_key": "数量", "confidence": 0.95 },
          "unit": { "value": "箱", "original_key": "单位", "confidence": 0.99 },
          "unit_price": { "value": 45.00, "original_key": "单价", "confidence": 0.93 },
          "total_amount": { "value": 4500.00, "original_key": "金额", "confidence": 0.92 }
        }
      },
      {
        "row_index": 2,
        "columns": {
          "product_name": { "value": "签字笔", "original_key": "品名", "confidence": 0.96 },
          "quantity": { "value": 200, "original_key": "数量", "confidence": 0.94 },
          "unit": { "value": "支", "original_key": "单位", "confidence": 0.98 },
          "unit_price": { "value": 3.50, "original_key": "单价", "confidence": 0.91 },
          "total_amount": { "value": 700.00, "original_key": "金额", "confidence": 0.90 }
        }
      }
    ],
    "raw_text": "采购单\n单号: CG-2026-00381  开票日期: 2026年3月28日\n...",
    "metadata": {
      "page_count": 1,
      "ocr_api": "accurate_basic",
      "process_time_ms": 1230,
      "image_width": 1920,
      "image_height": 1080
    }
  }
}
```

---

## 五、API接口设计

### 5.1 接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/recognize/upload` | 上传文件并识别 |
| GET | `/api/v1/recognize/{fileId}` | 查询识别结果 |
| GET | `/api/v1/recognize/history` | 识别历史列表 |
| POST | `/api/v1/recognize/correct` | 提交纠错数据 |
| GET | `/api/v1/field-mapping/config` | 获取字段映射配置 |
| PUT | `/api/v1/field-mapping/config` | 更新字段映射配置 |
| POST | `/api/v1/field-mapping/synonym` | 添加同义词 |
| GET | `/api/v1/learning/metrics` | 获取学习模型指标 |
| GET | `/api/v1/learning/history` | 学习历史记录 |
| POST | `/api/v1/learning/trigger-training` | 手动触发训练 |
| GET | `/api/v1/templates` | 获取模板列表 |
| POST | `/api/v1/templates` | 创建自定义模板 |

### 5.2 核心接口详细设计

#### 5.2.1 上传文件并识别

```
POST /api/v1/recognize/upload
Content-Type: multipart/form-data

参数:
  file          (必填) 上传的文件（图片或PDF）
  recognize_mode (选填) 识别模式: AUTO / GENERAL / HANDWRITING / TEMPLATE
  template_id    (选填) 指定iOCR模板ID（recognize_mode=TEMPLATE时必填）
  document_type  (选填) 单据类型提示: 采购单 / 出库单 / 入库单 / 报销单 / 其他

响应:
{
  "code": 200,
  "message": "识别成功",
  "data": { ... }  // 见4.2节JSON输出格式
}
```

#### 5.2.2 提交纠错数据

```
POST /api/v1/recognize/correct
Content-Type: application/json

请求体:
{
  "file_id": "f20260328001",
  "corrections": [
    {
      "correction_type": "FIELD_MAPPING",
      "field_path": "fields[0]",
      "original_key": "品类",
      "corrected_standard_key": "product_name",
      "original_value": null,
      "corrected_value": null
    },
    {
      "correction_type": "OCR_TEXT",
      "field_path": "table_data[1].columns.quantity",
      "original_key": null,
      "corrected_standard_key": null,
      "original_value": "1O0",
      "corrected_value": "100"
    }
  ]
}

响应:
{
  "code": 200,
  "message": "纠错已记录，系统将自动学习",
  "data": {
    "corrections_applied": 2,
    "model_updated": true,
    "new_synonyms_added": ["品类 → product_name"]
  }
}
```

#### 5.2.3 获取学习模型指标

```
GET /api/v1/learning/metrics

响应:
{
  "code": 200,
  "data": {
    "total_samples": 1520,
    "total_corrections": 238,
    "field_mapping_accuracy": 0.9412,
    "ocr_post_correction_rate": 0.0315,
    "auto_templates_count": 5,
    "top_learned_synonyms": [
      { "synonym": "货物名", "standard_field": "product_name", "hit_count": 45 },
      { "synonym": "供货方", "standard_field": "supplier", "hit_count": 32 }
    ],
    "accuracy_trend": [
      { "week": "2026-W10", "accuracy": 0.85 },
      { "week": "2026-W11", "accuracy": 0.89 },
      { "week": "2026-W12", "accuracy": 0.92 },
      { "week": "2026-W13", "accuracy": 0.94 }
    ]
  }
}
```

---

## 六、前端设计 (Vue 3)

### 6.1 页面结构

```
┌─────────────────────────────────────────────────────┐
│                    顶部导航栏                        │
│   Logo   单据识别   字段配置   学习中心   系统设置    │
├─────────────────────────────────────────────────────┤
│                                                     │
│  页面路由:                                           │
│                                                     │
│  /recognize        ← 单据上传与识别（主页面）         │
│  /recognize/:id    ← 识别结果详情与纠错              │
│  /history          ← 识别历史记录                    │
│  /field-config     ← 字段映射配置管理                │
│  /learning         ← 学习中心（模型指标与训练管理）    │
│  /templates        ← 模板管理                       │
│  /settings         ← 系统设置（百度OCR密钥等）       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 6.2 核心页面设计

#### 页面一：单据上传与识别 (`/recognize`)

```
┌─────────────────────────────────────────────────────────────────┐
│                       单据智能识别                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────┐  ┌──────────────────────┐ │
│  │                                 │  │   识别设置           │ │
│  │     📄 拖拽文件到此处上传        │  │                      │ │
│  │     或 点击选择文件              │  │  识别模式:           │ │
│  │                                 │  │  ○ 自动识别          │ │
│  │  支持: JPG/PNG/PDF  最大10MB    │  │  ○ 通用印刷体        │ │
│  │                                 │  │  ○ 手写体            │ │
│  │  [  已选: 采购单_001.jpg  ]     │  │  ○ 指定模板          │ │
│  │                                 │  │                      │ │
│  │     [ 开始识别 ]                │  │  单据类型(可选):     │ │
│  │                                 │  │  [  请选择 ▼  ]     │ │
│  └─────────────────────────────────┘  └──────────────────────┘ │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  📊 识别结果预览                                         │  │
│  │                                                          │  │
│  │  ┌────────────────────┐  ┌─────────────────────────────┐│  │
│  │  │                    │  │  结构化数据 (JSON)           ││  │
│  │  │   原始图片预览      │  │                             ││  │
│  │  │   (标注识别区域)    │  │  单据编号: CG-2026-00381   ││  │
│  │  │                    │  │  日期: 2026-03-28           ││  │
│  │  │                    │  │  供应商: XX贸易有限公司      ││  │
│  │  │                    │  │  ─────────────────────      ││  │
│  │  │                    │  │  品名    数量  单价  金额    ││  │
│  │  │                    │  │  A4纸    100   45   4500   ││  │
│  │  │                    │  │  签字笔  200   3.5  700    ││  │
│  │  │                    │  │                             ││  │
│  │  │                    │  │  [查看原始JSON] [下载JSON]  ││  │
│  │  └────────────────────┘  └─────────────────────────────┘│  │
│  │                                                          │  │
│  │  置信度: ████████░░ 95.23%     [ 确认正确 ] [ 去纠错 ]   │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

#### 页面二：识别结果纠错 (`/recognize/:id`)

```
┌─────────────────────────────────────────────────────────────────┐
│                     识别结果纠错                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  左侧: 原始图片 (可缩放)        右侧: 可编辑的结构化字段        │
│                                                                 │
│  ┌──────────────────┐  ┌─────────────────────────────────────┐ │
│  │                  │  │                                     │ │
│  │  [原始图片]       │  │  字段名映射          识别值          │ │
│  │   点击图片区域    │  │  ┌──────┐ → ┌──────┐ ┌───────────┐ │ │
│  │   高亮对应字段    │  │  │品类  │   │品名✎│ │A4打印纸  ✎│ │ │
│  │                  │  │  └──────┘   └──────┘ └───────────┘ │ │
│  │                  │  │  ┌──────┐ → ┌──────┐ ┌───────────┐ │ │
│  │                  │  │  │数晕  │   │数量✎│ │1O0     ✎│ │ │
│  │                  │  │  └──────┘   └──────┘ └───────────┘ │ │
│  │                  │  │       ⚠ 低置信度，请确认              │ │
│  │                  │  │                                     │ │
│  │                  │  │  [提交纠错并学习]  [跳过]             │ │
│  └──────────────────┘  └─────────────────────────────────────┘ │
│                                                                 │
│  💡 提示: 您的纠错将帮助系统自动学习，提升后续识别准确率          │
└─────────────────────────────────────────────────────────────────┘
```

#### 页面三：学习中心 (`/learning`)

```
┌─────────────────────────────────────────────────────────────────┐
│                       学习中心                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │ 总样本数  │  │ 纠错次数  │  │ 映射准确率│  │ 自动模板数   │   │
│  │  1,520   │  │   238    │  │  94.12%  │  │     5       │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  📈 准确率趋势图                                         │  │
│  │                                           ___            │  │
│  │                                     ___--/               │  │
│  │                              ___--/                      │  │
│  │                        __--/                             │  │
│  │                  __--/                                   │  │
│  │            __--/                                         │  │
│  │      __--/                                               │  │
│  │  --/                                                     │  │
│  │  ├────┼────┼────┼────┼────┼────┼────┼────┤              │  │
│  │  W6   W7   W8   W9  W10  W11  W12  W13                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────┐  ┌────────────────────────┐  │
│  │  🏷 已学习的同义词映射 (TOP10)│  │  🔧 操作               │  │
│  │                              │  │                        │  │
│  │  货物名 → 品名 (45次)        │  │  [ 手动触发训练 ]      │  │
│  │  供货方 → 供应商 (32次)       │  │  [ 导出训练数据 ]      │  │
│  │  入库号 → 单据编号 (28次)     │  │  [ 重置学习模型 ]      │  │
│  │  ...                         │  │                        │  │
│  └──────────────────────────────┘  └────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 前端技术栈

| 技术 | 用途 |
|------|------|
| Vue 3 + Composition API | 核心框架 |
| Vue Router 4 | 路由管理 |
| Pinia | 状态管理 |
| Element Plus | UI组件库 |
| Axios | HTTP请求 |
| ECharts | 学习指标图表 |
| vue-pdf-embed | PDF预览 |
| Vite | 构建工具 |

### 6.4 前端组件树

```
App.vue
├── layouts/
│   └── MainLayout.vue              // 主布局（导航栏 + 侧边栏 + 内容区）
├── views/
│   ├── RecognizePage.vue            // 上传与识别主页
│   ├── RecognizeDetailPage.vue      // 结果详情与纠错
│   ├── HistoryPage.vue              // 历史记录
│   ├── FieldConfigPage.vue          // 字段映射配置
│   ├── LearningPage.vue             // 学习中心
│   ├── TemplatePage.vue             // 模板管理
│   └── SettingsPage.vue             // 系统设置
├── components/
│   ├── FileUploader.vue             // 文件上传组件（拖拽+点击）
│   ├── ImagePreview.vue             // 图片预览（支持标注框高亮）
│   ├── StructuredResult.vue         // 结构化结果展示
│   ├── JsonViewer.vue               // JSON原始数据查看器
│   ├── CorrectionForm.vue           // 纠错表单
│   ├── ConfidenceBadge.vue          // 置信度标识
│   ├── AccuracyTrendChart.vue       // 准确率趋势图
│   └── SynonymTable.vue             // 同义词映射表格
├── api/
│   ├── recognize.ts                 // 识别相关API
│   ├── fieldMapping.ts              // 字段映射API
│   └── learning.ts                  // 学习中心API
├── stores/
│   ├── recognizeStore.ts            // 识别状态
│   └── learningStore.ts             // 学习状态
└── types/
    ├── recognize.ts                 // 识别相关类型定义
    └── fieldMapping.ts              // 映射相关类型定义
```

---

## 七、后端项目结构

```
ocr-agent-server/
├── pom.xml
├── src/main/java/com/ocragent/
│   ├── OcrAgentApplication.java
│   ├── config/
│   │   ├── BaiduOcrConfig.java           // 百度OCR配置
│   │   ├── RedisConfig.java              // Redis配置
│   │   ├── WebMvcConfig.java             // CORS等Web配置
│   │   └── AsyncConfig.java              // 异步任务配置
│   ├── controller/
│   │   ├── RecognitionController.java    // 识别相关接口
│   │   ├── FieldMappingController.java   // 字段映射接口
│   │   ├── LearningController.java       // 学习中心接口
│   │   └── TemplateController.java       // 模板管理接口
│   ├── service/
│   │   ├── FileProcessingService.java    // 文件处理
│   │   ├── OcrDispatchService.java       // OCR调度
│   │   ├── FieldMappingService.java      // 字段映射
│   │   ├── AutoLearningService.java      // 自动学习
│   │   └── TemplateService.java          // 模板管理
│   ├── ocr/
│   │   ├── BaiduTokenManager.java        // Token管理
│   │   ├── BaiduOcrClient.java           // 百度OCR调用封装
│   │   ├── adapter/
│   │   │   ├── OcrAdapter.java           // OCR适配器接口
│   │   │   ├── GeneralOcrAdapter.java    // 通用识别适配器
│   │   │   ├── HandwritingOcrAdapter.java// 手写识别适配器
│   │   │   ├── TemplateOcrAdapter.java   // 模板识别适配器
│   │   │   ├── DocAnalysisAdapter.java   // 文档抽取适配器
│   │   │   └── TableOcrAdapter.java      // 表格识别适配器
│   │   └── model/
│   │       ├── OcrRawResult.java         // OCR原始结果
│   │       └── TextLine.java             // 文本行
│   ├── learning/
│   │   ├── FieldMappingLearner.java      // 字段映射学习器
│   │   ├── OcrPostCorrectionLearner.java // OCR后处理纠错学习器
│   │   ├── TemplateLearner.java          // 模板布局学习器
│   │   ├── StrategyLearner.java          // 策略学习器
│   │   └── model/
│   │       ├── CorrectionRecord.java     // 纠错记录
│   │       └── LearningMetrics.java      // 学习指标
│   ├── mapper/
│   │   ├── FieldMappingRuleMapper.java   // 映射规则DAO
│   │   ├── CorrectionSampleMapper.java   // 纠错样本DAO
│   │   ├── RecognitionRecordMapper.java  // 识别记录DAO
│   │   └── OcrTemplateMapper.java        // 模板DAO
│   ├── model/
│   │   ├── entity/                       // 数据库实体
│   │   ├── dto/                          // 数据传输对象
│   │   ├── vo/                           // 视图对象
│   │   └── enums/                        // 枚举定义
│   └── common/
│       ├── Result.java                   // 统一响应体
│       ├── GlobalExceptionHandler.java   // 全局异常处理
│       └── Constants.java                // 常量定义
├── src/main/resources/
│   ├── application.yml                   // 主配置
│   ├── application-dev.yml               // 开发环境配置
│   ├── mapper/                           // MyBatis XML映射
│   └── synonym-preset.json              // 预设同义词表
└── src/test/java/
    └── com/ocragent/                     // 单元测试
```

---

## 八、关键技术方案

### 8.1 手写体 vs 机打体自动判别

```
判别策略:
1. 用户可手动指定识别模式（手写/机打/自动）
2. 自动模式下:
   a. 先调用通用高精度OCR识别
   b. 分析返回结果中的平均置信度
   c. 若平均置信度 < 0.80，可能是手写体 → 回退调用手写识别API
   d. 综合两次结果，取置信度更高者
3. 随着学习样本积累，逐步优化判别阈值
```

### 8.2 PDF处理方案

```
PDF处理链路:
1. 使用 Apache PDFBox 判断PDF类型:
   a. 文本型PDF → 直接提取文本 + 调百度文档抽取API做补充
   b. 扫描型PDF → 逐页转为图片 → 走图片识别链路
2. 多页PDF → 每页独立识别 → 合并为统一JSON结果
3. 大文件分片处理，支持异步识别
```

### 8.3 字段映射的匹配算法

```
匹配优先级（从高到低）:
1. 精确匹配: rawKey 完全等于某个 synonym → 直接映射
2. 归一化匹配: 去除空格/标点后匹配 → 如"品  名" → "品名"
3. 包含匹配: rawKey 包含某个 synonym → 如"商品品名" 包含 "品名"
4. 编辑距离匹配: 编辑距离 ≤ 2 → 如"品明"(OCR误识别) → "品名"
5. 学习模型匹配: 基于历史纠错数据训练的分类模型预测

每一级匹配都附带置信度分数，低于阈值的标记为待人工确认。
```

### 8.4 自动学习的触发机制

```
┌──────────────────────────────────────────┐
│            触发条件                       │
├──────────────────────────────────────────┤
│  实时学习（即时生效）:                    │
│  • 新同义词添加 → 立即加入映射表          │
│  • 字符纠错规则 → 立即生效                │
│                                          │
│  批量训练（定时/手动触发）:               │
│  • 每日凌晨2:00自动训练                   │
│  • 累积纠错样本 ≥ 50条 时触发             │
│  • 管理员手动触发                         │
│                                          │
│  模板自动生成（条件触发）:                │
│  • 同版式单据出现 ≥ 3次                  │
│  • 用户纠错率 < 10%                      │
│  • 管理员确认后生效                       │
└──────────────────────────────────────────┘
```

---

## 九、部署架构

```
┌──────────────────────────────────────────────────────────┐
│                     生产环境部署                          │
│                                                          │
│  ┌─────────┐    ┌────────────┐    ┌──────────────────┐  │
│  │  Nginx   │───→│ Vue3 前端   │    │ Spring Boot 后端 │  │
│  │  反向代理 │    │ (静态资源)  │    │   (JAR部署)      │  │
│  └─────────┘    └────────────┘    └────────┬─────────┘  │
│       │                                     │            │
│       │              ┌──────────────────────┤            │
│       │              │                      │            │
│       │         ┌────▼─────┐         ┌──────▼──────┐    │
│       │         │  MySQL   │         │   Redis     │    │
│       │         │  8.x     │         │   缓存      │    │
│       │         └──────────┘         └─────────────┘    │
│       │                                                  │
│       └──────────→ 百度AI OCR API (外网)                 │
└──────────────────────────────────────────────────────────┘
```

---

## 十、开发里程碑规划

| 阶段 | 内容 | 涉及模块 |
|------|------|----------|
| **P1 - 基础识别** | 文件上传 + 百度OCR通用/手写识别 + JSON输出 | FileProcessingService, OcrDispatchService, 前端上传与结果展示 |
| **P2 - 字段映射** | 字段智能映射（同义词表 + 规则匹配）+ 配置管理 | FieldMappingService, 前端字段配置页 |
| **P3 - 纠错与学习** | 用户纠错 + 实时学习（同义词/字符纠错） | AutoLearningService, 前端纠错页 |
| **P4 - 高级学习** | 批量训练 + 模板自动生成 + 策略优化 | Learning子模块, iOCR集成 |
| **P5 - 完善优化** | PDF支持完善 + 性能优化 + 学习中心仪表盘 | 全模块优化, 前端学习中心页 |

---

## 十一、风险与注意事项

| 风险项 | 说明 | 应对措施 |
|--------|------|----------|
| 百度OCR API调用限制 | 免费额度有限，QPS有上限 | 实现请求队列 + 限流 + 结果缓存 |
| 手写体识别准确率 | 手写体天然准确率较低（~90%） | 结合自动学习持续优化 + 人工纠错兜底 |
| 字段映射歧义 | 不同单据中相同词可能含义不同 | 引入单据类型上下文辅助判断 |
| 学习数据质量 | 错误的纠错数据可能污染模型 | 置信度机制 + 管理员审核 + 异常检测 |
| Token安全 | 百度API Key/Secret不可泄露 | 后端存储，前端不接触；环境变量注入 |
