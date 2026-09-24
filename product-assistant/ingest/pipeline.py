from __future__ import annotations

import argparse
import json
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path

from .chunk import Chunk, build_locator, chunk_document
from .embed import RagIndex
from .loaders import LoadedDoc, iter_docs, load_document
from .utils import file_sha256, project_paths

# 平台同步进来的切块路径前缀（MySQL 知识条目 / 附件），不走 docs-vault
PLATFORM_PREFIXES = ("knowledge_items/", "attachments/")


def is_platform_path(rel_path: str) -> bool:
    return rel_path.startswith(PLATFORM_PREFIXES)


def _row_to_chunk(row: dict) -> Chunk:
    source_kind = row.get("source_kind") or (
        "attachment" if row["rel_path"].startswith("attachments/") else "item"
    )
    return Chunk(
        chunk_id=row["chunk_id"],
        rel_path=row["rel_path"],
        title=row["title"],
        product=row["product"],
        doc_type=row["doc_type"],
        section=row["section"],
        page=row.get("page", ""),
        text=row["text"],
        source_kind=source_kind,
        item_id=str(row.get("item_id") or ""),
        attachment_id=str(row.get("attachment_id") or ""),
        filename=row.get("filename") or "",
        file_path=row.get("file_path") or "",
        locator=row.get("locator")
        or build_locator(source_kind, row.get("filename") or "", row.get("section") or "", row.get("page") or "", row.get("title") or ""),
    )


def load_platform_chunks(paths: dict) -> list[Chunk]:
    """从 chunks.jsonl 读回平台切块，避免 vault 重建时丢掉 MySQL 同步内容。"""
    if not paths["chunks_file"].exists():
        return []
    out: list[Chunk] = []
    with paths["chunks_file"].open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            row = json.loads(line)
            if is_platform_path(row["rel_path"]):
                out.append(_row_to_chunk(row))
    return out


def _item_owns_path(rel_path: str, item_id: str) -> bool:
    """精确匹配条目所属切块路径，避免 id=9 误伤 id=90。"""
    iid = str(item_id).strip()
    if not iid:
        return False
    return (
        rel_path == f"knowledge_items/{iid}"
        or rel_path.startswith(f"knowledge_items/{iid}/")
        or rel_path.startswith(f"attachments/{iid}/")
        or rel_path == f"attachments/{iid}"
        or rel_path.startswith(f"knowledge_items/attachment_{iid}")
    )


def chunk_platform_items(items: list[dict]) -> list[Chunk]:
    """把平台知识条目（已拼好列注释文本）切成向量块。

    两级文档：
      - 条目：knowledge_items/{item_id}，locator=条目正文 · 章节
      - 附件：attachments/{item_id}/{attachment_id}/{filename}，locator=附件名 · p.x · 章节
    每块都带元信息头 + 结构化出处字段。
    """
    from .loaders import LoadedDoc as _LD

    chunks: list[Chunk] = []
    for item in items or []:
        item_id = str(item.get("item_id") or item.get("id") or "").strip()
        title = (item.get("title") or "").strip()
        content = item.get("content") or ""
        category = (item.get("category") or "").strip()
        project = (item.get("project") or "").strip()
        doc_type = (item.get("doc_type") or "").strip() or (
            category.split("/")[0] if category else "knowledge"
        )
        product = category or title
        meta_header = _meta_header(title, category, project)

        if content.strip():
            doc = _LD(
                path=None,
                rel_path=f"knowledge_items/{item_id}",
                title=title,
                product=product,
                doc_type=doc_type,
                text=content,
            )
            # 结构化出处挂在 doc 上，供 _make_chunk 使用
            doc.source_kind = "item"
            doc.item_id = item_id
            doc.attachment_id = ""
            doc.filename = ""
            doc.file_path = ""
            for c in chunk_document(doc):
                c.product = product
                c.doc_type = doc_type
                c.text = meta_header + c.text
                c.locator = build_locator("item", "", c.section, c.page, title)
                chunks.append(c)

        for att in item.get("attachments") or []:
            att_text = (att.get("content") or "").strip()
            if not att_text:
                continue
            filename = att.get("filename") or "attachment"
            attachment_id = str(att.get("attachment_id") or att.get("id") or "").strip()
            file_path = (att.get("file_path") or "").strip()
            rel = (
                f"attachments/{item_id}/{attachment_id}/{filename}"
                if attachment_id
                else f"attachments/{item_id}/{filename}"
            )
            att_header = meta_header + f"原始文件名：{filename}\n"
            att_doc = _LD(
                path=None,
                rel_path=rel,
                title=f"{title} - {filename}" if title else filename,
                product=product,
                doc_type=doc_type,
                text=att_text,
            )
            att_doc.source_kind = "attachment"
            att_doc.item_id = item_id
            att_doc.attachment_id = attachment_id
            att_doc.filename = filename
            att_doc.file_path = file_path
            for c in chunk_document(att_doc):
                c.product = product
                c.doc_type = doc_type
                c.text = att_header + c.text
                c.source_kind = "attachment"
                c.item_id = item_id
                c.attachment_id = attachment_id
                c.filename = filename
                c.file_path = file_path
                c.locator = build_locator("attachment", filename, c.section, c.page, title)
                chunks.append(c)
    return chunks


def _meta_header(title: str, category: str, project: str) -> str:
    return (
        f"标题：{title}\n"
        f"所属分类ID：{category}\n"
        f"关联项目ID：{project}\n"
    )


def _ensure_meta(conn: sqlite3.Connection) -> None:
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS files (
            rel_path TEXT PRIMARY KEY,
            sha256 TEXT NOT NULL,
            title TEXT,
            product TEXT,
            doc_type TEXT,
            chunks INTEGER,
            ingested_at TEXT
        )
        """
    )
    conn.commit()


def run_ingest(project_root: Path, full: bool = False) -> dict:
    paths = project_paths(project_root)
    paths["vault"].mkdir(parents=True, exist_ok=True)
    paths["data"].mkdir(parents=True, exist_ok=True)
    paths["chunks"].mkdir(parents=True, exist_ok=True)
    paths["vectors"].mkdir(parents=True, exist_ok=True)
    paths["lancedb"].mkdir(parents=True, exist_ok=True)

    conn = sqlite3.connect(paths["meta"])
    _ensure_meta(conn)
    existing = {
        row[0]: row[1]
        for row in conn.execute("SELECT rel_path, sha256 FROM files")
    }

    all_chunks = []
    doc_meta = {}
    changed = 0
    skipped = 0
    failed = []

    for path in iter_docs(paths["vault"]):
        doc = load_document(path, paths["vault"])
        if doc is None:
            failed.append(str(path))
            continue
        sha = file_sha256(path)
        if not full and existing.get(doc.rel_path) == sha:
            # reload old chunks from store for unchanged docs
            skipped += 1
            continue
        try:
            chunks = chunk_document(doc)
        except Exception as exc:  # noqa: BLE001
            failed.append(f"{doc.rel_path}: {exc}")
            continue
        changed += 1
        doc_meta[doc.rel_path] = (sha, doc.title, doc.product, doc.doc_type, len(chunks))
        all_chunks.extend(chunks)

    # Unchanged docs: pull their chunks from previous JSONL
    prev_by_path: dict[str, list] = {}
    platform_chunks: list[Chunk] = []
    if paths["chunks_file"].exists():
        with paths["chunks_file"].open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                row = json.loads(line)
                if is_platform_path(row["rel_path"]):
                    platform_chunks.append(_row_to_chunk(row))
                    continue
                if full:
                    continue
                prev_by_path.setdefault(row["rel_path"], []).append(row)

    final_chunks = []

    for rel, (sha, title, product, doc_type, n) in doc_meta.items():
        final_chunks.extend([c for c in all_chunks if c.rel_path == rel])

    retained_paths = set(doc_meta.keys())
    for rel, rows in prev_by_path.items():
        if rel in retained_paths:
            continue
        if rel not in existing:
            continue
        # file still in vault? if disappeared from scan, drop it
        still = any(p.as_posix().endswith(rel) or True for p in [])
        vault_file = paths["vault"] / rel
        if not vault_file.exists():
            continue
        # unchanged file
        sha = file_sha256(vault_file)
        if existing.get(rel) == sha:
            for row in rows:
                final_chunks.append(_row_to_chunk(row))

    # 平台切块始终保留（除非本次通过 rebuild_full 用新数据覆盖）
    final_chunks.extend(platform_chunks)

    index = RagIndex(project_root)
    # Always rebuild vector table on --full; otherwise rebuild when anything changed
    index.build(final_chunks, build_vectors=True)
    index.save()

    now = datetime.now(timezone.utc).isoformat()
    for rel, (sha, title, product, doc_type, n) in doc_meta.items():
        conn.execute(
            """
            INSERT INTO files(rel_path, sha256, title, product, doc_type, chunks, ingested_at)
            VALUES(?,?,?,?,?,?,?)
            ON CONFLICT(rel_path) DO UPDATE SET
                sha256=excluded.sha256,
                title=excluded.title,
                product=excluded.product,
                doc_type=excluded.doc_type,
                chunks=excluded.chunks,
                ingested_at=excluded.ingested_at
            """,
            (rel, sha, title, product, doc_type, n, now),
        )
    # prune DB rows for deleted files
    vault_rels = set()
    for path in iter_docs(paths["vault"]):
        doc_rel = path.resolve().relative_to(paths["vault"].resolve()).as_posix()
        vault_rels.add(doc_rel)
    for rel in list(existing.keys()):
        if rel not in vault_rels:
            conn.execute("DELETE FROM files WHERE rel_path=?", (rel,))
    conn.commit()
    conn.close()

    stats = {
        "docs_indexed": index.list_sources() and len(index.list_sources()) or len({c.rel_path for c in final_chunks}),
        "chunks": len(final_chunks),
        "changed_docs": changed,
        "skipped_unchanged": skipped,
        "failed": failed,
        "meta_db": str(paths["meta"]),
        "index": str(paths["index_file"]),
    }
    stats["docs_indexed"] = len({c.rel_path for c in final_chunks})
    stats["vector_backend"] = index.embed_meta.get("vector_backend")
    stats["embed_model"] = index.embed_meta.get("model")
    stats["vectors"] = index.embed_meta.get("vectors") or index.vector_store.count_rows()
    stats["vector_ok"] = index.embed_meta.get("vector_ok", stats["vectors"] > 0)
    if index.embed_meta.get("vector_error"):
        stats["vector_error"] = index.embed_meta["vector_error"]
    return stats


def rebuild_full_index(project_root: Path, platform_items: list[dict] | None = None) -> dict:
    """真·全库重建：仅平台知识条目（MySQL），不再读 docs-vault。

    platform_items:
      - 传入列表：用本次数据重建平台切块（列注释拼好的 content + 附件文本）
      - 传 None：从旧 chunks.jsonl 保留现有平台切块
      - 传 []：清空全部索引
    """
    paths = project_paths(project_root)
    paths["data"].mkdir(parents=True, exist_ok=True)
    paths["chunks"].mkdir(parents=True, exist_ok=True)
    paths["vectors"].mkdir(parents=True, exist_ok=True)
    paths["lancedb"].mkdir(parents=True, exist_ok=True)

    # 仅平台切块（docs-vault 已废弃）
    if platform_items is not None:
        platform_chunks = chunk_platform_items(platform_items)
    else:
        platform_chunks = load_platform_chunks(paths)

    index = RagIndex(project_root)
    index.build(platform_chunks, build_vectors=True)
    index.save()

    stats = {
        "mode": "platform-only",
        "docs_indexed": len({c.rel_path for c in platform_chunks}),
        "chunks": len(platform_chunks),
        "platform_chunks": len(platform_chunks),
        "platform_items": len(platform_items) if platform_items is not None else None,
        "failed": [],
        "meta_db": str(paths["meta"]),
        "index": str(paths["index_file"]),
    }
    stats["vector_backend"] = index.embed_meta.get("vector_backend")
    stats["embed_model"] = index.embed_meta.get("model")
    stats["vectors"] = index.embed_meta.get("vectors") or index.vector_store.count_rows()
    stats["vector_ok"] = index.embed_meta.get("vector_ok", stats["vectors"] > 0)
    if index.embed_meta.get("vector_error"):
        stats["vector_error"] = index.embed_meta["vector_error"]
    return stats


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Ingest docs-vault into local RAG index")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="project root",
    )
    parser.add_argument("--full", action="store_true", help="force reindex all files")
    args = parser.parse_args(argv)
    stats = run_ingest(args.root, full=args.full)
    print(json.dumps(stats, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
