from __future__ import annotations

import argparse
import json
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path

from .chunk import chunk_document
from .embed import RagIndex
from .loaders import iter_docs, load_document
from .utils import file_sha256, project_paths


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
    if paths["chunks_file"].exists() and not full:
        with paths["chunks_file"].open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                row = json.loads(line)
                prev_by_path.setdefault(row["rel_path"], []).append(row)

    final_chunks = []
    from .chunk import Chunk

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
                final_chunks.append(
                    Chunk(
                        chunk_id=row["chunk_id"],
                        rel_path=row["rel_path"],
                        title=row["title"],
                        product=row["product"],
                        doc_type=row["doc_type"],
                        section=row["section"],
                        page=row.get("page", ""),
                        text=row["text"],
                    )
                )

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
