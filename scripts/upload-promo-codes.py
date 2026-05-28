#!/usr/bin/env python
"""
Sube los códigos promocionales de firestore-promo-codes.json a la colección
`promoCodes` de Firebase.

Requisitos:
  pip install firebase-admin
  Una service account JSON guardada como `service-account.json` en la raíz
  del repo (Firebase Console → Configuración del proyecto → Cuentas de
  servicio → Generar nueva clave privada).

Uso:
  python scripts/upload-promo-codes.py
"""
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CODES_FILE = ROOT / "firestore-promo-codes.json"
SERVICE_ACCOUNT = ROOT / "service-account.json"


def main():
    if not SERVICE_ACCOUNT.exists():
        print(f"ERROR: falta {SERVICE_ACCOUNT.name} en la raiz del repo.")
        print("Descargalo desde Firebase Console -> Configuracion del proyecto")
        print("-> Cuentas de servicio -> Generar nueva clave privada.")
        sys.exit(1)

    if not CODES_FILE.exists():
        print(f"ERROR: falta {CODES_FILE.name}. Genera primero los codigos.")
        sys.exit(1)

    import firebase_admin
    from firebase_admin import credentials, firestore

    cred = credentials.Certificate(str(SERVICE_ACCOUNT))
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    codes = json.loads(CODES_FILE.read_text(encoding="utf-8"))
    coll = db.collection("promoCodes")

    created = 0
    skipped = 0
    batch = db.batch()
    for code in codes:
        ref = coll.document(code)
        snap = ref.get()
        if snap.exists:
            print(f"  - {code} ya existe, lo dejo como esta.")
            skipped += 1
            continue
        batch.set(ref, {"used": False})
        created += 1
    batch.commit()

    print()
    print(f"OK: creados {created}, ya existian {skipped}.")
    print("Los codigos se pueden canjear desde la app en Perfil -> Quitar")
    print("anuncios -> Tienes un codigo?")


if __name__ == "__main__":
    main()
