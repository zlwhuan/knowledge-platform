from __future__ import annotations

import hashlib
from pathlib import Path


def file_sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def chunk_id(rel_path: str, section: str, text: str) -> str:
    raw = f"{rel_path}|{section}|{text[:80]}".encode("utf-8")
    return hashlib.sha1(raw).hexdigest()[:16]


def project_paths(project_root: Path) -> dict[str, Path]:
    root = project_root.resolve()
    return {
        "root": root,
        "vault": root / "docs-vault",
        "data": root / "data",
        "chunks": root / "data" / "chunks",
        "vectors": root / "data" / "vectors",
        "lancedb": root / "data" / "lancedb",
        "meta": root / "data" / "meta.sqlite",
        "chunks_file": root / "data" / "chunks" / "chunks.jsonl",
        "index_file": root / "data" / "vectors" / "index.npz",
        "vocab_file": root / "data" / "vectors" / "vectorizer.joblib",
        "embed_meta": root / "data" / "vectors" / "embed_meta.json",
    }
