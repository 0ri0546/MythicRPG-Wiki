# Architecture MythicRPG Wiki v0.2

## Source de vérité

`mod-source/src/` est une copie exacte et en lecture seule de `src(92)`. Son empreinte est contrôlée avant et après chaque extraction. `.gitattributes` protège ses octets de la normalisation des fins de ligne.

## Extracteur Python

- `java_extract.py` : arbres, constantes explicitement documentées et preuves d’enregistrement ;
- `resource_extract.py` : traductions, modèles, textures, blockstates et recettes JSON ;
- `systems_extract.py` : progression, fossiles, cuisine et systèmes Fishing ;
- `editorial.py` : Markdown et frontmatter YAML ;
- `build.py` : fusion, relations, recherche, rapports et export encyclopédie ;
- `utils.py` : parsing numérique sûr, sérialisation et empreinte multiplateforme.

Le Java est lu mais jamais exécuté.

## Catalogue partagé

`catalog.json` contient les données techniques, les textes éditoriaux, leur provenance et les structures interactives. `encyclopedia.json` applique les règles d’audience et retire les champs réservés au site.

## Site Astro

Astro génère le HTML statique. TypeScript n’est hydraté que pour les interactions utiles :

- recherche et filtres ;
- arbres de perks ;
- graphiques et calculateurs d’XP ;
- comparateurs Mining, Eating et Fishing ;
- relations entre contenus.

Aucune API, base de données ou serveur applicatif n’est nécessaire.

## Couverture des skills

Les neuf skills disposent de leur page, de leurs 20 perks, de leur arbre interactif et d’un contenu éditorial structuré. Mining, Eating et Fishing possèdent en plus des extracteurs spécialisés approfondis dans cette version.

## Sécurité d’affichage

Les interactions créent les éléments avec `createElement`, `textContent` et `replaceChildren`. Aucune affectation à `innerHTML` n’est utilisée pour injecter les données extraites.

## GitHub Pages

Le `base` est calculé depuis `GITHUB_REPOSITORY`. Le workflow exécute l’extraction, les tests, `npm ci`, le build Astro et publie `website/dist/`.
