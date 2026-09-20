import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from ingest.embed import RagIndex

idx = RagIndex.load(ROOT)
print("docs", len(idx.list_sources()), "chunks", idx.size)
print()
for s in idx.list_sources():
    print("-", s["path"], "|", s["title"][:40], "| chunks=", s["chunks"])
print()

queries = [
    "重症项目实施方案",
    "项目进度计划",
    "招标技术方案",
    "X1 最大并发连接",
    "如何添加设备",
]
for q in queries:
    print("Q:", q)
    for r in idx.search(q, top_k=3):
        text = r["text"][:60].replace("\n", " ")
        print(f"  {r['score']:.3f} | {r['path']} | {r['section'][:30]} | {text}")
    print()
