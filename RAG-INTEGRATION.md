# RAG 服务集成指南

本文档说明如何配置和使用 RAG（检索增强生成）服务，将知识库内容向量化并提供智能检索能力。

## 架构概述

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Vue Frontend  │────▶│ Spring Boot     │────▶│ Python RAG API  │
│   (Port 5173)   │     │ Backend (8080)  │     │ (Port 8081)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                              │                         │
                              ▼                         ▼
                        ┌──────────┐            ┌──────────────┐
                        │  MySQL   │            │   LanceDB    │
                        │ Database │            │ Vector Store │
                        └──────────┘            └──────────────┘
```

## 功能特性

1. **自动向量化**：知识条目创建/更新时自动同步到向量库
2. **附件内容提取**：支持从 Word、PDF、Excel 等文件中提取文本
3. **混合检索**：结合 BM25、TF-IDF 和 BGE 向量的混合检索
4. **实时同步**：事件驱动架构，数据变更立即触发向量化
5. **MCP 集成**：通过 MiMo Desktop 技能提供智能问答

## 快速开始

### 1. 安装依赖

```powershell
# 进入 RAG 服务目录
cd product-assistant

# 创建 Python 虚拟环境
python -m venv .venv

# 激活虚拟环境
.\.venv\Scripts\Activate.ps1

# 安装依赖
pip install -r requirements.txt
```

### 2. 启动服务

#### 方式一：启动全部服务（推荐）

```powershell
.\Start-FullPlatform.ps1
```

#### 方式二：分别启动

```powershell
# 终端 1：启动 RAG 服务
.\Start-RAGService.ps1

# 终端 2：启动后端
cd backend
.\mvnw.cmd spring-boot:run

# 终端 3：启动前端
cd frontend
npm run dev
```

### 3. 配置

#### 环境变量

在 `backend/src/main/resources/application.properties` 中配置：

```properties
# RAG 服务配置
rag.service.url=${RAG_SERVICE_URL:http://localhost:8081}
rag.service.enabled=${RAG_ENABLED:true}
rag.service.timeout=${RAG_TIMEOUT:30000}
```

或通过环境变量设置：

```powershell
$env:RAG_SERVICE_URL="http://localhost:8081"
$env:RAG_ENABLED="true"
```

## API 接口

### RAG 服务 API (端口 8081)

#### 同步知识条目

```http
POST /api/rag/sync
Content-Type: application/json

{
  "item_id": "123",
  "title": "知识条目标题",
  "content": "Markdown 内容",
  "category": "分类名称",
  "tags": ["标签1", "标签2"],
  "attachments": [
    {
      "filename": "文档.docx",
      "content": "提取的文本内容",
      "content_type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
  ]
}
```

#### 检索知识库

```http
POST /api/rag/search
Content-Type: application/json

{
  "query": "检索关键词",
  "top_k": 8,
  "category": "可选分类过滤",
  "doc_type": "可选文档类型过滤"
}
```

#### 删除条目

```http
DELETE /api/rag/{item_id}
```

#### 查看状态

```http
GET /api/rag/status
```

### 后端 API (端口 8080)

后端会自动调用 RAG 服务，无需手动同步。当知识条目创建、更新或删除时，会自动触发向量同步。

## 技能配置

在 MiMo Desktop 中使用以下技能：

### product-qa（产品知识问答）

```
/product-qa 什么是边缘网关？
```

技能会自动：
1. 调用 `rag_search` 检索相关文档
2. 返回带有出处的答案

### 其他可用技能

- `doc-writer`：按库内事实写文档
- `meeting-ingest`：会议纪要入库
- `sales-assist`：售前客服话术
- `compare-analyze`：多对象对比
- `param-match`：参数对照
- `bid-respond`：招标响应
- `impl-support`：实施支持

## 数据流

1. **入库流程**：
   ```
   用户创建知识条目 → 保存到 MySQL → 发布事件 → 
   异步调用 RAG API → 提取内容 → 切块 → 向量化 → 存储到 LanceDB
   ```

2. **检索流程**：
   ```
   用户提问 → 调用 rag_search → 混合检索（BM25 + TF-IDF + 向量） → 
   返回相关切块 → 生成答案（带出处）
   ```

## 文件结构

```
knowledge-platform/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/.../
│       ├── service/
│       │   ├── RagService.java           # RAG 服务接口
│       │   ├── impl/RagServiceImpl.java  # RAG 服务实现
│       │   └── AttachmentContentExtractor.java  # 附件内容提取
│       ├── event/
│       │   ├── KnowledgeItemEvent.java   # 知识条目事件
│       │   ├── AttachmentEvent.java      # 附件事件
│       │   └── EventPublisher.java       # 事件发布器
│       └── listener/
│           └── VectorSyncListener.java   # 事件监听器
├── product-assistant/          # Python RAG 服务
│   ├── api_server.py           # FastAPI 服务
│   ├── ingest/                 # 入库管道
│   ├── mcp-rag/                # MCP 服务
│   └── data/                   # 向量数据
└── Start-FullPlatform.ps1      # 统一启动脚本
```

## 故障排除

### RAG 服务无法启动

1. 检查 Python 虚拟环境是否正确创建
2. 检查依赖是否安装完整：`pip install -r requirements.txt`
3. 检查端口 8081 是否被占用

### 向量化失败

1. 检查 RAG 服务是否运行：访问 `http://localhost:8081/health`
2. 查看后端日志中的错误信息
3. 检查网络连接

### 检索结果不准确

1. 确保文档已正确入库：查看 `http://localhost:8081/api/rag/status`
2. 尝试不同的查询词
3. 检查文档内容是否完整

## 性能优化

1. **模型缓存**：首次运行会下载 BGE 模型（约 100MB），后续使用缓存
2. **增量更新**：只处理变更的文档，避免全量重建
3. **异步处理**：向量化任务在后台执行，不阻塞用户操作
4. **连接池**：RAG 服务使用连接池复用 HTTP 连接

## 扩展开发

### 添加新的文件格式支持

在 `AttachmentContentExtractor.java` 中添加新的提取方法：

```java
private String extractNewFormat(Path path) throws IOException {
    // 实现提取逻辑
    return extractedText;
}
```

### 更换嵌入模型

在 `product-assistant/ingest/embed.py` 中修改：

```python
DEFAULT_EMBED_MODEL = os.environ.get(
    "RAG_EMBED_MODEL", "BAAI/bge-large-zh-v1.5"  # 使用更大的模型
)
```

注意：更换模型后需要重建索引。

## 监控和日志

- RAG 服务日志：查看运行 RAG 服务的终端输出
- 后端日志：查看 Spring Boot 控制台输出
- 向量库状态：访问 `http://localhost:8081/api/rag/status`

## 常见问题

**Q: 为什么某些文档没有被索引？**
A: 检查文档格式是否支持，以及文档内容是否为空。

**Q: 如何手动触发全量重建？**
A: 调用 API：`POST http://localhost:8081/api/rag/rebuild`

**Q: 向量库占用多少磁盘空间？**
A: 约每 1000 个切块占用 50MB（取决于向量维度）。

**Q: 支持多语言吗？**
A: 当前使用中文优化的 BGE 模型，对中文支持最好，也支持英文。