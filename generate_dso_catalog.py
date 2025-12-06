#!/usr/bin/env python3
"""
Generatore catalogo DSO per Astro-Lair.

- Scarica OpenNGC (NGC + addendum)
- Converte in formato dso_catalog.json
- (Opzionale) fa git add/commit/push

Dipendenze:
    pip install requests
"""

import csv
import io
import json
import subprocess
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any, Optional

import requests

# =========================
# CONFIG
# =========================

# URL sorgenti OpenNGC
NGC_URL = "https://github.com/mattiaverga/OpenNGC/raw/master/database_files/NGC.csv"
ADDENDUM_URL = "https://github.com/mattiaverga/OpenNGC/raw/master/database_files/addendum.csv"

# Dove salvare il catalogo generato dentro al repo Astro-Lair
# es: astro-lair/catalog/dso_catalog.json
OUTPUT_PATH = Path("catalog/dso_catalog.json")

# Se True, alla fine fa git add/commit/push
AUTO_COMMIT_AND_PUSH = True

GIT_COMMIT_MESSAGE = "Update DSO catalog (generated from OpenNGC)"


# =========================
# FUNZIONI UTILI
# =========================

def fetch_csv(url: str) -> List[Dict[str, str]]:
    """Scarica un CSV da URL e lo restituisce come lista di dict."""
    print(f"[INFO] Scarico CSV da: {url}")
    resp = requests.get(url, timeout=60)
    resp.raise_for_status()
    text = resp.text

    # DictReader su StringIO
    reader = csv.DictReader(io.StringIO(text))
    rows = list(reader)
    print(f"[INFO]   → {len(rows)} righe lette")
    return rows


def try_parse_float(value: Optional[str]) -> Optional[float]:
    if value is None:
        return None
    v = value.strip()
    if not v:
        return None
    try:
        return float(v)
    except ValueError:
        return None


def build_dso_objects(rows: List[Dict[str, str]], default_catalog: str) -> List[Dict[str, Any]]:
    """
    Converte le righe del CSV OpenNGC (NGC.csv o addendum.csv)
    nel formato interno usato da Astro-Lair.
    """
    objects: List[Dict[str, Any]] = []

    for row in rows:
        # I nomi delle colonne in OpenNGC potrebbero essere tipo:
        # Name, Type, RAJ2000, DEJ2000, Const, SurfBr, m_B, m_V, Messier, Common names, ...
        name = (
            row.get("Name")
            or row.get("NAME")
            or row.get("name")
        )

        common = (
            row.get("Common names")
            or row.get("Common name")
            or row.get("CommonName")
            or row.get("Common")
        )

        obj_type = (
            row.get("Type")
            or row.get("TYPE")
        )

        constellation = (
            row.get("Const")
            or row.get("CONST")
            or row.get("Constellation")
        )

        ra_deg = try_parse_float(
            row.get("RAJ2000") or row.get("RAdeg") or row.get("RA")
        )
        dec_deg = try_parse_float(
            row.get("DEJ2000") or row.get("DEdeg") or row.get("Dec")
        )

        # Magnitudine (preferisci V, fallback B)
        mag_v = try_parse_float(row.get("m_V") or row.get("Vmag") or row.get("V_MAG"))
        mag_b = try_parse_float(row.get("m_B") or row.get("Bmag") or row.get("B_MAG"))
        mag = mag_v if mag_v is not None else mag_b

        surface_brightness = try_parse_float(
            row.get("SurfBr") or row.get("SurfBr_V") or row.get("SurfaceBrightness")
        )

        # Messier, se presente
        messier_code = (
            row.get("Messier")
            or row.get("M")
            or ""
        )
        messier_code = messier_code.strip()

        if messier_code:
            catalog = "Messier"
            code = f"M{messier_code}"
            try:
                number = int(messier_code)
            except ValueError:
                number = None
        else:
            catalog = default_catalog
            code = name or ""
            digits = "".join(ch for ch in code if ch.isdigit())
            number = int(digits) if digits else None

        # ID interno dell'oggetto: uso code (Mxx / NGCxxxx ecc)
        obj_id = code or (name or "").strip()

        # Alcune colonne utili aggiuntive (se esistono)
        ngc_designation = row.get("NGC") or None
        ic_designation = row.get("IC") or None

        dso = {
            "id": obj_id,
            "catalog": catalog,
            "code": code,
            "number": number,
            "ngc": ngc_designation,
            "ic": ic_designation,
            "name": (common or name or code or "").strip(),
            "type": obj_type or "",
            "constellation": constellation or "",
            "ra_deg": ra_deg,
            "dec_deg": dec_deg,
            "mag": mag,
            "surface_brightness": surface_brightness,
            "image_url": None,  # Per ora lasciamo vuoto, ci penseremo dopo
        }

        objects.append(dso)

    print(f"[INFO]   → convertiti {len(objects)} oggetti ({default_catalog})")
    return objects


def generate_catalog_json() -> Dict[str, Any]:
    """Scarica NGC + addendum, li converte e restituisce il dict JSON completo."""
    ngc_rows = fetch_csv(NGC_URL)
    add_rows = fetch_csv(ADDENDUM_URL)

    ngc_objects = build_dso_objects(ngc_rows, default_catalog="NGC/IC")
    add_objects = build_dso_objects(add_rows, default_catalog="Addendum")

    all_objects = ngc_objects + add_objects

    catalog = {
        "version": 1,
        "generated_at": datetime.utcnow().isoformat(timespec="seconds") + "Z",
        "source": "OpenNGC (NGC.csv + addendum.csv)",
        "object_count": len(all_objects),
        "objects": all_objects,
    }

    return catalog


def save_catalog_to_file(catalog: Dict[str, Any], path: Path) -> None:
    """Salva il catalogo in JSON pretty."""
    if not path.parent.exists():
        path.parent.mkdir(parents=True, exist_ok=True)

    print(f"[INFO] Salvo catalogo in: {path}")
    with path.open("w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)

    print(f"[INFO] File scritto con successo ({path.stat().st_size} byte)")


def git_commit_and_push(path: Path, message: str) -> None:
    """Esegue git add/commit/push sul file indicato.

    Richiede che:
      - lo script venga eseguito dentro la repo astro-lair
      - git sia configurato (remote origin, credenziali, ecc.)
    """
    print("[INFO] Eseguo git add/commit/push...")

    def run(cmd: list[str]) -> None:
        print(f"[CMD] {' '.join(cmd)}")
        subprocess.run(cmd, check=True)

    run(["git", "add", str(path)])
    # commit può fallire se non ci sono cambi, gestiamo il caso
    try:
        run(["git", "commit", "-m", message])
    except subprocess.CalledProcessError as e:
        print("[WARN] git commit fallito (probabilmente nessuna modifica). Dettagli:")
        print(f"       {e}")
        return

    run(["git", "push"])
    print("[INFO] git push completato.")


def main() -> None:
    print("=== Astro-Lair DSO Catalog Generator ===")

    catalog = generate_catalog_json()
    save_catalog_to_file(catalog, OUTPUT_PATH)

    if AUTO_COMMIT_AND_PUSH:
        try:
            git_commit_and_push(OUTPUT_PATH, GIT_COMMIT_MESSAGE)
        except Exception as e:
            print(f"[ERROR] Problema durante git add/commit/push: {e}")
            print("        Controlla di essere nella repo corretta e con git configurato.")


if __name__ == "__main__":
    main()
