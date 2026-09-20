# product-assistant

基于 MiMo Desktop 的公司产品文档 RAG 工作助手。

## 布局

- `docs-vault/`：原始文档唯一真相源（products / faq / meeting / competitor）
- `data/`：切块、向量、元数据（勿手工改）
- `ingest/`：入库管道
- `mcp-rag/`：检索 MCP 服务
- `.mimocode/skills/`：MiMoCode 技能
- `scripts/`：PowerShell 入口

## 入库

1. 把文档放入 `docs-vault/` 对应子目录。
2. **三种方式更新向量库**（任选）：

| 方式 | 操作 | 适用 |
|------|------|------|
| 对话触发 | 新开对话后说：`调用 rag_reingest 更新知识库` | 偶尔加文档 |
| 手动脚本 | `scripts\ingest.ps1` | 脚本/CI |
| 自动监视 | 保持运行 `scripts\watch.ps1` | 持续丢文件进 vault |

MCP 会在检索前对比 `data/chunks/chunks.jsonl` 的修改时间，**外部入库后下次提问会自动加载新索引**，无需重启对话。

```powershell
D:\XiaomiMiMoProjects\product-assistant\scripts\ingest.ps1
# 自动监视（另开一个终端保持运行）
D:\XiaomiMiMoProjects\product-assistant\scripts\watch.ps1
```

支持：`.md` `.txt` `.docx` `.pdf` `.xlsx` `.xlsm`。增量按文件内容哈希；忽略 `~$` Word/Excel 锁文件。

## 检索 MCP

- 服务入口：`mcp-rag/server.py`
- 注册位置：全局 `mimocode.jsonc` → `mcp.product-rag`
- 改配置后需**新开对话**才会加载。

工具：

| 名称 | 作用 |
|------|------|
| `rag_search` | 混合检索（向量 + BM25），返回文本块与出处 |
| `rag_list_sources` | 列出已入库文档 |
| `rag_get_document` | 读取指定文档原文（节选） |
| `rag_reingest` | 增量/全量入库并刷新索引 |
| `rag_ingest_status` | 入库统计 |

## 使用约定

- 回答产品事实必须标注出处（`path` + 章节）。
- 库内无依据时明确说未找到，禁止编造参数。
- 会议纪要先由 skill 写入 `docs-vault/meeting/`，再跑 ingest。
- 检索为 **混合检索**：BGE（`bge-small-zh-v1.5`）向量 + LanceDB + BM25/TF-IDF。
- 向量库目录：`data/lancedb/`；换嵌入模型必须 `scripts/rebuild.ps1`。
- MCP 工具名保持不变：`rag_search` / `rag_list_sources` / `rag_get_document` / `rag_ingest_status`。

## docs-vault 目录约定（投标/实施场景）

| 目录 | 放什么 | 常用 skill |
|------|--------|------------|
| `products/` | 产品说明、参数表 xlsx、实施方案 | product-qa / param-match |
| `tenders/` | 医院招标文件、答疑、补遗 | bid-respond |
| `bids/` | 我方标书、参数应答表、偏离表 | bid-respond |
| `impl/` | 实施记录、联调问题、培训材料 | impl-support |
| `meeting/` | 会议纪要、需求变更 | meeting-ingest |
| `competitor/` | 竞品与对比材料 | compare-analyze |
| `faq/` | 标准 FAQ | sales-assist |

## Skills（项目业务流）

| Skill ID | 用途 |
|----------|------|
| `product-qa` | 产品/项目知识问答，强制出处 |
| `doc-writer` | 按库内事实写 PRD/说明/方案 |
| `meeting-ingest` | 纪要/需求结构化入库 |
| `sales-assist` | 售前客服话术 |
| `compare-analyze` | 多对象对比 |
| `param-match` | 医院参数 vs 我方参数，能否满足 |
| `bid-respond` | 招标响应策略 + 标书问题排查 |
| `impl-support` | 实施现场问题处理步骤 |

改配置 / 新装 skill 后需**新开对话**。
