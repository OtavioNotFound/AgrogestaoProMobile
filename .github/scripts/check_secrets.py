#!/usr/bin/env python3
"""Falha o CI quando um arquivo versionado contém um padrão provável de segredo."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SELF = Path(__file__).resolve()
MAX_FILE_BYTES = 2 * 1024 * 1024
PATTERNS = (
    re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    re.compile(r"sk_(?:live|test)_[A-Za-z0-9]{20,}"),
    re.compile(r"eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}"),
    re.compile(
        r"(?:service_role|SUPABASE_SERVICE_ROLE|smtp_password)\s*[:=]\s*"
        r"[\"']?[A-Za-z0-9._-]{20,}",
        re.IGNORECASE,
    ),
)


def tracked_files() -> list[Path]:
    result = subprocess.run(
        ["git", "-c", f"safe.directory={ROOT.as_posix()}", "ls-files", "-z"],
        cwd=ROOT,
        check=True,
        capture_output=True,
    )
    return [ROOT / item.decode("utf-8") for item in result.stdout.split(b"\0") if item]


def main() -> int:
    findings: list[str] = []
    for path in tracked_files():
        if path == SELF or path.suffix.lower() == ".md" or not path.is_file():
            continue
        if path.stat().st_size > MAX_FILE_BYTES:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        relative = path.relative_to(ROOT).as_posix()
        for line_number, line in enumerate(text.splitlines(), start=1):
            if any(pattern.search(line) for pattern in PATTERNS):
                findings.append(f"{relative}:{line_number}")

    if findings:
        print("Possível segredo encontrado em arquivo versionado:", file=sys.stderr)
        print("\n".join(findings), file=sys.stderr)
        print("Remova o valor e troque a credencial antes de continuar.", file=sys.stderr)
        return 1

    print("Nenhum padrão conhecido de segredo foi encontrado nos arquivos versionados.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
