# Architecture MythicRPG Wiki v0.4.0

La v0.4.0 conserve l’architecture validée : extracteur Python, catalogue JSON commun, site Astro statique et export filtré pour la future encyclopédie.

## Source de vérité

`mod-source/src` est une copie exacte de `src(92)` et ne doit jamais être modifiée par la génération. La règle `.gitattributes` `mod-source/src/** -text` protège ses octets.

## Flux

1. les arbres, constantes, enums, registres, recettes et traductions sont lus dans la source ;
2. les contenus éditoriaux Markdown/YAML sont fusionnés ;
3. `data/generated/catalog.json` devient la source du site ;
4. Astro génère les pages statiques ;
5. `encyclopedia.json` réutilise les champs adaptés au manuel en jeu.

Le schéma du catalogue est `0.4.0`.

## Extracteurs spécialisés

- `systems_extract.py` : progression, Mining, Eating, Fishing, Fighting et Crafting ;
- `v040_extract.py` : Traveling, Building, Farming et Woodcutting.

Les pages utilisent des composants spécialisés, mais conservent `SkillTree`, les relations de contenu et les layouts communs.

## JavaScript navigateur

Le JavaScript reste ciblé sur la recherche, les filtres, les arbres et les calculateurs déjà justifiés. Les informations essentielles restent disponibles sans hover et les nouvelles pages n’ajoutent pas d’interactivité décorative.
