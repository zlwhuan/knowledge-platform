---
name: meeting-ingest
description: 把会议纪要、需求变更、沟通记录整理成结构化文档，并写入 docs-vault/meeting/，便于入库检索。用户粘贴纪要或说「整理会议 / 入库这条需求」时使用。
---

# 会议与需求入库（meeting-ingest）

把纪要写成结构化 Markdown，写入  
`D:\XiaomiMiMoProjects\product-assistant\docs-vault\meeting\YYYY-MM-DD-标题.md`。

## 流程（快）

1. 信息够用则**直接写文件**，不要先检索 RAG。  
2. 缺日期/标题时，用内容推断并标注。  
3. 模板保持精简：结论 / 变更 / 待办表 / 待确认。  
4. 写完后：优先调用 MCP **`rag_reingest`** 更新向量库；若工具不可用，再提示用户执行 `scripts\ingest.ps1`。用户明确要求时才代跑脚本。  

## 禁止

- 把未拍板内容写成已决策  
- 冗长复述原文  
- 未要求时改其他 vault 文档  
