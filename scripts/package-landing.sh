#!/usr/bin/env bash
# Empaqueta la carpeta docs/ en landing-ionos.zip listo para subir a IONOS.
set -e
cd "$(dirname "$0")/.."

OUT="landing-ionos.zip"
rm -f "$OUT"

python - <<'PY'
import os, zipfile
SRC = "docs"
OUT = "landing-ionos.zip"
SKIP = {"README.md"}
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for name in sorted(os.listdir(SRC)):
        if name in SKIP:
            continue
        path = os.path.join(SRC, name)
        if os.path.isfile(path):
            z.write(path, arcname=name)
print(f"OK Generado {OUT} ({os.path.getsize(OUT)//1024} KB)")
PY
