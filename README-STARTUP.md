# Knowledge Platform 启动指南

## 快速启动

### 方式一：使用批处理脚本（推荐）

```batch
双击运行 Start-Services.bat
```

这会打开 3 个命令行窗口，分别运行：
- RAG Service (端口 8081)
- Backend (端口 8080)
- Frontend (端口 5173)

### 方式二：手动启动

打开 3 个 PowerShell/CMD 窗口，分别运行：

**窗口 1 - RAG Service:**
```powershell
cd D:\Projects\knowledge-platform\product-assistant
.\.venv\Scripts\python.exe -m uvicorn api_server:app --host 0.0.0.0 --port 8081
```

**窗口 2 - Backend:**
```powershell
cd D:\Projects\knowledge-platform\backend
.\mvnw.cmd spring-boot:run
```

**窗口 3 - Frontend:**
```powershell
cd D:\Projects\knowledge-platform\frontend
npm run dev
```

## 验证服务状态

运行测试脚本：
```powershell
.\test-services.ps1
```

或手动检查：

1. **RAG Service**: 访问 http://localhost:8081/health
2. **Backend**: 访问 http://localhost:8080/swagger-ui.html
3. **Frontend**: 访问 http://localhost:5173

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend | 5173 | Vue 3 前端界面 |
| Backend | 8080 | Spring Boot 后端 API |
| RAG Service | 8081 | Python 向量化服务 |

## 常见问题

### 1. 后端启动缓慢
Spring Boot 首次启动需要下载依赖并编译，可能需要 1-3 分钟。请耐心等待。

### 2. 端口被占用
如果端口被占用，可以修改配置：
- 后端: `backend/src/main/resources/application.properties` 中的 `server.port`
- RAG 服务: 启动命令中的 `--port` 参数
- 前端: `frontend/vite.config.js` 中的 `server.port`

### 3. Java 找不到
确保安装了 Java 17 或更高版本，并配置了 JAVA_HOME 环境变量。

### 4. Python 依赖缺失
```powershell
cd product-assistant
.\.venv\Scripts\pip install -r requirements.txt
.\.venv\Scripts\pip install fastapi uvicorn pydantic python-multipart
```

## 测试 RAG 功能

1. 启动所有服务
2. 访问前端 http://localhost:5173
3. 创建知识条目并上传附件
4. 系统会自动向量化内容
5. 使用 MiMo Desktop 的 `product-qa` 技能查询

## 停止服务

关闭各个命令行窗口，或按 `Ctrl+C` 停止服务。

## 查看日志

- RAG Service: 查看运行 RAG 服务的命令行窗口
- Backend: 查看运行后端的命令行窗口
- Frontend: 查看运行前端的命令行窗口