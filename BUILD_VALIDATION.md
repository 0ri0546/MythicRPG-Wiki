# Validation du build Astro v0.2

## État dans cet environnement

- `website/package-lock.json` présent ;
- suite Python : validée ;
- génération du catalogue : validée ;
- `npm ci` : bloqué par le miroir npm interne, qui répond 404 pour certaines archives présentes dans le lockfile ;
- build Astro v0.2 : à relancer dans l’environnement Windows ou GitHub Actions déjà validé pour le projet.

Ce blocage ne vient pas du projet et aucun lockfile alternatif n’a été fabriqué.

## Commande de validation officielle

```bash
python scripts/build_all.py
```

Le script exécute l’extraction, les tests, `npm ci`, `npm run build`, puis `verify_delivery.py --require-build`.
