from __future__ import annotations

import json
import os
import sys
import threading
from pathlib import Path

PROJECT_ROOT = Path(os.environ.get("RAG_PROJECT_ROOT", Path(__file__).resolve().parents[1]))
sys.path.insert(0, str(PROJECT_ROOT))

# Speed: prefer local HF cache; silence noisy loggers
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")
os.environ.setdefault("HF_HUB_DISABLE_TELEMETRY", "1")

from mcp.server.mcpserver import MCPServer  # noqa: E402

from ingest.embed import (  # noqa: E402
    DEFAULT_EMBED_MODEL,
    RagIndex,
    get_embedder,
    mark_model_cached,
    warmup_embedder,
)
from ingest.utils import project_paths  # noqa: E402

mcp = MCPServer(
    name="product-rag",
    description="Local RAG over company product docs in docs-vault",
    instructions="Search product docs before answering product questions. Always cite path + section.",
)

_index: RagIndex | None = None
_index_lock = threading.Lock()
_warm_lock = threading.Lock()
_index_mtime: float = 0.0
_warm_done = False


def _chunks_mtime() -> float:
    paths = project_paths(PROJECT_ROOT)
    try:
        return paths["chunks_file"].stat().st_mtime
    except Exception:
        return 0.0


def get_index() -> RagIndex:
    global _index, _index_mtime
    mt = _chunks_mtime()
    with _index_lock:
        if _index is None or mt > _index_mtime:
            _index = RagIndex.load(PROJECT_ROOT)
            _index_mtime = mt or _chunks_mtime()
        return _index


def reload_index() -> RagIndex:
    global _index, _index_mtime
    with _index_lock:
        _index = RagIndex.load(PROJECT_ROOT)
        _index_mtime = _chunks_mtime()
        return _index


def _warmup_async() -> None:
    global _warm_done
    try:
        idx = get_index()  # load chunks + BM25 + open LanceDB
        if idx.embed_meta.get("vector_ok") or idx.vector_ready:
            ok = warmup_embedder(DEFAULT_EMBED_MODEL)
            if ok:
                mark_model_cached()
                get_embedder(DEFAULT_EMBED_MODEL)
                # prime one encode so first user query is warm
                idx.search("预热", top_k=1)
    except Exception:
        pass
    finally:
        _warm_done = True


def _start_warmup() -> None:
    with _warm_lock:
        t = threading.Thread(target=_warmup_async, daemon=True, name="rag-warmup")
        t.start()


# Kick background warmup at process start so first tool call is faster
_start_warmup()


def _fmt_results(results: list[dict]) -> str:
    if not results:
        return "未找到相关文档片段。请尝试更换关键词，或确认已运行 ingest 入库。"
    lines = []
    for r in results:
        page = f" p.{r['page']}" if r.get("page") else ""
        lines.append(
            f"### [{r['rank']}] score={r['score']} | {r['title']}\n"
            f"- 出处: `{r['path']}` § {r['section']}{page}\n"
            f"- 产品: {r['product']} | 类型: {r['doc_type']} | chunk: `{r['chunk_id']}`\n\n"
            f"{r['text']}\n"
        )
    return "\n---\n".join(lines)


@mcp.tool()
def rag_search(
    query: str,
    top_k: int = 8,
    product: str | None = None,
    doc_type: str | None = None,
) -> str:
    """在公司产品文档库中混合检索（向量 + BM25）。返回文本块、分数与出处。

    Args:
        query: 检索问题或关键词（中文可用）
        top_k: 返回条数，默认 8
        product: 可选，按产品/文件名过滤
        doc_type: 可选，products/faq/meeting/competitor
    """
    index = get_index()
    results = index.search(query, top_k=top_k, product=product, doc_type=doc_type)
    return _fmt_results(results)


@mcp.tool()
def rag_list_sources(product: str | None = None, doc_type: str | None = None) -> str:
    """列出已入库的文档来源（路径、标题、产品、块数量）。"""
    index = get_index()
    sources = index.list_sources(product=product, doc_type=doc_type)
    if not sources:
        return "知识库为空。请将文档放入 docs-vault/ 后运行 scripts/ingest.ps1。"
    lines = [f"共 {len(sources)} 份文档，索引块总数 {index.size}："]
    for s in sources:
        lines.append(
            f"- `{s['path']}` | {s['title']} | product={s['product']} | type={s['doc_type']} | chunks={s['chunks']}"
        )
    return "\n".join(lines)


@mcp.tool()
def rag_get_document(path: str, max_chars: int = 8000) -> str:
    """按 docs-vault 相对路径读取文档原文（默认最多 8000 字）。用于精读某一条检索命中。"""
    paths = project_paths(PROJECT_ROOT)
    rel = path.replace("\\", "/").lstrip("/")
    if rel.startswith("docs-vault/"):
        rel = rel[len("docs-vault/") :]
    target = (paths["vault"] / rel).resolve()
    vault = paths["vault"].resolve()
    if not str(target).startswith(str(vault)):
        return "路径不合法：必须位于 docs-vault/ 下。"
    if not target.exists() or not target.is_file():
        return f"文件不存在: {rel}"
    if target.suffix.lower() not in {".md", ".markdown", ".txt"}:
        return f"当前仅支持直接读取 md/txt。文件: {rel}（其他格式请依据 rag_search 返回的片段）"
    text = target.read_text(encoding="utf-8", errors="replace")
    if len(text) > max_chars:
        text = text[: max_chars] + f"\n\n...[truncated, total {len(text)} chars]"
    return f"# {rel}\n\n{text}"


@mcp.tool()
def rag_reingest(full: bool = False) -> str:
    """扫描 docs-vault 并增量（或全量）更新向量库。新增/修改文档后，若外部监视未开，可调用本工具。"""
    from ingest.pipeline import run_ingest

    try:
        stats = run_ingest(PROJECT_ROOT, full=bool(full))
    except Exception as exc:  # noqa: BLE001
        return f"入库失败: {exc}"
    reload_index()
    return json.dumps(stats, ensure_ascii=False, indent=2)


@mcp.tool()
def rag_ingest_status() -> str:
    """返回知识库状态：文档数、块数、向量库与模型信息。"""
    paths = project_paths(PROJECT_ROOT)
    index = get_index()
    docs = index.list_sources()
    vault_exists = paths["vault"].exists()
    n_files = 0
    if vault_exists:
        n_files = sum(
            1
            for p in paths["vault"].rglob("*")
            if p.is_file()
            and not p.name.startswith("~$")
            and p.suffix.lower() in {".md", ".txt", ".docx", ".pdf", ".markdown", ".xlsx", ".xlsm"}
        )
    return json.dumps(
        {
            "project_root": str(PROJECT_ROOT),
            "vault": str(paths["vault"]),
            "vault_files": n_files,
            "indexed_docs": len(docs),
            "indexed_chunks": index.size,
            "meta_db": str(paths["meta"]),
            "vector_backend": index.embed_meta.get("vector_backend", "lancedb"),
            "embed_model": index.embed_meta.get("model", ""),
            "vectors": index.vector_store.count_rows(),
            "vector_ready": index.vector_ready,
            "chunks_mtime": _chunks_mtime(),
            "ready": index.size > 0,
        },
        ensure_ascii=False,
        indent=2,
    )


if __name__ == "__main__":
    mcp.run()
