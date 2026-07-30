# Tests MythicRPG Wiki v0.1.1

## Tests d’invariants

Ils contrôlent la source inchangée, les neuf skills, les vingt perks par skill, l’unicité des identifiants, les relations, la symétrie des traductions, la provenance des valeurs, l’absence de chemins locaux, l’export encyclopédie et la cohérence des statuts d’objets.

## Tests du snapshot

`tests/test_source_snapshot.py` compare le catalogue à `config/source_snapshot.json`. Les nombres et valeurs propres à `src(91)` ne sont plus dispersés dans les tests d’invariants.

## Tests structurels

Ils contrôlent le `base` GitHub Pages, l’absence de commandes Gradle/Minecraft, l’utilisation de `npm ci`, les routes de recherche, la page de progression et l’absence d’affectations à `innerHTML`.

## Résultat local

Les tests Python passent, à l’exception du contrôle du lockfile qui est explicitement marqué comme bloqué lorsque `website/package-lock.json` est absent.

## Non exécuté

- Gradle, compilation Java et Minecraft ;
- `npm ci` ;
- build Astro ;
- test du site produit dans `website/dist`.

La cause npm exacte est documentée dans `BUILD_VALIDATION.md`.
