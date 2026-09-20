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
            vector_ready=index.vector_ready
        )
    except Exception as e:
        logger.error(f"Failed to get status: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/rag/sync", response_model=SyncResponse)
async def sync_item(req: SyncRequest, background_tasks: BackgroundTasks):
    """Sync a single knowledge item to vector database"""
    try:
        logger.info(f"Syncing knowledge item: {req.item_id} - {req.title}")
        
        # Build document content
        doc_text = req.content
        
        # Create main document
        doc = LoadedDoc(
            path=None,
            rel_path=f"knowledge_items/{req.item_id}",
            title=req.title,
            product=req.project or req.category or req.title,
            doc_type=req.doc_type,
            text=doc_text
        )
        
        # Chunk the main content
        chunks = chunk_document(doc)
        
        # Process attachments
        for att in req.attachments:
            if att.content and att.content.strip():
                att_doc = LoadedDoc(
                    path=None,
                    rel_path=f"attachments/{req.item_id}/{att.filename}",
                    title=f"{req.title} - {att.filename}",
                    product=req.project or req.category or req.title,
                    doc_type="attachment",
                    text=att.content
                )
                att_chunks = chunk_document(att_doc)
                chunks.extend(att_chunks)
                logger.info(f"  Added {len(att_chunks)} chunks from attachment: {att.filename}")
        
        # Add to index in background
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
        
        all_chunks = []
        for item in req.items:
            # Build document
            doc = LoadedDoc(
                path=None,
                rel_path=f"knowledge_items/{item.item_id}",
                title=item.title,
                product=item.project or item.category or item.title,
                doc_type=item.doc_type,
                text=item.content
            )
            chunks = chunk_document(doc)
            
            # Process attachments
            for att in item.attachments:
                if att.content and att.content.strip():
                    att_doc = LoadedDoc(
                        path=None,
                        rel_path=f"attachments/{item.item_id}/{att.filename}",
                        title=f"{item.title} - {att.filename}",
                        product=item.project or item.category or item.title,
                        doc_type="attachment",
                        text=att.content
                    )
                    chunks.extend(chunk_document(att_doc))
            
            all_chunks.extend(chunks)
        
        # Add to index in background
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
        
        # Remove old chunks for the same items
        item_ids = set()
        for chunk in chunks:
            if chunk.rel_path.startswith("knowledge_items/"):
                item_id = chunk.rel_path.split("/")[1]
                item_ids.add(item_id)
        
        # Filter out old chunks for these items
        if item_ids:
            index.chunks = [
                c for c in index.chunks
                if not any(
                    c.rel_path.startswith(f"knowledge_items/{iid}")
                    or c.rel_path.startswith(f"attachments/{iid}")
                    for iid in item_ids
                )
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
        
        # Remove chunks for this item
        # 支持两种格式：knowledge_items/XXX 和 knowledge_items/attachment_XXX
        index.chunks = [
            c for c in index.chunks
            if not (
                c.rel_path.startswith(f"knowledge_items/{actual_id}")
                or c.rel_path.startswith(f"knowledge_items/attachment_{actual_id}")
                or c.rel_path.startswith(f"attachments/{actual_id}/")
                or c.rel_path == f"attachments/{actual_id}"
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
    """Rebuild the entire index from docs-vault"""
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


async def _rebuild_full_index():
    """Background task to rebuild full index"""
    try:
        from ingest.pipeline import run_ingest
        
        stats = run_ingest(PROJECT_ROOT, full=True)
        reload_index()
        
        logger.info(f"Full index rebuild completed: {stats}")
    except Exception as e:
        logger.error(f"Full index rebuild failed: {e}")


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


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "api_server:app",
        host="0.0.0.0",
        port=8081,
        reload=True,
        log_level="info"
    )