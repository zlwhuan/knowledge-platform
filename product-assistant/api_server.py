"""
RAG API Server - FastAPI service for Knowledge Platform integration
Provides REST API for syncing knowledge items to vector database
"""
from __future__ import annotations

import json
import logging
import os
import sys
import threading
from datetime import datetime
from pathlib import Path
from typing import List, Optional

# Add project root to path
PROJECT_ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(PROJECT_ROOT))

# Speed: prefer local HF cache; silence noisy loggers
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")
os.environ.setdefault("HF_HUB_DISABLE_TELEMETRY", "1")

from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel, Field

from ingest.embed import (
    DEFAULT_EMBED_MODEL,
    RagIndex,
    get_embedder,
    mark_model_cached,
    warmup_embedder,
)
from ingest.chunk import Chunk, chunk_document
from ingest.loaders import LoadedDoc
from ingest.utils import project_paths, chunk_id

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("rag-api")

# FastAPI app
app = FastAPI(
    title="Knowledge Platform RAG Service",
    description="REST API for syncing knowledge items to vector database",
    version="1.0.0"
)

# Global index instance
_index: Optional[RagIndex] = None
_index_lock = threading.Lock()
_index_mtime: float = 0.0


# Pydantic models
class AttachmentData(BaseModel):
    """Attachment content data"""
    filename: str
    content: str
    content_type: str = ""
    attachment_id: str = ""
    file_path: str = ""


class SyncRequest(BaseModel):
    """Request model for syncing a knowledge item"""
    item_id: str
    title: str
    content: str
    category: str = ""
    project: str = ""
    tags: List[str] = Field(default_factory=list)
    source: str = ""
    doc_type: str = "products"
    attachments: List[AttachmentData] = Field(default_factory=list)


class SyncBatchRequest(BaseModel):
    """Request model for batch syncing"""
    items: List[SyncRequest]


class SearchRequest(BaseModel):
    """Request model for search"""
    query: str
    top_k: int = 8
    category: Optional[str] = None
    doc_type: Optional[str] = None


class SearchResult(BaseModel):
    """Search result model"""
    rank: int
    score: float
    chunk_id: str
    title: str
    section: str
    page: str = ""
    product: str
    doc_type: str
    path: str
    text: str
    source_kind: str = "item"
    item_id: str = ""
    attachment_id: str = ""
    filename: str = ""
    file_path: str = ""
    locator: str = ""


class SyncResponse(BaseModel):
    """Response model for sync operations"""
    status: str
    item_id: str
    chunks_created: int
    message: str = ""


class StatusResponse(BaseModel):
    """Status response model"""
    status: str
    total_chunks: int
    total_documents: int
    vector_backend: str
    embed_model: str
    vector_ready: bool
    vault_files: int = 0
    hybrid: bool = True
    vector_error: str = ""


class ChunkListItem(BaseModel):
    """Chunk list item for maintenance views"""
    chunk_id: str
    title: str
    section: str
    page: str = ""
    product: str
    doc_type: str
    path: str
    text_preview: str
    source_kind: str = "item"
    item_id: str = ""
    attachment_id: str = ""
    filename: str = ""
    file_path: str = ""
    locator: str = ""


class ChunkListResponse(BaseModel):
    """Chunk list response"""
    total: int
    items: List[ChunkListItem]


class ReindexRequest(BaseModel):
    """Request to reindex a source path"""
    path: str
    mode: str = "source"  # source | full


class RebuildFullRequest(BaseModel):
    """真·全库重建：docs-vault + 平台知识条目"""
    items: Optional[List[SyncRequest]] = None
    keep_existing_platform: bool = False


def _chunks_mtime() -> float:
    """Get modification time of chunks file"""
    paths = project_paths(PROJECT_ROOT)
    try:
        return paths["chunks_file"].stat().st_mtime
    except Exception:
        return 0.0


def get_index() -> RagIndex:
    """Get or create the global index instance"""
    global _index, _index_mtime
    mt = _chunks_mtime()
    with _index_lock:
        if _index is None or mt > _index_mtime:
            _index = RagIndex.load(PROJECT_ROOT)
            _index_mtime = mt or _chunks_mtime()
        return _index


def reload_index() -> RagIndex:
    """Force reload the index"""
    global _index, _index_mtime
    with _index_lock:
        _index = RagIndex.load(PROJECT_ROOT)
        _index_mtime = _chunks_mtime()
        return _index


def _warmup_async() -> None:
    """Async warmup of the embedder"""
    try:
        idx = get_index()
        if idx.embed_meta.get("vector_ok") or idx.vector_ready:
            ok = warmup_embedder(DEFAULT_EMBED_MODEL)
            if ok:
                mark_model_cached()
                get_embedder(DEFAULT_EMBED_MODEL)
                # Prime one encode so first user query is warm
                idx.search("预热", top_k=1)
            logger.info("Embedder warmup completed")
    except Exception as e:
        logger.warning(f"Embedder warmup failed: {e}")


def _start_warmup() -> None:
    """Start background warmup thread"""
    t = threading.Thread(target=_warmup_async, daemon=True, name="rag-warmup")
    t.start()


@app.on_event("startup")
async def startup_event():
    """Initialize on startup"""
    logger.info("Starting RAG API Server...")
    _start_warmup()
    logger.info("RAG API Server started successfully")


@app.get("/")
async def root():
    """Root endpoint"""
    return {"message": "Knowledge Platform RAG Service", "version": "1.0.0"}


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}


@app.get("/api/rag/status", response_model=StatusResponse)
async def get_status():
    """Get RAG service status"""
    try:
        index = get_index()
        paths = project_paths(PROJECT_ROOT)
        
        # Count vault files
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
        
        return StatusResponse(
            status="ready",
            total_chunks=index.size,
            total_documents=len(index.list_sources()),
            vector_backend=index.embed_meta.get("vector_backend", "lancedb"),
            embed_model=index.embed_meta.get("model", DEFAULT_EMBED_MODEL),
            vector_ready=index.vector_ready,
            vault_files=n_files,
            hybrid=bool(index.embed_meta.get("hybrid", True)),
            vector_error=str(index.embed_meta.get("vector_error", "") or ""),
        )
    except Exception as e:
        logger.error(f"Failed to get status: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/rag/sync", response_model=SyncResponse)
async def sync_item(req: SyncRequest, background_tasks: BackgroundTasks):
    """Sync a single knowledge item to vector database"""
    try:
        logger.info(f"Syncing knowledge item: {req.item_id} - {req.title}")

        from ingest.pipeline import chunk_platform_items

        chunks = chunk_platform_items([_sync_to_platform_dict(req)])
        background_tasks.add_task(_update_index_with_chunks, chunks, req.item_id)

        return SyncResponse(
            status="success",
            item_id=req.item_id,
            chunks_created=len(chunks),
            message=f"Queued {len(chunks)} chunks for indexing"
        )
    except Exception as e:
        logger.error(f"Failed to sync item {req.item_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/rag/sync-batch")
async def sync_batch(req: SyncBatchRequest, background_tasks: BackgroundTasks):
    """Sync multiple knowledge items to vector database"""
    try:
        logger.info(f"Batch syncing {len(req.items)} items")

        from ingest.pipeline import chunk_platform_items

        all_chunks = chunk_platform_items([_sync_to_platform_dict(item) for item in req.items])
        background_tasks.add_task(_update_index_with_chunks, all_chunks, "batch")

        return {
            "status": "success",
            "items_count": len(req.items),
            "total_chunks": len(all_chunks),
            "message": f"Queued {len(all_chunks)} chunks for indexing"
        }
    except Exception as e:
        logger.error(f"Batch sync failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def _update_index_with_chunks(chunks: List[Chunk], source: str):
    """Background task to update index with new chunks"""
    try:
        logger.info(f"Updating index with {len(chunks)} chunks from {source}")
        
        index = get_index()
        
        # Remove old chunks for the same items (精确匹配，避免 9 误伤 90)
        from ingest.pipeline import _item_owns_path, chunk_platform_items

        item_ids = set()
        for chunk in chunks:
            if chunk.rel_path.startswith("knowledge_items/"):
                item_id = chunk.rel_path.split("/")[1]
                item_ids.add(item_id)

        if item_ids:
            index.chunks = [
                c for c in index.chunks
                if not any(_item_owns_path(c.rel_path, iid) for iid in item_ids)
            ]
        
        # Add new chunks
        index.chunks.extend(chunks)
        
        # Rebuild index
        index.build(index.chunks, build_vectors=True)
        index.save()
        
        # Reload index
        reload_index()
        
        logger.info(f"Index updated successfully. Total chunks: {index.size}")
    except Exception as e:
        logger.error(f"Failed to update index: {e}")


@app.delete("/api/rag/{item_id}")
async def delete_item(item_id: str, background_tasks: BackgroundTasks):
    """Delete a knowledge item from vector database"""
    try:
        logger.info(f"Deleting item from index: {item_id}")
        
        # Delete in background
        background_tasks.add_task(_delete_from_index, item_id)
        
        return {
            "status": "success",
            "item_id": item_id,
            "message": "Queued for deletion"
        }
    except Exception as e:
        logger.error(f"Failed to delete item {item_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def _delete_from_index(item_id: str):
    """Background task to delete item from index"""
    try:
        index = get_index()
        
        # 处理附件删除：attachment_XXX -> attachments/XXX
        actual_id = item_id
        if item_id.startswith("attachment_"):
            actual_id = item_id[len("attachment_"):]
        
        # Remove chunks for this item（精确匹配）
        from ingest.pipeline import _item_owns_path

        index.chunks = [
            c for c in index.chunks
            if not (
                _item_owns_path(c.rel_path, actual_id)
                or c.rel_path.startswith(f"knowledge_items/attachment_{actual_id}")
            )
        ]
        
        # Rebuild index
        index.build(index.chunks, build_vectors=True)
        index.save()
        
        # Reload index
        reload_index()
        
        logger.info(f"Deleted item {item_id} from index. Remaining chunks: {index.size}")
    except Exception as e:
        logger.error(f"Failed to delete item {item_id}: {e}")


@app.post("/api/rag/search")
async def search(req: SearchRequest):
    """Search the knowledge base"""
    try:
        index = get_index()
        
        if not index.chunks:
            return []
        
        results = index.search(
            query=req.query,
            top_k=req.top_k,
            product=req.category,
            doc_type=req.doc_type
        )
        
        return results
    except Exception as e:
        logger.error(f"Search failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/rag/rebuild")
async def rebuild_index(background_tasks: BackgroundTasks):
    """Rebuild the entire index from docs-vault (keeps existing platform chunks)"""
    try:
        logger.info("Starting full index rebuild")

        # Rebuild in background
        background_tasks.add_task(_rebuild_full_index)

        return {
            "status": "success",
            "message": "Full index rebuild queued"
        }
    except Exception as e:
        logger.error(f"Failed to start rebuild: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/rag/rebuild-full")
async def rebuild_full(req: RebuildFullRequest, background_tasks: BackgroundTasks):
    """真·全库重建：docs-vault + 平台知识条目（列注释拼好的 content）"""
    try:
        if req.items is not None:
            items_payload = [_sync_to_platform_dict(item) for item in req.items]
            logger.info(f"Full library rebuild queued with {len(items_payload)} platform items")
        elif req.keep_existing_platform:
            items_payload = None  # pipeline 会从旧 chunks.jsonl 保留
            logger.info("Full library rebuild queued (keep existing platform chunks)")
        else:
            items_payload = []
            logger.info("Full library rebuild queued (vault only, drop platform chunks)")

        background_tasks.add_task(_rebuild_full_library, items_payload)
        return {
            "status": "success",
            "mode": "full-library",
            "message": "Full library rebuild queued",
        }
    except Exception as e:
        logger.error(f"Failed to start full library rebuild: {e}")
        raise HTTPException(status_code=500, detail=str(e))


def _sync_to_platform_dict(req: SyncRequest) -> dict:
    return {
        "item_id": req.item_id,
        "title": req.title,
        "content": req.content,
        "category": req.category,
        "project": req.project,
        "tags": req.tags,
        "source": req.source,
        "doc_type": req.doc_type,
        "attachments": [
            {
                "filename": a.filename,
                "content": a.content,
                "content_type": a.content_type,
                "attachment_id": a.attachment_id,
                "file_path": a.file_path,
            }
            for a in req.attachments
        ],
    }


async def _rebuild_full_index():
    """Background task to rebuild vault index (keeps platform chunks)"""
    try:
        from ingest.pipeline import rebuild_full_index

        stats = rebuild_full_index(PROJECT_ROOT, platform_items=None)
        reload_index()

        logger.info(f"Full index rebuild completed: {stats}")
    except Exception as e:
        logger.error(f"Full index rebuild failed: {e}")


async def _rebuild_full_library(items_payload: Optional[list]):
    """Background task: vault + platform merged rebuild"""
    try:
        from ingest.pipeline import rebuild_full_index

        # items_payload is None → keep existing; [] → drop platform; list → replace
        stats = rebuild_full_index(PROJECT_ROOT, platform_items=items_payload)
        reload_index()
        logger.info(f"Full library rebuild completed: {stats}")
    except Exception as e:
        logger.error(f"Full library rebuild failed: {e}")


@app.get("/api/rag/sources")
async def list_sources(category: Optional[str] = None, doc_type: Optional[str] = None):
    """List indexed documents"""
    try:
        index = get_index()
        sources = index.list_sources(product=category, doc_type=doc_type)

        return {
            "total": len(sources),
            "sources": sources
        }
    except Exception as e:
        logger.error(f"Failed to list sources: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/rag/chunks", response_model=ChunkListResponse)
async def list_chunks(
    path: Optional[str] = None,
    q: Optional[str] = None,
    doc_type: Optional[str] = None,
    limit: int = 50,
    offset: int = 0,
):
    """List indexed chunks for maintenance"""
    try:
        index = get_index()
        rows = []
        q_lower = (q or "").strip().lower()
        for c in index.chunks:
            if path and c.rel_path != path and not c.rel_path.startswith(path.rstrip("/") + "/"):
                continue
            if doc_type and c.doc_type.lower() != doc_type.lower():
                continue
            if q_lower:
                hay = f"{c.title} {c.section} {c.text} {c.rel_path}".lower()
                if q_lower not in hay:
                    continue
            preview = c.text.replace("\n", " ").strip()
            if len(preview) > 160:
                preview = preview[:160] + "…"
            rows.append(
                ChunkListItem(
                    chunk_id=c.chunk_id,
                    title=c.title,
                    section=c.section,
                    page=c.page,
                    product=c.product,
                    doc_type=c.doc_type,
                    path=c.rel_path,
                    text_preview=preview,
                    source_kind=getattr(c, "source_kind", "item") or "item",
                    item_id=str(getattr(c, "item_id", "") or ""),
                    attachment_id=str(getattr(c, "attachment_id", "") or ""),
                    filename=getattr(c, "filename", "") or "",
                    file_path=getattr(c, "file_path", "") or "",
                    locator=getattr(c, "locator", "") or "",
                )
            )
        total = len(rows)
        start = max(0, offset)
        end = start + max(1, min(limit, 200))
        return ChunkListResponse(total=total, items=rows[start:end])
    except Exception as e:
        logger.error(f"Failed to list chunks: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/rag/chunks/{chunk_id}")
async def get_chunk(chunk_id: str):
    """Get a single chunk full text"""
    try:
        index = get_index()
        for c in index.chunks:
            if c.chunk_id == chunk_id:
                return {
                    "chunk_id": c.chunk_id,
                    "title": c.title,
                    "section": c.section,
                    "page": c.page,
                    "product": c.product,
                    "doc_type": c.doc_type,
                    "path": c.rel_path,
                    "text": c.text,
                    "source_kind": getattr(c, "source_kind", "item") or "item",
                    "item_id": str(getattr(c, "item_id", "") or ""),
                    "attachment_id": str(getattr(c, "attachment_id", "") or ""),
                    "filename": getattr(c, "filename", "") or "",
                    "file_path": getattr(c, "file_path", "") or "",
                    "locator": getattr(c, "locator", "") or "",
                }
        raise HTTPException(status_code=404, detail="chunk not found")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get chunk {chunk_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/rag/chunks/{chunk_id}")
async def delete_chunk(chunk_id: str, background_tasks: BackgroundTasks):
    """Delete a single chunk from the index"""
    try:
        index = get_index()
        if not any(c.chunk_id == chunk_id for c in index.chunks):
            raise HTTPException(status_code=404, detail="chunk not found")
        background_tasks.add_task(_delete_chunk_from_index, chunk_id)
        return {"status": "success", "chunk_id": chunk_id, "message": "Queued for deletion"}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to delete chunk {chunk_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def _delete_chunk_from_index(chunk_id: str):
    """Background task to remove one chunk and rebuild"""
    try:
        index = get_index()
        index.chunks = [c for c in index.chunks if c.chunk_id != chunk_id]
        index.build(index.chunks, build_vectors=True)
        index.save()
        reload_index()
        logger.info(f"Deleted chunk {chunk_id}. Remaining: {index.size}")
    except Exception as e:
        logger.error(f"Failed to delete chunk {chunk_id}: {e}")


@app.delete("/api/rag/sources")
async def delete_source(path: str, background_tasks: BackgroundTasks):
    """Delete all chunks of a source path from the index"""
    try:
        index = get_index()
        prefix = path.rstrip("/")
        matched = [
            c for c in index.chunks
            if c.rel_path == prefix or c.rel_path.startswith(prefix + "/")
        ]
        if not matched:
            raise HTTPException(status_code=404, detail="source not found")
        background_tasks.add_task(_delete_source_from_index, path)
        return {
            "status": "success",
            "path": path,
            "chunks_removed": len(matched),
            "message": "Queued for deletion",
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to delete source {path}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def _delete_source_from_index(path: str):
    """Background task to remove a source and rebuild"""
    try:
        index = get_index()
        prefix = path.rstrip("/")
        index.chunks = [
            c for c in index.chunks
            if not (c.rel_path == prefix or c.rel_path.startswith(prefix + "/"))
        ]
        index.build(index.chunks, build_vectors=True)
        index.save()
        reload_index()
        logger.info(f"Deleted source {path}. Remaining chunks: {index.size}")
    except Exception as e:
        logger.error(f"Failed to delete source {path}: {e}")


@app.post("/api/rag/reindex")
async def reindex_source(req: ReindexRequest, background_tasks: BackgroundTasks):
    """Reindex a single source path or run full rebuild"""
    try:
        mode = (req.mode or "source").lower()
        if mode == "full":
            background_tasks.add_task(_rebuild_full_index)
            return {"status": "success", "mode": "full", "message": "Full rebuild queued"}

        path = (req.path or "").strip()
        if not path:
            raise HTTPException(status_code=400, detail="path is required")

        # knowledge_items/* and attachments/* come from the platform sync API,
        # not docs-vault — reindex by re-embedding existing chunks of that path.
        if path.startswith("knowledge_items/") or path.startswith("attachments/"):
            background_tasks.add_task(_reindex_existing_source, path)
            return {
                "status": "success",
                "mode": "source",
                "path": path,
                "message": "Re-embed queued for platform source",
            }

        background_tasks.add_task(_reindex_vault_source, path)
        return {
            "status": "success",
            "mode": "source",
            "path": path,
            "message": "Re-ingest queued for vault source",
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to reindex: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def _reindex_existing_source(path: str):
    """Re-embed chunks that already belong to a platform source path"""
    try:
        index = get_index()
        prefix = path.rstrip("/")
        kept = [
            c for c in index.chunks
            if c.rel_path == prefix or c.rel_path.startswith(prefix + "/")
        ]
        if not kept:
            logger.warning(f"No chunks found to reindex for {path}")
            return
        index.build(index.chunks, build_vectors=True)
        index.save()
        reload_index()
        logger.info(f"Reindexed platform source {path} ({len(kept)} chunks)")
    except Exception as e:
        logger.error(f"Failed to reindex source {path}: {e}")


async def _reindex_vault_source(path: str):
    """Re-ingest one vault file and merge into the index"""
    try:
        from ingest.pipeline import run_ingest

        # Incremental ingest picks up changed files; force full if path is dir-like
        stats = run_ingest(PROJECT_ROOT, full=False)
        reload_index()
        logger.info(f"Vault reindex finished for {path}: {stats}")
    except Exception as e:
        logger.error(f"Failed to reindex vault source {path}: {e}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "api_server:app",
        host="0.0.0.0",
        port=8081,
        reload=True,
        log_level="info"
    )