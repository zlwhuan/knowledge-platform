from __future__ import annotations

import re
from dataclasses import dataclass

from .loaders import LoadedDoc
from .utils import chunk_id

HEADING_RE = re.compile(r"^(#{1,6})\s+(.*)$")
PAGE_RE = re.compile(r"^\[page\s+(\d+)\]\s*$", re.I)

MAX_CHARS = 700
MIN_CHARS = 80
OVERLAP = 80
TABLE_MAX_CHARS = 1200
TABLE_MIN_CHARS = 24


@dataclass
class Chunk:
    chunk_id: str
    rel_path: str
    title: str
    product: str
    doc_type: str
    section: str
    page: str
    text: str


def _is_table_line(line: str) -> bool:
    return line.count(" | ") >= 2


def _split_long(text: str, max_chars: int = MAX_CHARS, min_chars: int = MIN_CHARS) -> list[str]:
    text = re.sub(r"[ \t]+", " ", text).strip()
    if len(text) <= max_chars:
        return [text] if text else []
    parts: list[str] = []
    start = 0
    n = len(text)
    while start < n:
        end = min(start + max_chars, n)
        if end < n:
            window = text[start:end]
            cut = max(
                window.rfind("\n"),
                window.rfind("。"),
                window.rfind("；"),
                window.rfind(" "),
            )
            if cut >= min_chars:
                end = start + cut + 1
        piece = text[start:end].strip()
        if piece:
            parts.append(piece)
        if end >= n:
            break
        start = max(end - OVERLAP, start + 1)
    return parts


def _make_chunk(doc: LoadedDoc, section: str, page: str, piece: str) -> Chunk:
    return Chunk(
        chunk_id=chunk_id(doc.rel_path, section, piece),
        rel_path=doc.rel_path,
        title=doc.title,
        product=doc.product,
        doc_type=doc.doc_type,
        section=section,
        page=page,
        text=piece,
    )


def chunk_document(doc: LoadedDoc) -> list[Chunk]:
    sections: list[tuple[str, str, list[str]]] = []
    current_section = doc.title
    current_page = ""
    buf: list[str] = []

    def flush():
        nonlocal buf
        body_lines = [ln for ln in buf if ln.strip()]
        buf = []
        if body_lines:
            sections.append((current_section, current_page, body_lines))

    for raw_line in doc.text.splitlines():
        line = raw_line.rstrip()
        page_m = PAGE_RE.match(line.strip())
        if page_m:
            flush()
            current_page = page_m.group(1)
            continue
        h_m = HEADING_RE.match(line.strip())
        if h_m:
            flush()
            heading_text = h_m.group(2).strip() or doc.title
            # 把标题本身也加入缓冲区，以便索引
            buf.append(heading_text)
            current_section = heading_text
            continue
        buf.append(line)
    flush()

    chunks: list[Chunk] = []
    for section, page, body_lines in sections:
        batch: list[str] = []
        plain: list[str] = []
        table_header = ""

        def emit_table_batch():
            nonlocal batch
            if not batch:
                return
            body = "\n".join(batch).strip()
            batch = []
            for piece in _split_long(body, max_chars=TABLE_MAX_CHARS, min_chars=TABLE_MIN_CHARS):
                if len(piece) < 12:
                    continue
                chunks.append(_make_chunk(doc, section, page, piece))

        def emit_plain():
            nonlocal plain
            if not plain:
                return
            body = "\n".join(plain).strip()
            plain = []
            # 如果内容较短，直接作为一个 chunk
            if len(body) < 20:
                if body:
                    chunks.append(_make_chunk(doc, section, page, body))
                return
            for piece in _split_long(body):
                if len(piece) < 20:
                    continue
                chunks.append(_make_chunk(doc, section, page, piece))

        for line in body_lines:
            if _is_table_line(line):
                if not batch:
                    # keep sheet/section + detected header as context
                    prefix = f"[{section}]" if section else ""
                    if table_header:
                        batch.append(table_header)
                    elif prefix:
                        batch.append(prefix)
                batch.append(line)
                if sum(len(x) for x in batch) >= TABLE_MAX_CHARS:
                    emit_table_batch()
            else:
                emit_table_batch()
                # capture short label rows as table header candidates
                if not table_header and " | " in line and len(line) < 80:
                    table_header = line
                plain.append(line)
        emit_table_batch()
        emit_plain()
    return chunks
