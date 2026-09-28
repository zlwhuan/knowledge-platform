# Knowledge Platform 环境与部署清单

> 给「下一次在其他电脑上部署」用。可把本文丢给 MiMo Desktop / Claude / 任意桌面 AI，按步骤检查并安装缺失项。

**目标**：在一台 Windows 机器上跑起 3 个服务 + 1 个数据库。

| 服务 | 目录 | 端口 | 启动脚本 |
|------|------|------|----------|
| 后端 Spring Boot | `backend/` | 8080 | `Start-Backend.ps1` |
| 前端 Vite/Vue | `frontend/` | 5173 | `Start-Frontend.ps1` |
| RAG 向量服务 (FastAPI) | `product-assistant/` | 8081 | `Start-RAGService.ps1` |
| MySQL | 系统服务 | 3306 | — |

**参考版本（本机已验证）**：Windows 10/11 · JDK 17 · Maven 3.9+ · Node 24 · npm 11 · Python 3.12 · MySQL 8.4

---

## 0. 给 AI 助手的执行说明

1. 按 **第 1 步 → 第 6 步** 顺序执行；每步先「检查」，缺失才「安装」。
2. 每条检查命令都是可直接跑的；输出为空/报错 = 未安装。
3. 安装后务必重跑对应检查命令确认。
4. 第 7 步「验证清单」全部通过才算部署完成。
5. 不要把 API Key 写进仓库；用环境变量或启动脚本输入。

---

## 1. 系统与基础工具

### 1.1 Windows + PowerShell

```powershell
$PSVersionTable.PSVersion   # 建议 5.1+ 或 7+
```

### 1.2 Git（拉代码用）

```powershell
git --version
```

安装：https://git-scm.com/download/win

### 1.3 JDK 17

```powershell
java -version
# 期望：17.x
```

安装（Eclipse Temurin / Adoptium 17）：
```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e
```

若 JAVA_HOME 未指向 JDK17，改 `Start-Backend.ps1` 里：
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.xxx-hotspot"
```

### 1.4 Maven（可用项目自带 wrapper，可选）

```powershell
# 后端目录有 mvnw.cmd，可不装全局 Maven
Test-Path backend\mvnw.cmd   # 应为 True
mvn -v   # 可选
```

---

## 2. 数据库 MySQL 8.x

### 检查

```powershell
mysql --version
# 或
Get-Service -Name "*mysql*" 
```

```powershell
mysql -h localhost -P 3306 -u root -p -e "SELECT VERSION();"
```

### 安装

```powershell
winget install --id Oracle.MySQL -e
# 或用 MySQL Installer 8.4
```

### 初始化

1. 建库（连接串默认）：

```sql
CREATE DATABASE IF NOT EXISTS knowledge_platform
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. 导入结构与示例数据（可选，仓库根目录有 `knowledge_platform.sql`）：

```powershell
mysql -u root -p knowledge_platform < knowledge_platform.sql
```

3. 后端 `application.properties` 默认连接：

```
jdbc:mysql://localhost:3306/knowledge_platform
用户名 root / 密码 Admin@123
```

密码不同则设环境变量 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`，或改配置文件。

4. 表结构由 JPA `ddl-auto=update` 自动补齐（含 `kp_skill`）。

---

## 3. Node.js 与前端

### 检查

```powershell
node -v    # 期望 >= 18
npm -v
```

### 安装

```powershell
winget install --id OpenJS.NodeJS.LTS -e
```

### 依赖安装

```powershell
cd frontend
npm install
```

主要包：`vue` `vue-router` `element-plus` `echarts` `axios` `md-editor-v3` `vite`（见 `frontend/package.json`）。

---

## 4. Python 与 RAG 服务（product-assistant）

### 检查

```powershell
python --version   # 期望 3.10–3.12
```

### 建虚拟环境

```powershell
cd product-assistant
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### 安装依赖（建议国内镜像）

```powershell
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

关键包（`requirements.txt`）：

| 包 | 用途 |
|----|------|
| fastapi / uvicorn / pydantic | RAG HTTP API |
| lancedb | 向量库 |
| sentence-transformers / torch | BGE 嵌入 |
| rank_bm25 / scikit-learn / jieba | 混合检索 |
| pypdf / python-docx / openpyxl | 文档抽取 |
| rapidocr-onnxruntime | **图片 OCR** |
| mcp | MiMo 技能检索 |

**嵌入模型**：首次运行会拉 `BAAI/bge-small-zh-v1.5`（约 100MB+）。网络差时先配置镜像：

```powershell
$env:HF_ENDPOINT = "https://hf-mirror.com"
```

也可用 `RAG_EMBED_MODEL` 换模型（换完需重建向量库）。

### 端口与数据

- API：`http://localhost:8081`
- 向量数据：`product-assistant/data/`（勿手改）
- 原始文档（可选，MCP 用）：`product-assistant/docs-vault/`

---

## 5. 业务后端（Spring Boot）

### 检查

```powershell
java -version          # 17
Test-Path backend\mvnw.cmd
```

### 编译

```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
```

首次会下载 Maven 依赖（Spring Boot 4.x、POI、PDFBox、Jsoup、MySQL 驱动、tess4j、springdoc 等），需能访问 Maven 中央仓库。

### 配置（application.properties / 环境变量）

| 变量 | 默认 | 说明 |
|------|------|------|
| `DB_URL` | jdbc:mysql://localhost:3306/knowledge_platform?… | 数据库 |
| `DB_USERNAME` | root | |
| `DB_PASSWORD` | Admin@123 | |
| `RAG_SERVICE_URL` | http://localhost:8081 | 向量服务 |
| `RAG_ENABLED` | true | |
| `LLM_API_BASE_URL` | — | 技能助手模型（OpenAI 兼容） |
| `LLM_MODEL` | — | 如 mimo-v2.6-flash |
| `LLM_API_KEY` | — | 启动时手动输入更安全 |
| `CORS_ALLOWED_ORIGINS` | http://localhost:5173,… | 前端跨域 |

本机常用模型端点（小米 token-plan）：

```
LLM_API_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
LLM_MODEL=mimo-v2.6-flash
```

---

## 6. 一键启动（按顺序）

```powershell
# 1) RAG（先起，后端会调它）
.\Start-RAGService.ps1

# 2) 后端（会提示输入 LLM_API_KEY）
.\Start-Backend.ps1

# 3) 前端
.\Start-Frontend.ps1
```

浏览器打开：`http://localhost:5173`

---

## 7. 验收清单（部署完成标准）

| # | 检查项 | 命令 / 操作 | 期望 |
|---|--------|-------------|------|
| 1 | MySQL 连通 | `mysql -uroot -p -e "USE knowledge_platform; SHOW TABLES;"` | 有表 |
| 2 | RAG 健康 | `curl http://127.0.0.1:8081/health` | `{"status":"healthy"}` |
| 3 | 后端健康 | 浏览器 `http://localhost:8080/api/skills/readiness` 或 swagger | 200 |
| 4 | 前端登录 | 打开 `http://localhost:5173` | 出登录页 |
| 5 | 知识库 | 登录后「知识库」 | 能看到条目 |
| 6 | 向量库 | 「向量库→库健康」 | 服务在线、有切块数 |
| 7 | 快捷搜索 | 搜产品关键词 | 有结果、出处可点 |
| 8 | 技能助手 | 选「产品知识问答」提问 | 流式回答 + 出处 |
| 9 | 管理员技能管理 | 右上「管理技能」 | 可增删改（仅 ADMIN） |
| 10 | 附件预览 | 点附件出处 | 打开预览而非下载 |

### 一键体检脚本（给 AI/运维贴这段）

```powershell
Write-Host "== Java =="; java -version
Write-Host "== Node =="; node -v; npm -v
Write-Host "== MySQL =="; mysql --version
Write-Host "== RAG =="; try { (Invoke-WebRequest http://127.0.0.1:8081/health -TimeoutSec 5).Content } catch { "RAG DOWN" }
Write-Host "== Backend =="; try { (Invoke-WebRequest http://127.0.0.1:8080/api/skills/readiness -TimeoutSec 5 -UseBasicParsing).StatusCode } catch { "Backend DOWN" }
Write-Host "== Frontend =="; try { (Invoke-WebRequest http://127.0.0.1:5173 -TimeoutSec 5 -UseBasicParsing).StatusCode } catch { "Frontend DOWN" }
Write-Host "== venv =="; Test-Path product-assistant\.venv\Scripts\python.exe
Write-Host "== node_modules =="; Test-Path frontend\node_modules
```

---

## 8. 可选组件

| 组件 | 用途 | 不装的影响 |
|------|------|------------|
| LibreOffice | Office 转 PDF 预览 | docx/xlsx/pptx 预览降级（可下载） |
| OnlyOffice | 在线文档协作预览 | 关闭 `preview.onlyoffice.enabled` 亦可 |
| Tesseract OCR | 图片 OCR 回退 | 用 RapidOCR 即可，非必须 |
| MiMo Desktop | AI 辅助部署/开发 | 与运行时无关 |

LibreOffice 路径（若安装）写在 `application.properties`：
```
preview.office.command=C:/Program Files/LibreOffice/program/soffice.exe
```

---

## 9. 常见问题

**8081 向量库不可达**  
- 只起一个 uvicorn（用 `.venv` 那个）  
- `Get-NetTCPConnection -LocalPort 8081` 看是否被占  
- 杀掉重复 `api_server.py` 再跑 `Start-RAGService.ps1`

**技能助手 401 / 超时**  
- 401：检查 `LLM_API_KEY`（不要带 `Bearer`、空格）  
- connect timeout：外网到 `token-plan-cn.xiaomimimo.com` 不通，换网络或加代理

**图片 OCR 无效**  
- 确认 `rapidocr-onnxruntime` 已装在 RAG 的 venv  
- 首次 OCR 需要加载 ONNX 模型，稍慢属正常

**附件不进向量库**  
- 看后端日志 `Skip attachment` / `Attachment file not found`  
- 路径需在 `backend/uploads/` 下；改完可「全库重建」

**JAR 被占用无法 clean**  
- 旧后端进程占文件：结束 `java.exe` 里命令行含 `knowledge-platform` 的进程

---

## 10. 目录速查

```
knowledge-platform/
├── Start-Backend.ps1 / Start-Frontend.ps1 / Start-RAGService.ps1
├── backend/                 # Spring Boot
│   ├── pom.xml
│   └── src/main/resources/skills/   # 默认技能 JSON（首次入库）
├── frontend/                # Vue + Vite
├── product-assistant/       # RAG (FastAPI + LanceDB)
│   ├── requirements.txt
│   ├── api_server.py
│   └── .venv/               # 本地虚拟环境
├── docs/                    # 部署与 SQL 文档
└── knowledge_platform.sql   # 数据库导出（可选）
```
