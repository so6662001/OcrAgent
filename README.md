# OcrAgent — 单据智能识别系统

基于百度OCR的图片/PDF单据智能识别Demo，支持手写体和机打印刷体识别，输出结构化JSON。

## 功能特性

- **多文件批量上传**：支持一次上传多个文件，自动排队处理
- **多格式支持**：JPG / PNG / BMP / TIFF / PDF
- **手写+机打**：自动判别手写体和印刷体，选择最优识别策略
- **字段智能映射**：自动识别"品类/品名/商品名称"等同义字段并归一化
- **可编辑结果**：识别后展示内容供用户修改，确认后输出JSON
- **自动学习**：用户纠错数据自动学习，持续优化识别准确率
- **H5适配**：响应式布局，支持移动端使用

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 21 + Spring Boot 3.2 |
| 前端 | Vue 3 + TypeScript + Vite |
| OCR | 百度AI开放平台 OCR API |

## 项目结构

```
├── ocr-server/          # Java后端
│   ├── src/main/java/com/ocragent/
│   │   ├── controller/  # REST API
│   │   ├── service/     # 业务逻辑
│   │   ├── ocr/         # 百度OCR集成
│   │   └── model/       # 数据模型
│   └── pom.xml
├── ocr-web/             # Vue3前端
│   ├── src/
│   │   ├── views/       # 页面
│   │   ├── components/  # 组件
│   │   ├── api/         # API请求
│   │   └── stores/      # Pinia状态管理
│   └── package.json
└── DESIGN.md            # 系统设计文档
```

## 快速开始

### 前置条件

- Java 21+
- Node.js 18+
- 百度AI开放平台账号（[申请地址](https://ai.baidu.com/tech/ocr)）

### 1. 配置百度OCR密钥

编辑 `ocr-server/src/main/resources/application.yml`：

```yaml
baidu:
  ocr:
    api-key: 你的API_KEY
    secret-key: 你的SECRET_KEY
```

或通过环境变量设置：

```bash
export BAIDU_OCR_API_KEY=你的API_KEY
export BAIDU_OCR_SECRET_KEY=你的SECRET_KEY
```

### 2. 启动后端

```bash
cd ocr-server
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd ocr-web
npm install
npm run dev
```

访问 http://localhost:3000

## API接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/recognize/upload` | 批量上传文件识别 |
| GET | `/api/v1/recognize/task/{taskId}` | 查询单个任务结果 |
| GET | `/api/v1/recognize/tasks` | 查询所有任务 |
| POST | `/api/v1/recognize/confirm` | 确认/纠错并学习 |
| GET | `/api/v1/field-mapping/config` | 获取字段映射配置 |
| POST | `/api/v1/field-mapping/synonym` | 添加同义词映射 |
