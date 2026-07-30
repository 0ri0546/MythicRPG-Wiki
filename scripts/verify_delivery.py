#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def fail(message: str) -> None:
    raise SystemExit(f'ERREUR: {message}')


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--require-build', action='store_true', help='Exiger package-lock.json et website/dist.')
    args = parser.parse_args()

    result = subprocess.run(
        [sys.executable, '-m', 'unittest', 'discover', '-s', 'tests', '-v'],
        cwd=ROOT,
        text=True,
    )
    if result.returncode:
        fail('La suite de tests Python a échoué.')

    snapshot = json.loads((ROOT / 'config/source_snapshot.json').read_text(encoding='utf-8'))
    report = json.loads((ROOT / 'data/generated/extraction-report.json').read_text(encoding='utf-8'))
    catalog = json.loads((ROOT / 'data/generated/catalog.json').read_text(encoding='utf-8'))
    if report['status'] != 'ok' or report['errors']:
        fail('Le rapport d’extraction contient des erreurs.')
    if catalog['source']['tree_sha256'] != snapshot['tree_sha256']:
        fail('L’empreinte de la source ne correspond pas au snapshot.')

    source_files = [path for path in (ROOT / 'mod-source/src').rglob('*') if path.is_file()]
    if len(source_files) != snapshot['counts']['source_files']:
        fail('Le nombre de fichiers source ne correspond pas au snapshot.')

    forbidden = ('/mnt/data/', 'wiki_v011_work', 'mythicrpg_site_work')
    for path in (ROOT / 'data/generated').glob('*.json'):
        text = path.read_text(encoding='utf-8')
        if any(token in text for token in forbidden):
            fail(f'Chemin local détecté dans {path.relative_to(ROOT)}.')

    lockfile = ROOT / 'website/package-lock.json'
    dist = ROOT / 'website/dist'
    build_ready = lockfile.is_file() and dist.is_dir() and any(dist.rglob('*.html'))
    if args.require_build and not build_ready:
        fail('Validation de build exigée, mais lockfile ou sortie Astro manquante.')

    print('Seconde vérification indépendante: OK pour le périmètre statique')
    print(f"Pages Astro: {len(list((ROOT / 'website/src/pages').rglob('*.astro')))}")
    print(f"Fichiers source du mod préservés: {len(source_files)}")
    print(f"Rapport: {report['counts']}")
    print(f"Lockfile présent: {lockfile.is_file()}")
    print(f"Build Astro validé: {build_ready}")
    if not build_ready:
        print('BLOCAGE CONNU: voir BUILD_VALIDATION.md')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
