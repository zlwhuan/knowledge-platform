"""Watch docs-vault and auto-run incremental ingest when files change."""
from __future__ import annotations

import argparse
import hashlib
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from ingest.loaders import SUPPORTED, iter_docs  # noqa: E402
from ingest.pipeline import run_ingest  # noqa: E402
from ingest.utils import project_paths  # noqa: E402


def vault_fingerprint(vault: Path) -> str:
    h = hashlib.sha256()
    if not vault.exists():
        return "empty"
    for p in iter_docs(vault):
        try:
            st = p.stat()
            h.update(str(p.resolve()).encode("utf-8", "ignore"))
            h.update(str(st.st_mtime_ns).encode())
            h.update(str(st.st_size).encode())
        except OSError:
            continue
    return h.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="Auto-ingest docs-vault on change")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--interval", type=float, default=3.0, help="seconds between scans")
    parser.add_argument("--once", action="store_true", help="single scan then exit")
    args = parser.parse_args()
    paths = project_paths(args.root)
    vault = paths["vault"]
    last = vault_fingerprint(vault)
    print(f"[watch] root={args.root} vault={vault} interval={args.interval}s", flush=True)
    print(f"[watch] initial fingerprint={last[:12]}…", flush=True)
    if args.once:
        stats = run_ingest(args.root, full=False)
        print("[watch] ingest:", stats, flush=True)
        return 0
    while True:
        time.sleep(max(args.interval, 1.0))
        fp = vault_fingerprint(vault)
        if fp == last:
            continue
        print(f"[watch] change detected {last[:12]}… -> {fp[:12]}…", flush=True)
        try:
            stats = run_ingest(args.root, full=False)
            print(
                f"[watch] ingested docs={stats.get('docs_indexed')} "
                f"changed={stats.get('changed_docs')} "
                f"chunks={stats.get('chunks')} "
                f"vectors={stats.get('vectors')} "
                f"vector_ok={stats.get('vector_ok')}",
                flush=True,
            )
        except Exception as exc:  # noqa: BLE001
            print(f"[watch] ingest failed: {exc}", flush=True)
            time.sleep(5)
            continue
        last = fp


if __name__ == "__main__":
    raise SystemExit(main())
