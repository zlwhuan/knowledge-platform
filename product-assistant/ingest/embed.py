from __future__ import annotations

import json
import logging
import os
import re
import threading
from pathlib import Path

# Prefer CN mirror; after first download use offline cache to skip HF network probes
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
os.environ.setdefault("HF_HUB_OFFLINE", "0")  # set to 1 after model is cached
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")

import jieba  # noqa: E402
import numpy as np  # noqa: E402
from rank_bm25 import BM25Okapi  # noqa: E402
from sklearn.feature_extraction.text import TfidfVectorizer  # noqa: E402
from sklearn.preprocessing import normalize  # noqa: E402

from .chunk import Chunk  # noqa: E402
from .utils import project_paths  # noqa: E402

_token_re = re.compile(r"[一-鿿A-Za-z0-9]+")

DEFAULT_EMBED_MODEL = os.environ.get(
    "RAG_EMBED_MODEL", "BAAI/bge-small-zh-v1.5"
)
TABLE_NAME = "chunks"

_model_lock = threading.Lock()
_model_cache: dict[str, object] = {}
_embedder_cache: dict[str, "Embedder"] = {}



def tokenize(text: str) -> list[str]:
    text = text.lower()
    tokens = _token_re.findall(text)
    out: list[str] = []
    for tok in tokens:
        if re.fullmatch(r"[一-鿿]+", tok):
            out.extend(jieba.lcut(tok))
        else:
            out.append(tok)
        if len(tok) >= 2:
            out.append(tok)
    return [t for t in out if t.strip()]


def _bge_query(text: str) -> str:
    # bge recommends instruction prefix for queries
    return f"为这个句子生成表示以用于检索相关文章：{text}"


class Embedder:
    def __init__(self, model_name: str = DEFAULT_EMBED_MODEL):
        self.model_name = model_name
        self._model = None
        self.dim: int | None = None

    @property
    def available(self) -> bool:
        try:
            self._ensure()
            return True
        except Exception:
            return False

    def _ensure(self):
        if self._model is not None:
            return self._model
        with _model_lock:
            if self.model_name in _model_cache:
                self._model = _model_cache[self.model_name]
                self._dim_from_model()
                return self._model
            from sentence_transformers import SentenceTransformer

            # Quiet HF noise; prefer cache after first successful download
            logging.getLogger("sentence_transformers").setLevel(logging.WARNING)
            logging.getLogger("huggingface_hub").setLevel(logging.WARNING)
            local_only = os.environ.get("RAG_EMBED_OFFLINE", "").lower() in {
                "1",
                "true",
                "yes",
            }
            try:
                self._model = SentenceTransformer(
                    self.model_name, local_files_only=local_only
                )
            except Exception:
                if local_only:
                    self._model = SentenceTransformer(self.model_name)
                else:
                    raise
            _model_cache[self.model_name] = self._model
            self._dim_from_model()
        return self._model

    def _dim_from_model(self):
        try:
            if hasattr(self._model, "get_embedding_dimension"):
                self.dim = int(self._model.get_embedding_dimension())
            else:
                self.dim = int(self._model.get_sentence_embedding_dimension())
        except Exception:
            self.dim = None

    def encode_texts(self, texts: list[str]) -> np.ndarray:
        model = self._ensure()
        if not texts:
            return np.zeros((0, self.dim or 384), dtype=np.float32)
        vecs = model.encode(
            texts,
            batch_size=32,
            convert_to_numpy=True,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return np.asarray(vecs, dtype=np.float32)

    def encode_query(self, query: str) -> np.ndarray:
        model = self._ensure()
        vec = model.encode(
            [_bge_query(query)],
            convert_to_numpy=True,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return np.asarray(vec, dtype=np.float32)[0]


def get_embedder(model_name: str = DEFAULT_EMBED_MODEL) -> Embedder:
    if model_name not in _embedder_cache:
        _embedder_cache[model_name] = Embedder(model_name)
    return _embedder_cache[model_name]


def warmup_embedder(model_name: str = DEFAULT_EMBED_MODEL) -> bool:
    try:
        emb = get_embedder(model_name)
        emb._ensure()
        emb.encode_query("预热")
        return True
    except Exception:
        return False


def mark_model_cached() -> None:
    """After first successful local download, turn on HF offline mode."""
    os.environ["RAG_EMBED_OFFLINE"] = "1"
    os.environ["HF_HUB_OFFLINE"] = "1"
    os.environ["TRANSFORMERS_OFFLINE"] = "1"



class LanceVectorStore:
    def __init__(self, db_path: Path, model_name: str = DEFAULT_EMBED_MODEL):
        self.db_path = Path(db_path)
        self.model_name = model_name
        self.embedder = get_embedder(model_name)
        self._db = None
        self._table = None
        self._by_id: dict[str, dict] = {}

    @property
    def ready(self) -> bool:
        try:
            self._ensure_table()
            return self.count_rows() > 0
        except Exception:
            return False

    def _ensure_db(self):
        if self._db is None:
            import lancedb

            self.db_path.mkdir(parents=True, exist_ok=True)
            self._db = lancedb.connect(str(self.db_path))
        return self._db

    def _ensure_table(self):
        if self._table is not None:
            return self._table
        db = self._ensure_db()
        names = db.table_names()
        if TABLE_NAME not in names:
            self._table = None
            return None
        self._table = db.open_table(TABLE_NAME)
        self._reload_lookup()
        return self._table

    def _reload_lookup(self):
        self._by_id = {}
        if self._table is None:
            return
        try:
            rows = self._table.to_pandas()
        except Exception:
            return
        for _, row in rows.iterrows():
            self._by_id[str(row["chunk_id"])] = row.to_dict()

    def count_rows(self) -> int:
        t = self._ensure_table()
        if t is None:
            return 0
        try:
            return int(t.count_rows())
        except Exception:
            return len(self._by_id)

    def overwrite(self, chunks: list[Chunk]) -> dict:
        import lancedb

        self.db_path.mkdir(parents=True, exist_ok=True)
        self._db = lancedb.connect(str(self.db_path))
        if TABLE_NAME in self._db.table_names():
            self._db.drop_table(TABLE_NAME)

        if not chunks:
            self._table = None
            self._by_id = {}
            return {"vectors": 0, "model": self.model_name, "dim": None}

        texts = [c.text for c in chunks]
        vecs = self.embedder.encode_texts(texts)
        dim = int(vecs.shape[1]) if vecs.ndim == 2 and vecs.shape[0] else (
            self.embedder.dim or 384
        )
        data = [
            {
                "chunk_id": c.chunk_id,
                "rel_path": c.rel_path,
                "title": c.title,
                "product": c.product,
                "doc_type": c.doc_type,
                "section": c.section,
                "page": c.page,
                "text": c.text,
                "vector": vecs[i].tolist() if len(vecs) else [0.0] * dim,
            }
            for i, c in enumerate(chunks)
        ]
        self._table = self._db.create_table(TABLE_NAME, data=data)
        self._reload_lookup()
        meta = {
            "model": self.model_name,
            "dim": dim,
            "rows": len(data),
        }
        return {"vectors": len(data), "model": self.model_name, "dim": dim}

    def search(
        self,
        query: str,
        top_k: int = 8,
        product: str | None = None,
        doc_type: str | None = None,
    ) -> list[dict]:
        t = self._ensure_table()
        if t is None or not query.strip():
            return []
        try:
            qvec = self.embedder.encode_query(query)
        except Exception:
            return []
        # fetch extra if filtering
        fetch_k = top_k
        if product or doc_type:
            fetch_k = min(max(top_k * 8, 40), max(self.count_rows(), top_k))
        try:
            res = (
                t.search(qvec)
                .limit(fetch_k)
                .to_list()
            )
        except Exception:
            return []
        out: list[dict] = []
        for row in res:
            if product and str(row.get("product", "")).lower() != product.lower():
                continue
            if doc_type and str(row.get("doc_type", "")).lower() != doc_type.lower():
                continue
            out.append(
                {
                    "chunk_id": str(row.get("chunk_id", "")),
                    "title": row.get("title", ""),
                    "section": row.get("section", ""),
                    "page": str(row.get("page") or ""),
                    "product": row.get("product", ""),
                    "doc_type": row.get("doc_type", ""),
                    "path": row.get("rel_path", ""),
                    "text": row.get("text", ""),
                    "_distance": float(row.get("_distance", 0.0) or 0.0),
                }
            )
            if len(out) >= top_k:
                break
        return out


class RagIndex:
    def __init__(self, project_root: Path):
        self.paths = project_paths(project_root)
        self.chunks: list[Chunk] = []
        self.doc_ids: list[str] = []
        self._bm25: BM25Okapi | None = None
        self._vectorizer: TfidfVectorizer | None = None
        self._tfidf = None
        self.vector_store = LanceVectorStore(
            self.paths["lancedb"], DEFAULT_EMBED_MODEL
        )
        self.embed_meta: dict = {}
        self._lexical_ready = False

    @property
    def size(self) -> int:
        return len(self.chunks)

    @property
    def vector_ready(self) -> bool:
        return self.vector_store.ready

    def build(self, chunks: list[Chunk], build_vectors: bool = True) -> None:
        self.chunks = list(chunks)
        corpus = [c.text for c in self.chunks]
        tokenized = [tokenize(t) for t in corpus]
        self._bm25 = BM25Okapi(tokenized) if tokenized else None
        self._vectorizer = TfidfVectorizer(
            tokenizer=tokenize,
            token_pattern=None,
            ngram_range=(1, 2),
            min_df=1,
            sublinear_tf=True,
        )
        if corpus:
            matrix = self._vectorizer.fit_transform(corpus)
            self._tfidf = normalize(matrix, norm="l2")
        else:
            self._vectorizer = None
            self._tfidf = None

        self.embed_meta = {
            "model": DEFAULT_EMBED_MODEL,
            "vector_backend": "lancedb",
            "hybrid": True,
            "chunks": len(self.chunks),
        }
        if build_vectors:
            try:
                stats = self.vector_store.overwrite(self.chunks)
                self.embed_meta.update(stats)
                self.embed_meta["vector_ok"] = stats.get("vectors", 0) > 0
            except Exception as exc:  # noqa: BLE001
                self.embed_meta["vector_ok"] = False
                self.embed_meta["vector_error"] = str(exc)

    def save(self) -> None:
        paths = self.paths
        paths["vectors"].mkdir(parents=True, exist_ok=True)
        paths["chunks"].mkdir(parents=True, exist_ok=True)
        import joblib

        rows = []
        for c in self.chunks:
            rows.append(
                {
                    "chunk_id": c.chunk_id,
                    "rel_path": c.rel_path,
                    "title": c.title,
                    "product": c.product,
                    "doc_type": c.doc_type,
                    "section": c.section,
                    "page": c.page,
                    "text": c.text,
                }
            )
        with paths["chunks_file"].open("w", encoding="utf-8") as f:
            for row in rows:
                f.write(json.dumps(row, ensure_ascii=False) + "\n")

        payload = {
            "vectorizer": self._vectorizer,
            "tfidf": self._tfidf,
            "chunk_ids": [c.chunk_id for c in self.chunks],
            "tokenized": [tokenize(c.text) for c in self.chunks],
        }
        joblib.dump(payload, paths["index_file"])
        meta = {
            "size": len(self.chunks),
            "docs": len({c.rel_path for c in self.chunks}),
        }
        joblib.dump(meta, paths["vocab_file"])
        paths["embed_meta"].write_text(
            json.dumps(self.embed_meta, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    @classmethod
    def load(cls, project_root: Path) -> "RagIndex":
        paths = project_paths(project_root)
        index = cls(project_root)
        if paths["embed_meta"].exists():
            try:
                index.embed_meta = json.loads(
                    paths["embed_meta"].read_text(encoding="utf-8")
                )
            except Exception:
                index.embed_meta = {}
        if not paths["chunks_file"].exists():
            return index
        import json as _json
        import joblib

        chunks: list[Chunk] = []
        with paths["chunks_file"].open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                row = _json.loads(line)
                chunks.append(
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
        index.chunks = chunks
        if paths["index_file"].exists():
            payload = joblib.load(paths["index_file"])
            index._vectorizer = payload.get("vectorizer")
            index._tfidf = payload.get("tfidf")
            tokenized = payload.get("tokenized")
            if tokenized and len(tokenized) == len(chunks):
                index._bm25 = BM25Okapi(tokenized) if tokenized else None
            else:
                tokenized = [tokenize(c.text) for c in chunks]
                index._bm25 = BM25Okapi(tokenized) if tokenized else None
            index._lexical_ready = True
        # open lancedb table if present
        try:
            index.vector_store._ensure_table()
        except Exception:
            pass
        # model is cached on disk; prefer offline to skip HF HTTP on warm start
        try:
            model_dir = Path.home() / ".cache" / "huggingface" / "hub"
            key = DEFAULT_EMBED_MODEL.replace("/", "--")
            if any(model_dir.glob(f"models--{key}*")):
                mark_model_cached()
        except Exception:
            pass
        return index

    @staticmethod
    def _norm(x: np.ndarray) -> np.ndarray:
        if x.size == 0:
            return x
        mx = float(x.max())
        if mx <= 0:
            return np.zeros_like(x)
        return x / mx

    @staticmethod
    def _rrf(rank_lists: list[list[str]], k: int = 60) -> dict[str, float]:
        scores: dict[str, float] = {}
        for ranks in rank_lists:
            for i, cid in enumerate(ranks, 1):
                scores[cid] = scores.get(cid, 0.0) + 1.0 / (k + i)
        return scores

    def search(
        self,
        query: str,
        top_k: int = 8,
        product: str | None = None,
        doc_type: str | None = None,
    ) -> list[dict]:
        if not self.chunks or not query.strip():
            return []

        # lexical scores
        q_tokens = tokenize(query)
        bm25_scores = np.zeros(len(self.chunks))
        if self._bm25 is not None and q_tokens:
            bm25_scores = np.asarray(self._bm25.get_scores(q_tokens), dtype=float)
        tfidf_scores = np.zeros(len(self.chunks))
        if self._vectorizer is not None and self._tfidf is not None:
            q_vec = normalize(self._vectorizer.transform([query]), norm="l2")
            tfidf_scores = (self._tfidf @ q_vec.T).toarray().ravel()

        by_id = {c.chunk_id: c for c in self.chunks}
        id_order = [c.chunk_id for c in self.chunks]

        # vector scores
        vec_score = {cid: 0.0 for cid in id_order}
        vec_hits: list[dict] = []
        if self.vector_ready:
            try:
                vec_hits = self.vector_store.search(
                    query,
                    top_k=max(top_k * 3, 24),
                    product=product,
                    doc_type=doc_type,
                )
            except Exception:
                vec_hits = []
            for rank, hit in enumerate(vec_hits, 1):
                cid = hit.get("chunk_id", "")
                # cosine similarity approx from normalized vectors; lance distance
                # use rank-based contribution primarily
                dist = hit.get("_distance", 1.0) or 1.0
                sim = 1.0 / (1.0 + max(dist, 0.0))
                vec_score[cid] = max(vec_score.get(cid, 0.0), sim)
                _ = rank

        # rank lists for RRF
        bm25_order = [id_order[i] for i in np.argsort(-bm25_scores) if bm25_scores[i] > 0]
        tfidf_order = [id_order[i] for i in np.argsort(-tfidf_scores) if tfidf_scores[i] > 0]
        vec_order = [h["chunk_id"] for h in vec_hits if h.get("chunk_id") in by_id]
        rrf = self._rrf([bm25_order[:50], tfidf_order[:50], vec_order[:50]])

        fused = np.zeros(len(self.chunks))
        has_vec = bool(vec_order)
        for i, cid in enumerate(id_order):
            score = rrf.get(cid, 0.0)
            if has_vec:
                score += 0.35 * vec_score.get(cid, 0.0)
            else:
                score += 0.25 * float(self._norm(bm25_scores)[i])
                score += 0.20 * float(self._norm(tfidf_scores)[i])
            # always keep a lexical component for exact model numbers
            score += 0.15 * float(self._norm(bm25_scores)[i])
            fused[i] = score

        q_lower = query.lower()
        for i, c in enumerate(self.chunks):
            hay = f"{c.title} {c.section} {c.rel_path} {c.product}".lower()
            if q_lower and q_lower in hay:
                fused[i] += 0.08
            if product and c.product.lower() == product.lower():
                fused[i] += 0.06
            if doc_type and c.doc_type.lower() == doc_type.lower():
                fused[i] += 0.03
            if product and c.product.lower() != product.lower():
                fused[i] = -1.0
            if doc_type and c.doc_type.lower() != doc_type.lower():
                fused[i] = -1.0

        order = np.argsort(-fused)[: max(top_k, 1)]
        results = []
        for rank, idx in enumerate(order, 1):
            c = self.chunks[int(idx)]
            score = float(fused[int(idx)])
            if score < 0:
                continue
            results.append(
                {
                    "rank": rank,
                    "score": round(score, 4),
                    "chunk_id": c.chunk_id,
                    "title": c.title,
                    "section": c.section,
                    "page": c.page,
                    "product": c.product,
                    "doc_type": c.doc_type,
                    "path": c.rel_path,
                    "text": c.text,
                }
            )
        return results

    def list_sources(self, product: str | None = None, doc_type: str | None = None) -> list[dict]:
        seen: dict[str, dict] = {}
        for c in self.chunks:
            if product and c.product.lower() != product.lower():
                continue
            if doc_type and c.doc_type.lower() != doc_type.lower():
                continue
            key = c.rel_path
            if key not in seen:
                seen[key] = {
                    "path": c.rel_path,
                    "title": c.title,
                    "product": c.product,
                    "doc_type": c.doc_type,
                    "chunks": 0,
                }
            seen[key]["chunks"] += 1
        return sorted(seen.values(), key=lambda x: x["path"])
