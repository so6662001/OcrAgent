# OcrAgent — OCR智能识别系统

基于百度OCR混合调用的企业级智能单据识别系统，支持采购进货单、进项发票、询价清单等多种单据的自动识别、结构化提取和人工审核。

## 项目结构

```
ocr-agent/
├── ocr-common/          # 公共模块（DTO、枚举、异常、上下文）
├── ocr-api/             # 对外API定义（回调DTO、供应商同步DTO）
├── ocr-business/        # 业务管理（实体、Mapper、Service、Controller）
├── ocr-file/            # 文件服务（上传、MinIO存储、PDF转换）
├── ocr-recognition/     # OCR识别（百度OCR调用、供应商匹配、置信度评估）
├── ocr-starter/         # Spring Boot Starter + Flyway迁移脚本
├── ocr-demo/            # 测试DEMO宿主系统（JWT鉴权、模拟SSO）
├── ocr-frontend/        # Vue3前端（Element Plus）
├── DESIGN.md            # 架构设计文档 v2.1
└── pom.xml              # Maven父POM
```

## 技术栈

- **后端**: Java 21, Spring Boot 3.2, MyBatis-Plus 3.5, MySQL, Redis, RabbitMQ, MinIO
- **OCR引擎**: 百度OCR付费API（混合调用：通用识别 + 手写识别 + 文档结构化）
- **前端**: Vue 3, Element Plus, Pinia, Vue Router, Axios, Vite
- **部署**: Docker, Kubernetes

## 快速启动（DEMO模式）

### 后端启动（H2内存数据库，零配置）

```bash
mvn clean compile -q
mvn spring-boot:run -pl ocr-demo -Dspring-boot.run.profiles=h2
```

启动后访问 http://localhost:8080

### 前端启动

```bash
cd ocr-frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 测试账号

| 用户名 | 密码 | 租户ID | 角色 |
|--------|------|--------|------|
| admin | 123456 | T001 | 管理员 |
| user1 | 123456 | T001 | 普通用户 |
| admin2 | 123456 | T002 | 管理员 |

## 核心功能

1. **文件识别**: 批量上传PDF/图片，选择单据类型即可自动识别
2. **任务查询**: 查看所有识别任务的进度和状态
3. **识别记录**: 跨任务查询所有文件，支持按供应商筛选
4. **人工审核**: 低置信度字段红色高亮，供应商未匹配时可手动选择
5. **系统配置**: 单据类型/字段定义、租户阈值自定义
6. **HTTP回调**: 识别完成自动回调外部系统

## 配置百度OCR

在 `application.yml` 或环境变量中配置:

```yaml
ocr:
  baidu:
    app-id: your-app-id
    api-key: your-api-key
    secret-key: your-secret-key
```

## API文档

详见 [DESIGN.md](DESIGN.md) 第7章 API接口设计。
