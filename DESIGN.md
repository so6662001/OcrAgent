# OCR智能识别系统 — 架构设计文档（v2.0）

## 1. 项目概述

### 1.1 业务背景

企业在日常运营中需要处理大量纸质/电子单据（进货单、进项发票、询价清单、供应商价格表等），目前依赖人工录入，效率低且易出错。本系统旨在通过百度OCR技术实现单据自动识别与数据提取，作为**已有系统的一个嵌入式模块**运行，大幅提升业务效率。

### 1.2 核心需求

| 需求项 | 描述 |
|--------|------|
| 万能识别 | 支持PDF、图片（机打/手写），处理模糊、扭曲等异常场景 |
| 百度OCR混合调用 | 机打+扭曲/模糊、手写文字识别、文档结构化，三合一混合调用 |
| 可配置对象 | 单据类型及其字段在系统中可自定义配置 |
| 多租户支持 | 每个租户可独立设置识别阈值、购买存储空间 |
| 供应商智能匹配 | OCR识别出供应商名称后自动与外部系统传入的供应商数据比对匹配 |
| 智能分流 | 识别率≥阈值（默认95%，租户可按对象自定义）自动回调，低于阈值人工审核 |
| 批量处理 | 支持批量/单个文件识别，上传时只选单据类型，不选供应商 |
| 多端支持 | PC端 (Element Plus) + H5移动端 (Vant) |
| 嵌入式模块 | 作为已有系统的子模块集成，非独立应用 |
| 多语言 | 支持中文 + 英文单据识别 |
| SSO对接 | 用户体系对接现有统一认证系统 |
| 数据留存 | 默认保留1年，租户可充值购买存储空间延长至无限期 |
| 高并发 | 支持1万并发 |

### 1.3 已确认决策

| 决策项 | 确认结果 |
|--------|----------|
| OCR引擎 | 百度付费OCR，混合调用模式 |
| 供应商选择 | 上传时不选供应商，OCR识别后自动匹配外部系统传入的供应商 |
| 阈值机制 | 默认95%，每个租户可针对不同识别对象自定义阈值 |
| 前端框架 | Vue 3 + Element Plus(PC) + Vant(H5) |
| 回调方式 | HTTP回调，回调接口规范由本系统定义 |
| 部署环境 | K8s集群 |
| 用户体系 | 对接现有SSO统一认证 |
| 项目形态 | 嵌入已有系统的模块 |
| 多语言 | 中文 + 英文 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     已有业务系统 (宿主系统)                               │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                  OCR识别模块 (嵌入式)                               │  │
│  │                                                                   │  │
│  │  ┌─────────────────────┐                                          │  │
│  │  │  前端组件            │  Vue3 + Element Plus / Vant              │  │
│  │  │  (嵌入宿主前端)      │  以路由模块/微前端方式嵌入                  │  │
│  │  └──────────┬──────────┘                                          │  │
│  │             │ HTTPS / WebSocket                                   │  │
│  │             ▼                                                     │  │
│  │  ┌──────────────────────────────────────────────────────────────┐ │  │
│  │  │              API网关层 (复用宿主系统网关 或 独立路由前缀)       │ │  │
│  │  │              /api/ocr/** → OCR模块                            │ │  │
│  │  └──────────────────────────┬───────────────────────────────────┘ │  │
│  │                             │                                     │  │
│  │       ┌─────────────────────┼─────────────────────┐               │  │
│  │       ▼                     ▼                     ▼               │  │
│  │ ┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐     │  │
│  │ │  文件服务      │  │  OCR识别服务      │  │  业务管理服务     │     │  │
│  │ │              │  │                  │  │                  │     │  │
│  │ │ · 文件上传    │  │ · 百度OCR调用     │  │ · 对象/字段配置   │     │  │
│  │ │ · MinIO存储   │  │ · 混合识别策略    │  │ · 租户阈值管理   │     │  │
│  │ │ · PDF解析     │  │ · 结果结构化      │  │ · 供应商匹配     │     │  │
│  │ │ · 批量管理    │  │ · 置信度评估      │  │ · 任务/文件管理   │     │  │
│  │ └──────┬───────┘  └────────┬─────────┘  │ · 人工审核        │     │  │
│  │        │                   │             │ · 数据查询        │     │  │
│  │        │                   │             │ · HTTP回调        │     │  │
│  │        ▼                   ▼             └────────┬─────────┘     │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                         基础设施层                                   ││
│  │  MinIO  ·  MySQL  ·  Redis  ·  RabbitMQ  ·  Elasticsearch          ││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                │                                        │
│  ┌─────────────────────────────┼───────────────────────────────────────┐│
│  │       宿主系统已有能力        │                                      ││
│  │  SSO统一认证 · 租户管理 · 供应商主数据 · ERP/WMS/财务系统              ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        百度OCR云服务                                     │
│          通用文字识别 · 手写文字识别 · 文档结构化识别                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

由于是**嵌入已有系统**，不再拆为独立微服务，而是以**Maven多模块**方式组织，打包时可独立部署也可嵌入宿主系统。

| 模块名 | 职责 | 技术选型 |
|--------|------|----------|
| `ocr-common` | 公共模块（DTO、枚举、工具类、异常） | Java Library |
| `ocr-api` | 对外暴露的API接口定义（供宿主系统引用） | Java Library |
| `ocr-file` | 文件上传、存储、格式转换、PDF拆页 | Spring Boot Starter + MinIO |
| `ocr-recognition` | 百度OCR调用、混合识别、结果结构化、置信度评估 | Spring Boot Starter + 百度OCR SDK |
| `ocr-business` | 对象配置、字段管理、租户阈值、供应商匹配、任务管理、人工审核、数据查询、HTTP回调 | Spring Boot Starter + MyBatis-Plus |
| `ocr-starter` | Spring Boot Starter自动配置，宿主系统引入此依赖即可启用OCR模块 | Spring Boot Starter |

### 2.3 嵌入方式

```
宿主系统 pom.xml:
  <dependency>
    <groupId>com.ocr</groupId>
    <artifactId>ocr-starter</artifactId>
    <version>1.0.0</version>
  </dependency>

嵌入后效果:
  · 自动注册 /api/ocr/** 路由
  · 自动创建所需数据库表（Flyway/Liquibase）
  · 自动配置消息队列消费者
  · 宿主系统SSO Token直接复用，无需重复认证
  · 前端以路由模块方式挂载到宿主系统菜单下
```

---

## 3. 核心流程设计

### 3.1 主流程 — 文件识别全链路

```
用户上传文件（单个/批量）
  │  只需选择「单据类型」
  │  不需要选择供应商（供应商由OCR识别结果自动匹配）
  │
  ▼
┌──────────────────┐
│ 文件接收存储       │ ← 存入MinIO，生成文件记录
│ 记录租户ID+任务ID  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ 文件预处理         │ ← PDF拆页转图片、图片格式标准化
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────────┐
│  百度OCR混合调用                      │
│                                      │
│  Step1: 文档类型智能判断              │
│    ├─ 含表格 → 文档结构化识别          │
│    ├─ 机打文本 → 通用文字识别(高精度)   │
│    ├─ 手写文本 → 手写文字识别          │
│    └─ 混合内容 → 多接口组合调用        │
│                                      │
│  Step2: 对扭曲/模糊图片启用增强参数    │
│    · detect_direction=true (方向检测)  │
│    · paragraph=true (段落输出)         │
│    · language_type=CHN_ENG (中英混合)  │
└──────────┬───────────────────────────┘
           │
           ▼
┌──────────────────────┐
│ 结构化数据提取         │ ← 根据对象定义提取表头/表体字段
│ · 字段智能匹配         │ ← 处理不同供应商字段名称差异
│ · 置信度计算           │ ← 每个字段独立计算置信度
└──────────┬───────────┘
           │
           ▼
┌────────────────────────────────────┐
│ 供应商自动匹配                      │
│                                    │
│ 将OCR识别出的供应商名称/编码         │
│ 与外部系统同步的供应商主数据比对      │
│                                    │
│ 匹配策略:                           │
│  1. 精确匹配供应商名称               │
│  2. 模糊匹配(去空格/简繁转换/别名)    │
│  3. 匹配供应商编码                   │
│  4. 未匹配到 → 标记"供应商未匹配"     │
│     允许用户在审核页面手动选择        │
└──────────┬─────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │ 置信度判断     │
    │ 所有字段≥阈值？│ ← 阈值=租户针对该对象的自定义值(默认95%)
    │ 且供应商已匹配？│
    └──┬────────┬──┘
      Yes       No
       │         │
       ▼         ▼
┌──────────┐ ┌──────────────────────────────┐
│ 自动回调  │ │ 进入人工审核                   │
│ 外部系统  │ │ · 低置信度字段红色高亮           │
│ (HTTP)   │ │ · 供应商未匹配时提醒并可手动选择  │
└──────────┘ └──────────┬───────────────────┘
                        │
                        ▼
                ┌──────────────┐
                │ 用户人工修正  │
                │ 确认提交      │
                └──────┬───────┘
                       │
                       ▼
                ┌──────────────┐
                │ HTTP回调      │
                │ 外部系统      │
                └──────────────┘
```

### 3.2 置信度评估与阈值机制

```
置信度级别定义（颜色展示）:
  ├─ 高置信度 (≥阈值)    → 绿色显示，可自动通过
  ├─ 中置信度 (60%~阈值) → 橙色显示，需人工确认
  └─ 低置信度 (<60%)     → 红色显示，重点需人工修正

阈值配置（多租户 × 多对象）:
  ┌─────────────────────────────────────────────────┐
  │  租户A:                                          │
  │    采购进货单 → 阈值 95%                          │
  │    进项发票   → 阈值 98%  (发票要求更严格)         │
  │    询价清单   → 阈值 90%  (容忍度较高)             │
  │                                                  │
  │  租户B:                                          │
  │    采购进货单 → 阈值 92%                          │
  │    进项发票   → 阈值 95%                          │
  │                                                  │
  │  未配置的 → 使用系统默认阈值 95%                   │
  └─────────────────────────────────────────────────┘

自动通过条件（同时满足）:
  1. 所有字段置信度均≥该租户该对象的阈值
  2. 供应商已成功匹配到外部系统中的供应商
  3. 数据校验全部通过（类型校验、必填校验、逻辑校验）
```

### 3.3 供应商匹配流程

```
外部系统 ──同步──→ ocr_supplier(供应商主数据表)
                         │
                         │  比对
                         ▼
            OCR识别出的供应商字段值
            (如: "深圳市XX科技有限公司")
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
    ┌──────────┐  ┌──────────┐  ┌──────────┐
    │ 精确匹配  │  │ 模糊匹配  │  │ 编码匹配  │
    │ 全名一致  │  │ 相似度   │  │ 编码一致  │
    │          │  │ >85%    │  │          │
    └────┬─────┘  └────┬─────┘  └────┬─────┘
         │             │             │
         └──────┬──────┘──────┬──────┘
                │             │
          匹配成功?      匹配失败?
                │             │
                ▼             ▼
        填入supplier_id    标记"供应商未匹配"
        正常流转           → 进入人工审核
                           → 页面展示供应商下拉选择
                           → 用户手动选择后确认
```

---

## 4. 百度OCR接入设计

### 4.1 混合调用策略

采用百度OCR**混合调用方案**，根据文档内容特征选择最优API组合：

| 场景 | 百度OCR API | 说明 |
|------|-------------|------|
| 机打文字（含扭曲/模糊） | 通用文字识别（高精度版） | `accurate_basic` / `accurate`，支持方向检测、去畸变 |
| 手写文字 | 手写文字识别 | `handwriting`，专门针对手写体优化 |
| 表格/结构化文档 | 文档结构化识别 | `doc_analysis`，自动识别表格、Key-Value |
| 混合内容 | 多API组合 | 先用文档结构化做整体分析，再对手写区域单独调手写识别 |

### 4.2 API调用流程

```
                    ┌──────────────┐
                    │ 输入文件/图片  │
                    └──────┬───────┘
                           │
                           ▼
                   ┌───────────────┐
                   │ 预判文档特征    │
                   │ (是否含表格?   │
                   │  是否手写?     │
                   │  是否扭曲?)    │
                   └───┬───┬───┬───┘
                       │   │   │
          ┌────────────┘   │   └────────────┐
          ▼                ▼                ▼
  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
  │ 文档结构化    │ │ 通用文字识别  │ │ 手写文字识别  │
  │ doc_analysis │ │ accurate     │ │ handwriting  │
  │              │ │              │ │              │
  │ · 表格区域   │ │ · 机打内容   │ │ · 手写区域   │
  │ · KV对提取   │ │ · 扭曲纠正   │ │ · 手写体     │
  │ · 版面分析   │ │ · 模糊增强   │ │              │
  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
         │                │                │
         └────────┬───────┘────────┬───────┘
                  │                │
                  ▼                ▼
         ┌──────────────────────────────┐
         │ 识别结果合并与去重             │
         │ · 合并多API返回的文本和置信度   │
         │ · 坐标去重，取最高置信度        │
         │ · 统一输出格式                 │
         └──────────────────────────────┘
```

### 4.3 百度OCR关键参数配置

```java
// 通用文字识别(高精度) - 处理机打+扭曲/模糊
GeneralParams accurateParams = new GeneralParams()
    .setDetectDirection(true)       // 方向检测
    .setParagraph(true)             // 段落输出
    .setLanguageType("CHN_ENG")     // 中英文混合
    .setProbability(true);          // 返回每行置信度

// 手写文字识别
HandwritingParams handwritingParams = new HandwritingParams()
    .setRecognizeGranularity("big") // 识别粒度
    .setWordsType("handprint_mix"); // 手写+印刷体混排

// 文档结构化识别
DocAnalysisParams docParams = new DocAnalysisParams()
    .setResultType("big")           // 大粒度结果
    .setDetectDirection(true)
    .setLanguageType("CHN_ENG");    // 中英文
```

### 4.4 百度OCR QPS管理

百度OCR有QPS限制，需要在系统层面管理：

```
┌─────────────────────────────────────────────┐
│  百度OCR QPS管理策略                          │
│                                             │
│  1. 令牌桶限流                               │
│     · Redis实现分布式令牌桶                    │
│     · 按API类型独立限流                       │
│     · 超出QPS的请求排队等待                    │
│                                             │
│  2. Access Token缓存                        │
│     · Redis缓存Token，过期前自动刷新          │
│     · 避免频繁请求Token接口                   │
│                                             │
│  3. 并发控制                                 │
│     · 消息队列控制OCR调用并发数               │
│     · 根据购买的QPS配额动态调整               │
│                                             │
│  4. 失败重试                                 │
│     · QPS超限时自动重试(指数退避)              │
│     · 接口异常时切换备用API                   │
└─────────────────────────────────────────────┘
```

### 4.5 费用预估

| API | 单价(元/次) | 月均调用量(估) | 月费用(估) |
|-----|------------|--------------|-----------|
| 通用文字识别(高精度) | 0.0035 | 50,000 | 175 |
| 手写文字识别 | 0.005 | 10,000 | 50 |
| 文档结构化识别 | 0.01 | 30,000 | 300 |
| **合计** | | | **约525元/月** |

> 实际费用取决于调用量，百度OCR有阶梯定价，量大价更低。建议购买资源包降低成本。

---

## 5. 核心模块详细设计

### 5.1 对象与字段配置模块

#### 5.1.1 数据模型

```
┌─────────────────────────────────────────────────────┐
│                  ocr_document_type                   │
│  (单据类型定义，如：采购进货单、进项发票、询价清单)      │
├─────────────────────────────────────────────────────┤
│  id              BIGINT         主键                 │
│  type_code       VARCHAR(50)    类型编码              │
│  type_name       VARCHAR(100)   类型名称              │
│  description     VARCHAR(500)   描述                 │
│  default_threshold DECIMAL(5,2) 系统默认阈值(95.00)   │
│  status          TINYINT        启用状态              │
│  created_at      DATETIME       创建时间              │
│  updated_at      DATETIME       更新时间              │
└─────────────────────────────────────────────────────┘
                    │ 1:N                        │ 1:N
                    ▼                            ▼
┌────────────────────────────────┐  ┌────────────────────────────────┐
│      ocr_field_definition      │  │    ocr_tenant_threshold        │
│  (字段定义，分表头和表体)        │  │  (租户自定义阈值)               │
├────────────────────────────────┤  ├────────────────────────────────┤
│  id            BIGINT   主键    │  │  id            BIGINT   主键   │
│  doc_type_id   BIGINT   类型ID  │  │  tenant_id     VARCHAR  租户ID │
│  field_code    VARCHAR  编码    │  │  doc_type_id   BIGINT   类型ID │
│  field_name    VARCHAR  名称    │  │  threshold     DECIMAL  阈值   │
│  field_type    VARCHAR  类型    │  │  created_at    DATETIME        │
│  │  (STRING/NUMBER/DATE/       │  │  updated_at    DATETIME        │
│  │   AMOUNT/ENUM)              │  └────────────────────────────────┘
│  position      VARCHAR  位置   │
│  │  (HEADER / BODY)            │
│  is_required   TINYINT  必填   │
│  sort_order    INT      排序   │
│  validation_rule VARCHAR 校验  │
│  default_value VARCHAR  默认值 │
│  status        TINYINT  状态   │
└────────────────────────────────┘
                    │ 1:N
                    ▼
┌────────────────────────────────────────────────────┐
│              ocr_field_alias                        │
│  (字段别名映射——处理不同供应商字段名差异)              │
├────────────────────────────────────────────────────┤
│  id              BIGINT         主键                │
│  field_id        BIGINT         所属字段定义          │
│  supplier_id     BIGINT         供应商ID(NULL=通用)   │
│  alias_name      VARCHAR(100)   别名                │
│  alias_lang      VARCHAR(10)    别名语言(zh/en)      │
│  priority        INT            优先级               │
└────────────────────────────────────────────────────┘
```

#### 5.1.2 阈值查询优先级

```
获取某租户对某对象的识别阈值:
  1. 查 ocr_tenant_threshold (租户+对象) → 有则使用
  2. 查 ocr_document_type.default_threshold → 有则使用
  3. 使用系统全局默认值 95%
```

#### 5.1.3 字段别名匹配机制

不同供应商的单据字段名可能不同，且需要支持中英文，例如：
- "供应商名称" / "供货商" / "Vendor" / "Supplier" / "供方" → 系统字段 `supplier_name`
- "金额" / "合计金额" / "Total Amount" / "Price" → 系统字段 `total_amount`

匹配策略（优先级从高到低）：
1. **精确匹配**: OCR识别的字段名与别名完全一致（区分中/英文别名）
2. **模糊匹配**: 使用编辑距离算法（Levenshtein Distance），相似度>80%
3. **语义匹配**: 使用同义词库进行语义相似度匹配
4. **位置匹配**: 根据字段在文档中的相对位置推断（表格列位置）
5. **历史学习**: 同一供应商历次识别成功的映射关系缓存

### 5.2 供应商数据管理

#### 5.2.1 供应商主数据表（由外部系统同步）

```
┌─────────────────────────────────────────────────────┐
│                    ocr_supplier                      │
│  (供应商主数据——由外部系统同步写入)                     │
├─────────────────────────────────────────────────────┤
│  id              BIGINT         主键                 │
│  tenant_id       VARCHAR(50)    租户ID               │
│  ext_supplier_id VARCHAR(100)   外部系统供应商ID       │
│  supplier_code   VARCHAR(50)    供应商编码             │
│  supplier_name   VARCHAR(200)   供应商名称             │
│  supplier_alias  VARCHAR(500)   供应商别名(JSON数组)   │
│  contact_person  VARCHAR(100)   联系人                │
│  phone           VARCHAR(50)    电话                  │
│  status          TINYINT        启用状态              │
│  synced_at       DATETIME       最近同步时间           │
│  created_at      DATETIME       创建时间              │
│  updated_at      DATETIME       更新时间              │
└─────────────────────────────────────────────────────┘

supplier_alias JSON 示例:
["深圳XX科技", "XX科技有限公司", "SZ XX Tech"]
```

#### 5.2.2 供应商同步接口

外部系统通过以下方式将供应商数据同步到OCR模块：

```
POST /api/ocr/supplier/sync
Content-Type: application/json

请求体:
{
  "tenantId": "T001",
  "suppliers": [
    {
      "extSupplierId": "SUP001",
      "supplierCode": "GYS001",
      "supplierName": "深圳市XX科技有限公司",
      "supplierAlias": ["深圳XX科技", "XX科技"],
      "contactPerson": "张三",
      "phone": "13800138000"
    }
  ]
}

响应:
{
  "code": 200,
  "data": { "syncCount": 1, "insertCount": 1, "updateCount": 0 }
}
```

#### 5.2.3 供应商匹配算法

```java
// 供应商匹配优先级
public SupplierMatchResult matchSupplier(String ocrSupplierText, String tenantId) {
    // 1. 精确匹配: supplier_name 完全一致
    // 2. 精确匹配: supplier_alias 中任一别名完全一致
    // 3. 模糊匹配: supplier_name 相似度>85%（去空格/标点后比较）
    // 4. 模糊匹配: supplier_alias 相似度>85%
    // 5. 编码匹配: 如果OCR文本中包含供应商编码
    // 6. 未匹配: 返回 NOT_MATCHED，标记为需人工审核
}
```

### 5.3 供应商模板管理

```
┌─────────────────────────────────────────────────────┐
│              ocr_supplier_template                   │
│  (供应商模板——学习并存储每个供应商的单据特征)            │
├─────────────────────────────────────────────────────┤
│  id              BIGINT         主键                 │
│  tenant_id       VARCHAR(50)    租户ID               │
│  supplier_id     BIGINT         供应商ID              │
│  doc_type_id     BIGINT         单据类型              │
│  template_name   VARCHAR(100)   模板名称              │
│  layout_feature  JSON           布局特征(字段位置等)    │
│  field_mapping   JSON           字段映射关系           │
│  sample_file_url VARCHAR(500)   样本文件地址           │
│  match_count     INT            累计匹配次数           │
│  status          TINYINT        启用状态              │
│  created_at      DATETIME       创建时间              │
│  updated_at      DATETIME       更新时间              │
└─────────────────────────────────────────────────────┘
```

`field_mapping` JSON 示例：
```json
{
  "header": {
    "供货商": "supplier_name",
    "订单号码": "order_no",
    "Date": "order_date",
    "Total": "total_amount"
  },
  "body": {
    "品名": "product_name",
    "Specification": "specification",
    "数量": "quantity",
    "单价": "unit_price",
    "金额": "amount"
  }
}
```

### 5.4 结构化数据提取

#### 5.4.1 处理流程

```
百度OCR原始识别结果（文本+坐标+置信度）
        │
        ▼
┌──────────────────┐
│  版面分析         │ ← 区分表头区域 / 表体区域 / 无关区域
│  (doc_analysis   │    百度文档结构化自动完成
│   自动完成)       │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  表格结构识别      │ ← 百度OCR返回的table结构直接使用
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Key-Value提取    │ ← 表头区域：提取"标签:值"对
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  字段映射匹配     │ ← 将识别的标签映射到系统定义字段
│                  │    （使用别名库+模糊匹配，支持中英文）
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  供应商匹配       │ ← 识别出的供应商与外部系统数据比对
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  数据类型转换     │ ← 日期格式化、金额解析、数值提取
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  校验与置信度计算  │ ← 正则校验、逻辑校验、交叉验证
└────────┬─────────┘
         │
         ▼
  结构化识别结果JSON
```

#### 5.4.2 识别结果数据结构

```json
{
  "taskId": "T202601010001",
  "fileId": "F001",
  "fileName": "进货单_供应商A_20260101.pdf",
  "docTypeCode": "PURCHASE_ORDER",
  "docTypeName": "采购进货单",
  "supplierMatch": {
    "ocrSupplierText": "深圳市XX科技有限公司",
    "matched": true,
    "supplierId": 1001,
    "supplierName": "深圳市XX科技有限公司",
    "matchType": "EXACT",
    "matchConfidence": 100
  },
  "overallConfidence": 92.5,
  "autoApproved": false,
  "approveThreshold": 95.0,
  "header": [
    {
      "fieldCode": "order_no",
      "fieldName": "订单编号",
      "value": "PO20260101001",
      "confidence": 99.2,
      "status": "HIGH",
      "ocrRawText": "PO20260101001",
      "boundingBox": [120, 50, 350, 80]
    },
    {
      "fieldCode": "supplier_name",
      "fieldName": "供应商名称",
      "value": "深圳市XX科技有限公司",
      "confidence": 75.3,
      "status": "LOW",
      "ocrRawText": "深圳市X X科技有限公 司",
      "boundingBox": [120, 90, 400, 120]
    }
  ],
  "body": [
    {
      "rowIndex": 0,
      "cells": [
        {
          "fieldCode": "product_name",
          "fieldName": "商品名称",
          "value": "电子元器件A",
          "confidence": 98.0,
          "status": "HIGH"
        },
        {
          "fieldCode": "quantity",
          "fieldName": "数量",
          "value": "1000",
          "confidence": 95.5,
          "status": "HIGH"
        },
        {
          "fieldCode": "unit_price",
          "fieldName": "单价",
          "value": "12.50",
          "confidence": 88.0,
          "status": "MEDIUM"
        }
      ]
    }
  ],
  "ocrRawJson": { ... }
}
```

---

## 6. 数据库设计

### 6.1 完整表清单

| 序号 | 表名 | 说明 | 数据量预估 |
|------|------|------|-----------|
| 1 | ocr_document_type | 单据类型定义 | 小 |
| 2 | ocr_field_definition | 字段定义 | 小 |
| 3 | ocr_field_alias | 字段别名 | 中 |
| 4 | ocr_tenant_threshold | 租户阈值配置 | 小 |
| 5 | ocr_supplier | 供应商主数据(外部同步) | 中 |
| 6 | ocr_supplier_template | 供应商模板 | 中 |
| 7 | ocr_recognition_task | 识别任务 | 大 |
| 8 | ocr_recognition_file | 识别文件 | 大 |
| 9 | ocr_recognition_detail | 识别结果明细 | 大 |
| 10 | ocr_callback_log | 回调日志 | 大 |
| 11 | ocr_tenant_storage | 租户存储空间 | 小 |
| 12 | ocr_storage_order | 存储购买记录 | 中 |

### 6.2 核心表结构

#### 识别任务表

```
┌──────────────────────────────────────────────────────┐
│                  ocr_recognition_task                  │
│  (识别任务——一次批量上传为一个任务)                       │
├──────────────────────────────────────────────────────┤
│  id              BIGINT         主键                   │
│  tenant_id       VARCHAR(50)    租户ID                 │
│  task_no         VARCHAR(50)    任务编号                │
│  doc_type_id     BIGINT         单据类型ID              │
│  total_files     INT            文件总数                │
│  success_count   INT            自动通过数              │
│  review_count    INT            待审核数                │
│  failed_count    INT            识别失败数              │
│  confirmed_count INT            审核完成数              │
│  status          VARCHAR(20)    任务状态                │
│  │                              UPLOADING / PROCESSING │
│  │                              / PARTIAL_DONE / DONE  │
│  │                              / ALL_CALLBACK         │
│  callback_url    VARCHAR(500)   回调地址                │
│  created_by      VARCHAR(50)    创建人                  │
│  created_at      DATETIME       创建时间                │
│  updated_at      DATETIME       更新时间                │
│  expire_at       DATETIME       数据过期时间(留存策略)    │
└──────────────────────────────────────────────────────┘
```

#### 识别文件表

```
┌──────────────────────────────────────────────────────┐
│                  ocr_recognition_file                  │
│  (识别文件——每个上传的文件一条记录)                       │
├──────────────────────────────────────────────────────┤
│  id              BIGINT         主键                   │
│  tenant_id       VARCHAR(50)    租户ID                 │
│  task_id         BIGINT         所属任务ID              │
│  file_name       VARCHAR(200)   原始文件名              │
│  file_url        VARCHAR(500)   文件存储地址             │
│  file_type       VARCHAR(20)    文件类型(PDF/JPG/PNG)   │
│  file_size       BIGINT         文件大小(字节)           │
│  page_count      INT            页数(PDF)               │
│  status          VARCHAR(20)    状态                    │
│  │                              PENDING / PREPROCESSING│
│  │                              / RECOGNIZING / SUCCESS │
│  │                              / NEED_REVIEW / FAILED  │
│  │                              / CONFIRMED             │
│  overall_conf    DECIMAL(5,2)   整体置信度               │
│  threshold_used  DECIMAL(5,2)   使用的阈值               │
│  supplier_id     BIGINT         匹配到的供应商ID         │
│  supplier_matched TINYINT       供应商是否匹配成功        │
│  result_json     JSON           识别结果JSON             │
│  review_json     JSON           审核修改后JSON           │
│  ocr_raw_json    JSON           百度OCR原始返回JSON       │
│  error_message   VARCHAR(500)   错误信息                 │
│  ocr_api_type    VARCHAR(50)    使用的百度OCR API类型     │
│  process_time    INT            处理耗时(毫秒)           │
│  baidu_cost      DECIMAL(10,4)  百度OCR调用费用           │
│  reviewed_by     VARCHAR(50)    审核人                   │
│  reviewed_at     DATETIME       审核时间                 │
│  callback_status VARCHAR(20)    回调状态                 │
│  │                              PENDING / SUCCESS       │
│  │                              / FAILED / RETRYING     │
│  callback_time   DATETIME       回调时间                 │
│  created_at      DATETIME       创建时间                 │
│  updated_at      DATETIME       更新时间                 │
│  expire_at       DATETIME       数据过期时间              │
└──────────────────────────────────────────────────────┘
```

#### 识别结果明细表

```
┌──────────────────────────────────────────────────────┐
│              ocr_recognition_detail                    │
│  (识别结果字段明细)                                      │
├──────────────────────────────────────────────────────┤
│  id              BIGINT         主键                   │
│  file_id         BIGINT         所属文件ID              │
│  field_def_id    BIGINT         字段定义ID              │
│  position        VARCHAR(10)    HEADER/BODY            │
│  row_index       INT            行索引(表体用)           │
│  ocr_raw_text    VARCHAR(500)   OCR原始识别文本          │
│  parsed_value    VARCHAR(500)   解析后的值               │
│  confidence      DECIMAL(5,2)   置信度                  │
│  status          VARCHAR(10)    HIGH/MEDIUM/LOW         │
│  is_modified     TINYINT        是否被人工修改           │
│  modified_value  VARCHAR(500)   修改后的值               │
│  bounding_box    VARCHAR(100)   文字区域坐标             │
│  created_at      DATETIME       创建时间                │
│  updated_at      DATETIME       更新时间                │
└──────────────────────────────────────────────────────┘
```

#### 回调日志表

```
┌──────────────────────────────────────────────────────┐
│                  ocr_callback_log                      │
│  (HTTP回调日志)                                        │
├──────────────────────────────────────────────────────┤
│  id              BIGINT         主键                   │
│  tenant_id       VARCHAR(50)    租户ID                 │
│  file_id         BIGINT         文件ID                 │
│  callback_url    VARCHAR(500)   回调地址                │
│  request_body    TEXT           请求体                  │
│  response_body   TEXT           响应体                  │
│  http_status     INT            HTTP状态码              │
│  status          VARCHAR(20)    SUCCESS/FAILED          │
│  retry_count     INT            已重试次数              │
│  error_message   VARCHAR(500)   错误信息                │
│  created_at      DATETIME       创建时间                │
└──────────────────────────────────────────────────────┘
```

#### 租户存储空间表

```
┌──────────────────────────────────────────────────────┐
│                  ocr_tenant_storage                    │
│  (租户存储空间管理)                                      │
├──────────────────────────────────────────────────────┤
│  id              BIGINT         主键                   │
│  tenant_id       VARCHAR(50)    租户ID                 │
│  total_space_mb  BIGINT         总空间(MB)              │
│  used_space_mb   BIGINT         已用空间(MB)            │
│  default_retain_days INT        默认留存天数(365)        │
│  is_unlimited    TINYINT        是否无限期              │
│  created_at      DATETIME       创建时间                │
│  updated_at      DATETIME       更新时间                │
└──────────────────────────────────────────────────────┘
```

#### 存储购买记录表

```
┌──────────────────────────────────────────────────────┐
│                  ocr_storage_order                     │
│  (存储空间购买/充值记录)                                 │
├──────────────────────────────────────────────────────┤
│  id              BIGINT         主键                   │
│  tenant_id       VARCHAR(50)    租户ID                 │
│  order_no        VARCHAR(50)    订单号                  │
│  space_mb        BIGINT         购买空间(MB)            │
│  retain_days     INT            留存天数(0=无限期)       │
│  amount          DECIMAL(10,2)  金额                    │
│  status          VARCHAR(20)    PAID/REFUNDED           │
│  paid_at         DATETIME       支付时间                │
│  created_at      DATETIME       创建时间                │
└──────────────────────────────────────────────────────┘
```

### 6.3 完整ER关系图

```
                                    ocr_tenant_storage
                                    ocr_storage_order
                                           │
                                      tenant_id
                                           │
ocr_document_type  1───N  ocr_field_definition  1───N  ocr_field_alias
       │                          │
       │1                         │
       │                          │
       N                          │
ocr_tenant_threshold              │
(tenant_id + doc_type_id)         │
       │                          │
       │                          │
ocr_supplier_template             │
(tenant_id + supplier_id)         │
       │                          │
       │                          │
ocr_supplier                      │
(tenant_id)                       │
                                  │
ocr_recognition_task  1───N  ocr_recognition_file  1───N  ocr_recognition_detail
(tenant_id)                  (tenant_id)                      │
       │                          │                           N
       │                          │                           │
       │                     1───N│                           1
       │              ocr_callback_log               ocr_field_definition
       │              (tenant_id)
```

---

## 7. API接口设计

### 7.1 文件上传与识别

```
POST /api/ocr/recognize
Content-Type: multipart/form-data
Header: Authorization: Bearer {SSO Token}

参数：
  files[]          File[]       文件列表（支持多文件，最多50个）
  docTypeCode      String       单据类型编码（必填）
  callbackUrl      String       回调地址（必填）

注意: 不需要传供应商ID，供应商由OCR识别结果自动匹配

响应：
{
  "code": 200,
  "data": {
    "taskId": "T202601010001",
    "taskNo": "OCR20260101001",
    "totalFiles": 5,
    "status": "PROCESSING",
    "estimatedTime": 30
  }
}
```

### 7.2 任务列表查询

```
GET /api/ocr/tasks?page=1&size=20&status=&docTypeCode=&startDate=&endDate=
Header: Authorization: Bearer {SSO Token}

响应：
{
  "code": 200,
  "data": {
    "total": 128,
    "pages": 7,
    "list": [
      {
        "taskId": "T202601010001",
        "taskNo": "OCR20260101001",
        "docTypeName": "采购进货单",
        "totalFiles": 5,
        "successCount": 3,
        "reviewCount": 1,
        "confirmedCount": 0,
        "failedCount": 1,
        "status": "PARTIAL_DONE",
        "createdBy": "张三",
        "createdAt": "2026-01-01 10:30:00"
      }
    ]
  }
}
```

### 7.3 独立文件查询（跨任务）

不依赖任务维度，直接查询所有识别文件记录，支持按供应商筛选，查看某供应商的所有OCR识别历史。

```
GET /api/ocr/files?page=1&size=20&supplierName=&supplierMatched=&status=
    &docTypeCode=&fileName=&startDate=&endDate=
Header: Authorization: Bearer {SSO Token}

参数说明：
  supplierName     String    供应商名称（模糊查询）
  supplierMatched  Boolean   供应商是否匹配成功（可选）
  status           String    文件状态（可选）
  docTypeCode      String    单据类型编码（可选）
  fileName         String    文件名（模糊查询，可选）
  startDate        String    开始时间（可选）
  endDate          String    结束时间（可选）

响应：
{
  "code": 200,
  "data": {
    "total": 256,
    "pages": 13,
    "list": [
      {
        "fileId": "F001",
        "taskNo": "OCR20260101001",
        "fileName": "发票_001.pdf",
        "docTypeName": "采购进货单",
        "status": "SUCCESS",
        "overallConfidence": 98.5,
        "supplierName": "深圳市XX科技有限公司",
        "supplierMatched": true,
        "headerSummary": {
          "order_no": "PO20260101001",
          "order_date": "2026-01-01",
          "total_amount": "125000.00"
        },
        "bodyRowCount": 5,
        "callbackStatus": "SUCCESS",
        "processTime": 2300,
        "createdAt": "2026-01-01 10:30:05"
      }
    ]
  }
}
```

### 7.4 任务详情——文件列表查询

```
GET /api/ocr/task/{taskId}/files?page=1&size=20&status=
Header: Authorization: Bearer {SSO Token}

响应：
{
  "code": 200,
  "data": {
    "taskId": "T202601010001",
    "taskNo": "OCR20260101001",
    "docTypeName": "采购进货单",
    "status": "PARTIAL_DONE",
    "totalFiles": 5,
    "files": [
      {
        "fileId": "F001",
        "fileName": "发票_001.pdf",
        "status": "SUCCESS",
        "overallConfidence": 98.5,
        "supplierName": "深圳市XX科技有限公司",
        "supplierMatched": true,
        "headerSummary": {
          "order_no": "PO20260101001",
          "order_date": "2026-01-01",
          "total_amount": "125000.00"
        },
        "bodyRowCount": 5,
        "callbackStatus": "SUCCESS",
        "processTime": 2300,
        "createdAt": "2026-01-01 10:30:05"
      },
      {
        "fileId": "F002",
        "fileName": "发票_002.jpg",
        "status": "NEED_REVIEW",
        "overallConfidence": 82.3,
        "supplierName": "未匹配",
        "supplierMatched": false,
        "headerSummary": {
          "order_no": "PO20260101002",
          "order_date": "2026-01-02",
          "total_amount": "88000.00"
        },
        "bodyRowCount": 3,
        "callbackStatus": "PENDING",
        "processTime": 3100,
        "createdAt": "2026-01-01 10:30:08"
      }
    ]
  }
}
```

### 7.5 文件识别结果详情（含原始JSON）

```
GET /api/ocr/file/{fileId}
Header: Authorization: Bearer {SSO Token}

响应：
{
  "code": 200,
  "data": {
    "fileId": "F001",
    "fileName": "发票_001.pdf",
    "fileUrl": "https://minio.../发票_001.pdf",
    "docTypeName": "采购进货单",
    "status": "SUCCESS",
    "overallConfidence": 98.5,
    "supplierMatch": {
      "ocrSupplierText": "深圳市XX科技有限公司",
      "matched": true,
      "supplierId": 1001,
      "supplierName": "深圳市XX科技有限公司",
      "matchType": "EXACT"
    },
    "header": [ ... ],
    "body": [ ... ],
    "callbackStatus": "SUCCESS",
    "callbackTime": "2026-01-01 10:30:10"
  }
}
```

### 7.6 查看OCR原始JSON

```
GET /api/ocr/file/{fileId}/raw-json
Header: Authorization: Bearer {SSO Token}

响应：
{
  "code": 200,
  "data": {
    "ocrEngine": "baidu_doc_analysis",
    "rawResponse": {
      "log_id": 123456789,
      "results_num": 10,
      "results": [ ... ]
    }
  }
}
```

### 7.7 获取待审核文件详情

```
GET /api/ocr/file/{fileId}/review
Header: Authorization: Bearer {SSO Token}

响应：
{
  "code": 200,
  "data": {
    "fileId": "F002",
    "fileName": "发票_002.jpg",
    "fileUrl": "https://...",
    "docTypeName": "采购进货单",
    "overallConfidence": 82.3,
    "thresholdUsed": 95.0,
    "supplierMatch": {
      "ocrSupplierText": "深圳市XX科技有限公 司",
      "matched": false,
      "candidates": [
        { "supplierId": 1001, "name": "深圳市XX科技有限公司", "similarity": 92 },
        { "supplierId": 1002, "name": "深圳市XY科技有限公司", "similarity": 78 }
      ]
    },
    "header": [
      {
        "fieldCode": "supplier_name",
        "fieldName": "供应商名称",
        "value": "深圳市XX科技有限公 司",
        "confidence": 75.3,
        "status": "LOW",
        "ocrRawText": "深圳市XX科技有限公 司",
        "boundingBox": [120, 90, 400, 120]
      }
    ],
    "body": [ ... ]
  }
}
```

### 7.8 提交审核修改

```
POST /api/ocr/file/{fileId}/confirm
Header: Authorization: Bearer {SSO Token}

请求体：
{
  "supplierId": 1001,
  "header": [
    {
      "fieldCode": "supplier_name",
      "value": "深圳市XX科技有限公司"
    }
  ],
  "body": [
    {
      "rowIndex": 0,
      "cells": [
        {
          "fieldCode": "unit_price",
          "value": "12.80"
        }
      ]
    }
  ]
}

响应：
{
  "code": 200,
  "message": "审核确认成功，数据已推送至外部系统"
}
```

### 7.9 对象类型管理

```
POST   /api/ocr/config/doc-type                  创建单据类型
PUT    /api/ocr/config/doc-type/{id}              更新单据类型
GET    /api/ocr/config/doc-types                  查询单据类型列表
DELETE /api/ocr/config/doc-type/{id}              删除单据类型

POST   /api/ocr/config/field                     创建字段定义
PUT    /api/ocr/config/field/{id}                 更新字段定义
GET    /api/ocr/config/doc-type/{id}/fields       查询类型下的字段
DELETE /api/ocr/config/field/{id}                 删除字段定义

POST   /api/ocr/config/field-alias               创建字段别名
GET    /api/ocr/config/field/{id}/aliases         查询字段别名
DELETE /api/ocr/config/field-alias/{id}           删除字段别名
```

### 7.10 租户阈值管理

```
GET    /api/ocr/config/threshold                          查询当前租户所有阈值
PUT    /api/ocr/config/threshold/{docTypeId}               设置阈值
DELETE /api/ocr/config/threshold/{docTypeId}               恢复默认阈值

请求体（PUT）:
{
  "threshold": 98.00
}
```

### 7.11 租户存储空间管理

```
GET    /api/ocr/storage/info                     查看存储空间使用情况
POST   /api/ocr/storage/purchase                 购买存储空间

购买请求体:
{
  "spaceMb": 10240,
  "retainDays": 0,
  "paymentMethod": "BALANCE"
}
```

### 7.12 供应商同步接口

```
POST   /api/ocr/supplier/sync                   批量同步供应商
GET    /api/ocr/suppliers?keyword=               查询供应商列表（审核时供选择）
```

### 7.13 回调外部系统接口规范

由于外部系统目前还没有接口规范，由本系统定义回调报文格式：

```
POST {callbackUrl}
Content-Type: application/json

请求体：
{
  "taskId": "T202601010001",
  "taskNo": "OCR20260101001",
  "docTypeCode": "PURCHASE_ORDER",
  "docTypeName": "采购进货单",
  "fileId": "F001",
  "fileName": "进货单_001.pdf",
  "isAutoApproved": true,
  "reviewedBy": null,
  "supplier": {
    "extSupplierId": "SUP001",
    "supplierCode": "GYS001",
    "supplierName": "深圳市XX科技有限公司"
  },
  "data": {
    "header": {
      "order_no": "PO20260101001",
      "supplier_name": "深圳市XX科技有限公司",
      "order_date": "2026-01-01",
      "total_amount": "125000.00"
    },
    "body": [
      {
        "product_name": "电子元器件A",
        "specification": "TYPE-C 3.0",
        "quantity": "1000",
        "unit_price": "12.50",
        "amount": "12500.00"
      }
    ]
  },
  "confidence": {
    "overall": 98.5,
    "threshold": 95.0
  },
  "timestamp": "2026-01-01T10:30:00Z",
  "sign": "sha256签名"
}

外部系统响应要求：
{
  "code": 200,
  "message": "success"
}

回调重试策略：
  · 非200响应或网络超时 → 自动重试
  · 重试间隔: 5s, 30s, 2min, 10min, 1h (共5次)
  · 5次失败后标记为回调失败，可手动重试
```

---

## 8. 前端设计

### 8.1 技术选型

| 组件 | PC端 | H5端 |
|------|------|------|
| 框架 | Vue 3 | Vue 3 |
| UI库 | Element Plus | Vant 4 |
| 构建 | Vite | Vite |
| 状态管理 | Pinia | Pinia |
| HTTP | Axios | Axios |

### 8.2 嵌入策略

作为宿主系统的子模块嵌入，有两种方式（根据宿主系统架构选择）：

| 方式 | 适用场景 | 实现 |
|------|---------|------|
| **路由模块** | 宿主系统也是Vue3 | 将OCR路由注册到宿主路由表，打包时合并 |
| **微前端** | 宿主系统非Vue3或需独立部署 | 使用qiankun/micro-app，OCR作为子应用 |

```
宿主系统菜单结构:
  ├── 首页
  ├── 采购管理
  ├── 库存管理
  ├── OCR智能识别          ← 新增模块
  │   ├── 文件识别           ← 上传+识别
  │   ├── 任务查询           ← 任务列表 → 点击进入任务内文件列表
  │   ├── 识别记录           ← 独立文件查询（跨任务，可按供应商查询）
  │   ├── 人工审核           ← 待审核列表+审核操作
  │   └── 系统配置           ← 对象/字段/阈值配置
  └── 系统管理
```

### 8.3 页面设计

#### 页面1: 文件上传识别页

```
┌──────────────────────────────────────────────────┐
│  OCR智能识别 > 文件识别                            │
├──────────────────────────────────────────────────┤
│                                                  │
│  单据类型: [采购进货单     ▼]     ← 必选          │
│  回调地址: [https://erp.xxx.com/api/callback]     │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │                                          │    │
│  │     拖拽文件到此处或点击上传                │    │
│  │     支持 PDF / JPG / PNG / JPEG           │    │
│  │     单次最多上传 50 个文件                 │    │
│  │                                          │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  已选文件:                                        │
│  ┌──────────────────────────────────────────┐    │
│  │ ✓ 进货单_001.pdf    2.3MB    [删除]       │    │
│  │ ✓ 进货单_002.jpg    1.1MB    [删除]       │    │
│  │ ✓ 进货单_003.png    0.8MB    [删除]       │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  [开始识别]                                       │
│                                                  │
│  注: 供应商无需手动选择，系统将从识别内容中自动匹配 │
│                                                  │
└──────────────────────────────────────────────────┘
```

#### 页面2: 任务查询页

```
┌──────────────────────────────────────────────────────────────────────┐
│  OCR智能识别 > 任务查询                                               │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  查询条件:                                                            │
│  任务编号: [________]  单据类型: [全部 ▼]  状态: [全部 ▼]              │
│  创建时间: [2026-01-01] ~ [2026-01-31]       [查询] [重置]            │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ 任务编号         单据类型    文件数  成功  待审核  失败  状态   │  │
│  ├────────────────────────────────────────────────────────────────┤  │
│  │ OCR20260101001  采购进货单   5     3     1      1    处理中  │  │
│  │ OCR20260101002  进项发票     3     3     0      0    已完成  │  │
│  │ OCR20260101003  询价清单    10     8     2      0    处理中  │  │
│  │ OCR20260102001  采购进货单   1     0     1      0    待审核  │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  共 128 条  < 1 2 3 4 5 ... 7 >                                     │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

#### 页面3: 识别记录页（独立文件查询，跨任务）

不依赖任务维度，直接查询所有识别文件。核心场景：输入供应商名称，查看该供应商所有OCR识别历史。

```
┌──────────────────────────────────────────────────────────────────────┐
│  OCR智能识别 > 识别记录                                                │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  查询条件:                                                            │
│  供应商:  [__________]  单据类型: [全部 ▼]  状态: [全部 ▼]            │
│  文件名:  [__________]  供应商匹配: [全部 ▼]                          │
│  创建时间: [2026-01-01] ~ [2026-01-31]       [查询] [重置]            │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │ 文件名        所属任务         供应商          单据类型           ││
│  │               (匹配状态)       订单号          金额   置信度     ││
│  │                                                      状态 操作  ││
│  ├──────────────────────────────────────────────────────────────────┤│
│  │ 发票_001.pdf  OCR20260101001  XX科技(已匹配)  采购进货单         ││
│  │                               PO20260101001   12.5万  98.5%     ││
│  │                                                      已完成     ││
│  │                                              [查看详情] [JSON]  ││
│  │                                                                  ││
│  │ 发票_008.jpg  OCR20260102003  XX科技(已匹配)  进项发票            ││
│  │                               INV20260101005  6.8万   96.2%     ││
│  │                                                      已完成     ││
│  │                                              [查看详情] [JSON]  ││
│  │                                                                  ││
│  │ 发票_012.png  OCR20260103001  XX科技(已匹配)  询价清单            ││
│  │                               IQ20260103002   --      82.3%     ││
│  │                                                      待审核     ││
│  │                                              [审核] [JSON]      ││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                      │
│  共 256 条  < 1 2 3 4 5 ... 13 >                                    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘

使用场景:
  · 查看某供应商历史所有识别记录 → 输入供应商名称查询
  · 查看所有识别失败的文件 → 状态选择"失败"
  · 查看所有供应商未匹配的文件 → 供应商匹配选择"未匹配"
  · 跨任务搜索某个文件 → 输入文件名查询
```

#### 页面4: 文件列表页（点击任务进入）

```
┌──────────────────────────────────────────────────────────────────────┐
│  OCR智能识别 > 任务查询 > OCR20260101001                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  任务信息:  编号 OCR20260101001 | 类型 采购进货单 | 文件 5个          │
│  进度统计:  成功 3 | 待审核 1 | 失败 1                                │
│                                                                      │
│  查询条件:                                                            │
│  文件名: [________]  状态: [全部 ▼]  供应商: [全部 ▼]  [查询]         │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │ 文件名        供应商          订单号         金额     置信度      ││
│  │               (匹配状态)                              状态 操作  ││
│  ├──────────────────────────────────────────────────────────────────┤│
│  │ 发票_001.pdf  XX科技(已匹配)  PO20260101001  12.5万   98.5%     ││
│  │                                                      已完成     ││
│  │                                                                  ││
│  │ 发票_002.jpg  未匹配          PO20260101002  8.8万    82.3%     ││
│  │                                                      待审核     ││
│  │                                              [审核] [查看JSON]  ││
│  │                                                                  ││
│  │ 发票_003.png  YY电子(已匹配)  PO20260101003  5.2万    97.0%     ││
│  │                                                      已完成     ││
│  │                                                                  ││
│  │ 发票_004.pdf  ZZ贸易(已匹配)  PO20260101004  3.1万    96.8%     ││
│  │                                                      已完成     ││
│  │                                                                  ││
│  │ 发票_005.jpg  --             --              --       --        ││
│  │                                                      识别失败   ││
│  │                                              [重试] [查看错误]  ││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

#### 页面5: OCR原始JSON查看弹窗

```
┌──────────────────────────────────────────────────────────────────┐
│  识别结果JSON - 发票_001.pdf                              [关闭] │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [结构化结果]  [百度OCR原始返回]     ← Tab切换                    │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  {                                                       │    │
│  │    "taskId": "T202601010001",                            │    │
│  │    "fileId": "F001",                                     │    │
│  │    "docTypeCode": "PURCHASE_ORDER",                      │    │
│  │    "supplierMatch": {                                    │    │
│  │      "matched": true,                                    │    │
│  │      "supplierName": "深圳市XX科技有限公司"                │    │
│  │    },                                                    │    │
│  │    "header": [...],                                      │    │
│  │    "body": [...]                                         │    │
│  │  }                                                       │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                  │
│                      [复制JSON]  [下载JSON]                       │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

#### 页面6: 人工审核页（核心页面）

```
┌──────────────────────────────────────────────────────────────────┐
│  人工审核 - 发票_002.jpg                       [上一个] [下一个]   │
│  任务: OCR20260101001 | 待审核: 1/1                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────┐  ┌──────────────────────────────────┐  │
│  │                     │  │  ⚠ 供应商未匹配                    │  │
│  │                     │  │  OCR识别: "深圳市XX科技有限公 司"   │  │
│  │                     │  │  请选择: [深圳市XX科技有限公司 ▼]   │  │
│  │                     │  │  候选: XX科技(92%) | XY科技(78%)  │  │
│  │    原始图片/PDF      │  │                                  │  │
│  │    预览区域          │  │  表头信息                          │  │
│  │                     │  │  ┌──────────────────────────────┐ │  │
│  │  (支持缩放/拖拽)     │  │  │ 订单编号: PO20260101001      │ │  │
│  │                     │  │  │ 订单日期: 2026-01-01 ✓       │ │  │
│  │  点击右侧字段时      │  │  │ 供应商:  [深圳市XX科技有限公  │ │  │
│  │  自动定位到对应区域   │  │  │          司_______________]  │ │  │
│  │  并高亮显示          │  │  │          ↑ 红色边框 75.3%    │ │  │
│  │                     │  │  │ 联系人:  张三 ✓              │ │  │
│  │                     │  │  └──────────────────────────────┘ │  │
│  │                     │  │                                  │  │
│  │                     │  │  表体信息                          │  │
│  │                     │  │  ┌──────┬──────┬─────┬──────────┐│  │
│  │                     │  │  │品名  │数量  │单价  │金额       ││  │
│  │                     │  │  ├──────┼──────┼─────┼──────────┤│  │
│  │                     │  │  │元件A │1000  │12.50│ 12500    ││  │
│  │                     │  │  │元件B │[500] │8.00 │ [40 00]  ││  │
│  │                     │  │  │ ↑绿色  ↑橙色   ↑绿色  ↑红色    ││  │
│  └─────────────────────┘  │  └──────┴──────┴─────┴──────────┘│  │
│                           └──────────────────────────────────┘  │
│                                                                  │
│  颜色说明: 绿色(≥阈值) 橙色(60%~阈值) 红色(<60%)                  │
│  当前阈值: 95% (采购进货单)                                       │
│                                                                  │
│                            [确认提交]    [跳过]                    │
└──────────────────────────────────────────────────────────────────┘

审核交互要点：
  · 左侧显示原始文件预览，右侧显示结构化数据
  · 供应商未匹配时顶部醒目提示，提供下拉选择
  · 低置信度字段红色高亮，可编辑修改
  · 点击右侧字段时，左侧自动定位到对应区域
  · 修改后的字段标记为"已修改"
  · 支持键盘 Tab 键快速在字段间切换
  · 支持 [上一个][下一个] 切换待审核文件
```

#### 页面7: 系统配置页

```
┌──────────────────────────────────────────────────────────────────┐
│  OCR智能识别 > 系统配置                                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [单据类型管理]  [字段配置]  [阈值设置]  [存储管理]    ← Tab页签   │
│                                                                  │
│  ─── 阈值设置 ───────────────────────────────────────────────── │
│                                                                  │
│  系统默认阈值: 95%                                                │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │ 单据类型      系统默认阈值    本租户自定义阈值    操作      │    │
│  ├──────────────────────────────────────────────────────────┤    │
│  │ 采购进货单    95%            [95.0    ]         [保存]   │    │
│  │ 进项发票      95%            [98.0    ]         [保存]   │    │
│  │ 询价清单      95%            [90.0    ]         [保存]   │    │
│  │ 供应商价格表   95%            未设置(用默认)      [设置]   │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ─── 存储管理 ───────────────────────────────────────────────── │
│                                                                  │
│  总空间: 10 GB  |  已用: 3.2 GB  |  剩余: 6.8 GB                 │
│  ████████████████████░░░░░░░░░░░░ 32%                            │
│                                                                  │
│  当前留存策略: 1年                                                │
│  [购买存储空间]  [设为无限期留存]                                   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 8.4 H5端适配

H5端使用 Vant 4 组件库，核心页面适配策略：

| PC页面 | H5适配 | 说明 |
|--------|--------|------|
| 文件上传页 | Vant Uploader | 支持拍照+相册选择+文件选择 |
| 任务查询页 | Vant List + PullRefresh | 下拉刷新+无限滚动 |
| 文件列表页 | Vant Cell + Tag | 列表式展示，状态用Tag |
| 人工审核页 | 上下分栏 | 上方图片预览(pinch缩放)，下方可编辑表单 |
| 配置页 | 简化版 | 仅保留阈值设置、存储查看 |

H5端人工审核页布局：

```
┌────────────────────────────┐
│  审核 - 发票_002.jpg  1/3  │
├────────────────────────────┤
│  ┌──────────────────────┐  │
│  │                      │  │
│  │    原始图片预览        │  │
│  │    (支持双指缩放)      │  │
│  │                      │  │
│  └──────────────────────┘  │
│                            │
│  ⚠ 供应商未匹配            │
│  [请选择供应商         ▼]  │
│                            │
│  ── 表头 ──                │
│  订单编号: PO20260101001 ✓ │
│  订单日期: 2026-01-01 ✓    │
│  供应商:  [深圳市XX科技...] │  ← 红色输入框
│                            │
│  ── 表体 ──                │
│  第1行:                    │
│  品名: 元件A ✓             │
│  数量: [500]    ← 橙色     │
│  单价: 8.00 ✓              │
│  金额: [40 00]  ← 红色     │
│                            │
│  [确认提交]     [跳过]      │
└────────────────────────────┘
```

---

## 9. 高并发架构设计（1万并发）

### 9.1 整体策略

由于使用百度OCR云服务，系统本身不需要GPU资源，高并发重点在于：
- 文件上传接收能力
- 消息队列缓冲+有序消费
- 百度OCR QPS管理
- 数据库/缓存读写能力

```
                           ┌──────────────┐
                           │ K8s Ingress  │
                           │ (Nginx)      │
                           └──────┬───────┘
                                  │
                   ┌──────────────┼──────────────┐
                   ▼              ▼              ▼
            ┌──────────┐  ┌──────────┐  ┌──────────┐
            │ App Pod  │  │ App Pod  │  │ App Pod  │
            │ (含OCR   │  │ (含OCR   │  │ (含OCR   │
            │  模块)   │  │  模块)   │  │  模块)   │
            │ ×10+     │  │ ×10+     │  │ ×10+     │
            └────┬─────┘  └────┬─────┘  └────┬─────┘
                 │             │             │
                 └──────┬──────┘──────┬──────┘
                        │             │
         ┌──────────────┤             ├──────────────┐
         ▼              ▼             ▼              ▼
  ┌────────────┐ ┌────────────┐ ┌──────────┐ ┌──────────┐
  │   MinIO    │ │   MySQL    │ │  Redis   │ │ RabbitMQ │
  │  (文件存储) │ │  (读写分离) │ │  (集群)  │ │  (集群)  │
  └────────────┘ └────────────┘ └──────────┘ └────┬─────┘
                                                   │
                                        ┌──────────┼──────────┐
                                        ▼          ▼          ▼
                                  ┌────────┐ ┌────────┐ ┌────────┐
                                  │OCR消费  │ │OCR消费  │ │OCR消费  │
                                  │Worker  │ │Worker  │ │Worker  │
                                  │Pod×N   │ │Pod×N   │ │Pod×N   │
                                  └────┬───┘ └────┬───┘ └────┬───┘
                                       │          │          │
                                       └────┬─────┘────┬─────┘
                                            │          │
                                            ▼          ▼
                                    ┌────────────────────────┐
                                    │     百度OCR云服务       │
                                    │  (QPS令牌桶限流控制)     │
                                    └────────────────────────┘
```

### 9.2 关键设计点

| 策略 | 实现方式 |
|------|----------|
| **异步处理** | 文件上传后立即返回任务ID，OCR识别通过RabbitMQ异步处理 |
| **消息队列** | RabbitMQ解耦上传与识别，削峰填谷 |
| **弹性伸缩** | K8s HPA根据MQ队列积压自动扩缩OCR Worker Pod |
| **QPS管理** | Redis分布式令牌桶控制百度OCR调用频率 |
| **文件存储** | MinIO分布式存储，支持高并发读写 |
| **缓存** | Redis缓存供应商数据、字段映射、百度Token |
| **数据库** | MySQL读写分离 + 大表按月分表 |
| **限流** | Sentinel限流，保护服务 |
| **WebSocket** | 实时推送识别进度给前端 |
| **百度Token** | Redis缓存Access Token，过期前异步刷新 |

### 9.3 并发能力估算

```
假设条件：
  · 单个文件OCR处理平均耗时: 2秒(百度云服务响应快)
  · 百度OCR购买QPS: 50 QPS (可升级)
  · OCR Worker Pod数: 20个
  · 每Pod消费并发: 5个

理论吞吐量:
  · 受限于百度OCR QPS: 50文件/秒
  · 每小时处理: 180,000 文件

并发支持:
  · 前端接口层（上传+查询）: 10+ App Pod × 1000并发 = 10,000+
  · 文件上传: 快速存入MinIO+入MQ队列，不阻塞
  · OCR处理: MQ缓冲，不受前端并发限制
  · 查询/审核: MySQL读写分离+Redis缓存，支撑高并发读

峰值场景:
  · 10,000用户同时上传 → 文件快速入队
  · OCR Worker按百度QPS配额有序消费
  · 用户通过WebSocket获取实时进度
  · 若百度QPS不足 → 购买更高QPS配额
```

---

## 10. 数据留存与存储计费

### 10.1 留存策略

```
┌─────────────────────────────────────────────────┐
│  数据留存策略                                     │
│                                                  │
│  默认留存: 1年                                    │
│    · 超过1年的识别结果和原始文件自动清理            │
│    · 定时任务每日凌晨扫描过期数据                   │
│    · 先清MinIO文件，再清数据库记录                  │
│                                                  │
│  无限期留存: 租户充值购买                           │
│    · 购买后 is_unlimited=true                     │
│    · 数据永不自动清理                              │
│    · 按实际存储用量计费                            │
│                                                  │
│  存储空间计算:                                     │
│    · 原始文件: 存MinIO，按实际大小计算              │
│    · 识别结果JSON: 存MySQL，按行估算               │
│    · 每月统计一次存储用量                           │
└─────────────────────────────────────────────────┘
```

### 10.2 自动清理流程

```
定时任务 (每日 02:00)
    │
    ▼
查询 expire_at < 当前时间 的文件
    │
    ▼
排除 is_unlimited=true 的租户
    │
    ▼
批量删除 MinIO 文件
    │
    ▼
批量删除 ocr_recognition_detail
    │
    ▼
批量删除 ocr_recognition_file
    │
    ▼
更新 ocr_recognition_task 统计数
    │
    ▼
更新 ocr_tenant_storage.used_space_mb
```

---

## 11. SSO对接设计

### 11.1 认证流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  用户     │     │ 宿主系统  │     │ SSO中心  │
└────┬─────┘     └────┬─────┘     └────┬─────┘
     │  访问OCR模块    │                │
     │────────────────>│                │
     │                 │  验证Token      │
     │                 │───────────────>│
     │                 │  返回用户信息    │
     │                 │<───────────────│
     │                 │                │
     │  ┌──────────────┴──────────┐     │
     │  │ OCR模块从请求Header获取: │     │
     │  │  · SSO Token            │     │
     │  │  · tenant_id (租户ID)   │     │
     │  │  · user_id (用户ID)     │     │
     │  │  · user_name (用户名)   │     │
     │  └──────────────┬──────────┘     │
     │                 │                │
     │  返回OCR页面/数据│                │
     │<────────────────│                │
```

### 11.2 实现方式

```java
// OCR模块不做独立登录，复用宿主系统的SSO认证
// 通过拦截器从请求上下文中获取用户信息

@Component
public class OcrAuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // 从宿主系统的SecurityContext中获取当前用户
        UserContext user = SecurityContextHolder.getContext().getUser();
        OcrUserContext.set(user.getTenantId(), user.getUserId(), user.getUserName());
        return true;
    }
}
```

---

## 12. 技术栈总览

### 后端

| 组件 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.2+ |
| ORM | MyBatis-Plus | 3.5+ |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |
| 消息队列 | RabbitMQ | 3.12+ |
| 文件存储 | MinIO | 最新 |
| **OCR引擎** | **百度OCR (付费API)** | **最新** |
| 认证 | 宿主系统SSO (复用) | - |
| 限流熔断 | Sentinel | 1.8+ |
| 容器化 | Docker + Kubernetes | - |
| 数据库迁移 | Flyway / Liquibase | - |

### 前端

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 | 3.4+ |
| PC端UI | Element Plus | 2.5+ |
| H5端UI | Vant | 4.x |
| 构建工具 | Vite | 5.x |
| 状态管理 | Pinia | 2.x |
| HTTP客户端 | Axios | 1.x |
| 图片预览 | viewerjs | - |
| PDF预览 | pdf.js | - |
| WebSocket | socket.io-client | - |
| JSON展示 | vue-json-pretty | - |

---

## 13. 测试DEMO宿主系统

### 13.1 设计目标

提供一个完整可运行的测试DEMO，作为宿主系统直接启动，内置模拟SSO鉴权，使开发和测试过程无需依赖真实宿主系统。

### 13.2 DEMO功能

```
ocr-demo（测试宿主系统）:
  · 内置模拟SSO登录（用户名/密码登录，返回JWT Token）
  · 内置多租户模拟（预置2个测试租户）
  · 内置供应商测试数据
  · 引入ocr-starter依赖，自动启用OCR模块
  · 提供回调接收接口（模拟外部系统接收回调数据）
  · H2/MySQL双模式（开发用H2内存库，集成测试用MySQL）
  · 前端DEMO页面（登录页 + 宿主系统框架页 + OCR模块嵌入）
```

### 13.3 模拟SSO鉴权

```
登录接口:
  POST /api/auth/login
  { "username": "admin", "password": "123456" }
  →
  { "code": 200, "data": { "token": "eyJhbGc...", "tenantId": "T001", "userId": "U001", "userName": "管理员" } }

预置测试账户:
  ┌──────────┬──────────┬──────────┬──────────┐
  │ 用户名    │ 密码      │ 租户ID   │ 角色      │
  ├──────────┼──────────┼──────────┼──────────┤
  │ admin    │ 123456   │ T001    │ 管理员    │
  │ user1    │ 123456   │ T001    │ 普通用户  │
  │ admin2   │ 123456   │ T002    │ 管理员    │
  └──────────┴──────────┴──────────┴──────────┘

Token验证:
  · 所有 /api/ocr/** 请求需携带 Authorization: Bearer {token}
  · 拦截器解析Token获取 tenantId, userId, userName
  · Token有效期: 24小时
```

### 13.4 模拟回调接收

```
DEMO内置回调接收接口:
  POST /api/demo/callback/receive

  · 接收OCR识别结果回调
  · 打印到日志 + 存入内存列表
  · 提供查询接口查看已接收的回调数据

  GET /api/demo/callback/list
  → 返回所有已接收的回调数据列表
```

### 13.5 DEMO启动方式

```bash
# 方式1: 使用H2内存数据库（零配置启动）
mvn spring-boot:run -pl ocr-demo -Dspring.profiles.active=h2

# 方式2: 使用MySQL（需要先创建数据库）
mvn spring-boot:run -pl ocr-demo -Dspring.profiles.active=mysql

# 方式3: Docker Compose一键启动（含MySQL+Redis+MinIO+RabbitMQ）
docker-compose -f deploy/docker/docker-compose-demo.yml up
```

---

## 14. 项目模块结构

```
ocr-agent/                              # OCR识别模块（嵌入宿主系统）
├── ocr-common/                          # 公共模块
│   └── src/main/java/
│       └── com.ocr.common/
│           ├── dto/                     # 数据传输对象
│           ├── enums/                   # 枚举（文件状态、置信度级别等）
│           ├── exception/               # 异常定义
│           ├── result/                  # 统一响应
│           ├── context/                 # OcrUserContext（租户/用户上下文）
│           └── utils/                   # 工具类
│
├── ocr-api/                             # 对外API定义
│   └── src/main/java/
│       └── com.ocr.api/
│           ├── dto/                     # 回调DTO、同步DTO
│           ├── event/                   # 事件定义（供宿主系统监听）
│           └── service/                 # SPI接口（供宿主系统实现）
│
├── ocr-file/                            # 文件服务模块
│   └── src/main/java/
│       └── com.ocr.file/
│           ├── controller/              # 文件上传接口
│           ├── service/                 # 文件处理逻辑
│           ├── storage/                 # MinIO存储
│           └── converter/               # PDF转图片
│
├── ocr-recognition/                     # OCR识别模块
│   └── src/main/java/
│       └── com.ocr.recognition/
│           ├── consumer/                # MQ消费者（OCR Worker）
│           ├── engine/                  # 百度OCR引擎
│           │   ├── BaiduOcrClient       # 百度OCR HTTP客户端
│           │   ├── BaiduTokenManager    # Token管理（Redis缓存）
│           │   ├── AccurateOcrService   # 通用文字识别(高精度)
│           │   ├── HandwritingService   # 手写文字识别
│           │   ├── DocAnalysisService   # 文档结构化识别
│           │   └── MixedOcrStrategy     # 混合调用策略
│           ├── parser/                  # 结果解析
│           │   ├── LayoutAnalyzer       # 版面分析
│           │   ├── TableParser          # 表格解析
│           │   └── KvExtractor          # Key-Value提取
│           ├── matcher/                 # 字段匹配
│           │   ├── FieldMatcher         # 字段别名匹配
│           │   └── SupplierMatcher      # 供应商匹配
│           ├── confidence/              # 置信度计算
│           └── qps/                     # 百度QPS限流管理
│
├── ocr-business/                        # 业务管理模块
│   └── src/main/java/
│       └── com.ocr.business/
│           ├── controller/
│           │   ├── DocTypeController     # 单据类型管理
│           │   ├── FieldController       # 字段管理
│           │   ├── ThresholdController   # 租户阈值管理
│           │   ├── TaskController        # 任务查询
│           │   ├── FileController        # 文件查询
│           │   ├── ReviewController      # 人工审核
│           │   ├── SupplierController    # 供应商同步/查询
│           │   ├── StorageController     # 存储空间管理
│           │   └── CallbackController    # 回调管理（手动重试等）
│           ├── service/
│           │   ├── TaskQueryService      # 任务查询服务
│           │   ├── FileQueryService      # 文件查询服务
│           │   ├── ReviewService         # 审核服务
│           │   ├── CallbackService       # HTTP回调服务
│           │   ├── SupplierSyncService   # 供应商同步服务
│           │   ├── StorageService        # 存储计费服务
│           │   └── DataCleanupService    # 数据清理服务（定时任务）
│           ├── mapper/
│           └── entity/
│
├── ocr-starter/                         # Spring Boot Starter
│   └── src/main/java/
│       └── com.ocr.starter/
│           ├── OcrAutoConfiguration     # 自动配置类
│           ├── OcrProperties            # 配置属性
│           └── resources/
│               ├── META-INF/
│               │   └── spring.factories # 自动配置注册
│               └── db/migration/        # Flyway迁移脚本
│
├── ocr-demo/                            # 测试DEMO宿主系统
│   └── src/main/java/
│       └── com.ocr.demo/
│           ├── OcrDemoApplication        # 启动类
│           ├── config/
│           │   ├── SecurityConfig        # Spring Security配置
│           │   └── WebMvcConfig          # Web配置
│           ├── auth/
│           │   ├── JwtTokenProvider      # JWT Token生成/解析
│           │   ├── JwtAuthFilter         # JWT认证过滤器
│           │   └── AuthController        # 登录接口
│           ├── mock/
│           │   ├── MockCallbackController # 模拟回调接收
│           │   └── MockDataInitializer    # 测试数据初始化
│           └── resources/
│               ├── application.yml        # 主配置
│               ├── application-h2.yml     # H2内存库配置
│               └── application-mysql.yml  # MySQL配置
│
├── ocr-frontend/                        # 前端模块
│   ├── src/
│   │   ├── views/
│   │   │   ├── upload/                  # 文件上传页
│   │   │   ├── task/                    # 任务查询页
│   │   │   ├── file/                    # 文件列表页（任务内）
│   │   │   ├── record/                  # 识别记录页（独立文件查询，可按供应商）
│   │   │   ├── review/                  # 人工审核页
│   │   │   └── config/                  # 系统配置页（类型/字段/阈值/存储）
│   │   ├── components/
│   │   │   ├── pc/                      # PC端组件 (Element Plus)
│   │   │   ├── h5/                      # H5端组件 (Vant)
│   │   │   ├── JsonViewer.vue           # JSON查看组件
│   │   │   ├── FilePreview.vue          # 文件预览组件
│   │   │   ├── ConfidenceTag.vue        # 置信度标签(颜色)
│   │   │   └── SupplierSelect.vue       # 供应商选择组件
│   │   ├── api/                         # API调用
│   │   ├── stores/                      # Pinia状态
│   │   ├── router/                      # 路由（嵌入宿主路由）
│   │   └── utils/
│   ├── package.json
│   └── vite.config.ts
│
├── deploy/                              # 部署配置
│   ├── k8s/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── hpa.yaml                     # 自动伸缩
│   │   └── configmap.yaml               # 百度OCR配置
│   └── docker/
│       └── Dockerfile
│
├── docs/
│   ├── api/                             # API文档
│   ├── sql/                             # 数据库脚本
│   └── callback-spec.md                 # 回调接口规范（供外部系统对接）
│
└── pom.xml                              # 父POM
```

---

## 15. K8s部署架构

```
┌──────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                      │
│                                                          │
│  ┌──────────────────────────────────────────────────┐    │
│  │  Ingress Controller (Nginx)                       │    │
│  │  · SSL终止                                        │    │
│  │  · /api/ocr/** → OCR Service                     │    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                │
│  ┌──────────────────────┴───────────────────────────┐    │
│  │  Deployment: ocr-app (HPA: 3~20 replicas)        │    │
│  │  · 宿主系统 + OCR模块 一体部署                     │    │
│  │  · 包含: 文件上传、业务管理、查询、审核              │    │
│  │  · resources: 2C4G per pod                        │    │
│  └──────────────────────────────────────────────────┘    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐    │
│  │  Deployment: ocr-worker (HPA: 2~30 replicas)     │    │
│  │  · OCR识别消费者（从MQ消费任务）                    │    │
│  │  · 调用百度OCR API                                │    │
│  │  · 根据MQ队列积压自动扩缩容                        │    │
│  │  · resources: 1C2G per pod                        │    │
│  └──────────────────────────────────────────────────┘    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐    │
│  │  中间件层 (StatefulSet / Operator)                 │    │
│  │  · MySQL 8.0 (主从复制)                            │    │
│  │  · Redis 7.0 (Cluster 3主3从)                     │    │
│  │  · RabbitMQ 3.12 (镜像队列)                        │    │
│  │  · MinIO (分布式4节点)                             │    │
│  └──────────────────────────────────────────────────┘    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐    │
│  │  监控 (DaemonSet / Deployment)                    │    │
│  │  · Prometheus + Grafana                           │    │
│  │  · ELK Stack (日志)                               │    │
│  └──────────────────────────────────────────────────┘    │
│                                                          │
│  HPA 策略:                                               │
│  · ocr-app: CPU>70% 扩容，<30% 缩容                      │
│  · ocr-worker: MQ队列>1000 扩容，<100 缩容                │
└──────────────────────────────────────────────────────────┘
```

---

## 16. 安全设计

| 安全策略 | 实现方式 |
|----------|----------|
| 接口鉴权 | 复用宿主系统SSO Token，OCR模块校验租户权限 |
| 多租户隔离 | 所有数据表带tenant_id，查询时自动过滤 |
| 文件安全 | 文件类型白名单(PDF/JPG/PNG/JPEG)、大小限制(单文件≤20MB)、MinIO隔离存储 |
| 数据传输 | HTTPS/TLS加密 |
| 回调安全 | SHA256签名校验，防篡改 |
| 百度OCR安全 | API Key/Secret Key 加密存储在K8s Secret中 |
| 敏感数据 | 发票号码等敏感信息按租户配置脱敏展示 |
| 操作审计 | 所有人工审核操作记录审计日志（who/when/what） |
| 限流防刷 | Sentinel + 租户级限流 + 接口级限流 |

---

## 17. 监控与告警

| 监控维度 | 工具 | 关键指标 |
|----------|------|----------|
| 服务监控 | Prometheus + Grafana | QPS、响应时间、错误率 |
| 链路追踪 | SkyWalking | 全链路耗时、百度OCR调用耗时 |
| 日志监控 | ELK Stack | 错误日志、识别失败分析 |
| 业务监控 | 自定义Dashboard | 识别成功率、平均置信度、人工审核率、供应商匹配率 |
| 队列监控 | RabbitMQ Management | 队列积压量、消费速度 |
| 百度OCR监控 | 自定义指标 | 调用量、QPS使用率、费用、错误率 |
| 存储监控 | MinIO Dashboard | 存储用量、租户维度用量 |

---

## 18. 开发计划（建议）

| 阶段 | 内容 | 建议工期 |
|------|------|----------|
| **第一阶段** | 基础框架搭建(Maven多模块+Starter) + 数据库表 + SSO对接 | 1.5周 |
| **第二阶段** | 对象/字段配置模块 + 租户阈值管理 + 供应商同步接口 | 1.5周 |
| **第三阶段** | 百度OCR集成(混合调用+QPS管理+Token缓存) | 2周 |
| **第四阶段** | 文件上传 + MQ异步处理 + 结构化提取 + 供应商匹配 + 置信度评估 | 2.5周 |
| **第五阶段** | 前端开发——上传页 + 任务查询页 + 文件列表页 + JSON查看 | 2周 |
| **第六阶段** | 前端开发——人工审核页(PC+H5) + 供应商选择 + 配置页 | 2.5周 |
| **第七阶段** | HTTP回调机制 + 数据留存/清理 + 存储计费 | 1.5周 |
| **第八阶段** | H5端全面适配 + 多语言(英文)支持 | 1.5周 |
| **第九阶段** | 高并发优化 + K8s部署 + 压测 + 联调 | 2周 |
| **总计** | | **约17周** |

---

## 19. 风险与对策

| 风险 | 对策 |
|------|------|
| 百度OCR手写识别率不稳定 | 手写体识别结果自动降低置信度评分，引导人工审核 |
| 百度OCR QPS限制 | 购买更高QPS配额 + MQ队列缓冲削峰 |
| 百度OCR服务不可用 | 接入监控告警 + 自动重试 + 预留备用OCR方案(如腾讯OCR) |
| 不同供应商模板差异大 | 字段别名机制 + 供应商模板学习 + 历史匹配缓存 |
| 供应商名称匹配不准 | 多级匹配策略(精确→模糊→编码) + 未匹配时人工选择 |
| 百度OCR费用失控 | 实时费用监控 + 租户级调用量限制 + 费用告警 |
| 大量文件导致存储快速增长 | 租户存储配额 + 自动过期清理 + 充值扩容 |
| 中英文混排识别困难 | 使用百度OCR CHN_ENG模式 + 字段别名同时配中英文 |
| 嵌入宿主系统兼容性 | Starter自动配置 + 最小化依赖 + 充分测试 |

---

*文档版本: v2.1*
*创建日期: 2026-02-23*
*更新日期: 2026-02-23*
*更新说明: v2.1 新增独立文件查询页面(按供应商跨任务查询) + 测试DEMO宿主系统设计*
*作者: OCR Agent System*
