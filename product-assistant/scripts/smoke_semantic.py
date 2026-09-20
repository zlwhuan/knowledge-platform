import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from ingest.embed import RagIndex

idx = RagIndex.load(ROOT)
print("docs", len(idx.list_sources()), "chunks", idx.size)
print("vector_ready", idx.vector_ready)
print("embed_meta", idx.embed_meta)
print("lancedb_rows", idx.vector_store.count_rows())
print()

queries = [
    "断网了数据会不会丢",
    "系统能不能减少护士写文书的工作量",
    "X1 最大并发连接",
    "如何添加设备",
    "项目实施有哪些阶段",
]
for q in queries:
    print("Q:", q)
    for r in idx.search(q, top_k=3):
        text = r["text"][:55].replace("\n", " ")
        print(f"  {r['score']:.3f} | {r['path'][:48]} | {r['section'][:24]} | {text}")
    print()
