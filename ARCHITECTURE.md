# Architecture MythicRPG Wiki v0.3.0

## Source de vérité

`mod-source/src/` est une copie exacte et en lecture seule de `src(92)`. Son empreinte est contrôlée avant et après chaque extraction. `.gitattributes` protège ses octets de la normalisation des fins de ligne avec `mod-source/src/** -text`.

## Extracteur Python

- `java_extract.py` : arbres, constantes explicitement documentées et preuves d’enregistrement ;
- `resource_extract.py` : traductions, modèles, textures, blockstates et recettes JSON ;
- `systems_extract.py` : progression, Mining, Eating, Fishing, Fighting et Crafting ;
- `editorial.py` : Markdown et frontmatter YAML ;
- `build.py` : fusion, relations, recherche, rapports et export encyclopédie ;
- `utils.py` : parsing numérique sûr, sérialisation et empreinte multiplateforme.

Le Java est lu mais jamais exécuté.

## Catalogue partagé

`catalog.json` contient les données techniques, les textes éditoriaux, leur provenance et les structures utilisées par le site. `encyclopedia.json` applique les règles d’audience et retire les champs réservés au site.

La version de schéma du catalogue est `0.3.0`.

## Site Astro

Astro génère le HTML statique. TypeScript ou JavaScript n’est utilisé que pour les interactions utiles :

- recherche et filtres ;
- arbres de perks et simulation de builds ;
- calculateurs ;
- catalogues de Barons ;
- explorateurs Craft Score, transformations et Lucky Blocks ;
- relations entre contenus.

Aucune API, base de données ou serveur applicatif n’est nécessaire.

## Couverture des skills

Les neuf skills disposent de leur page, de leurs 20 perks, de leur arbre interactif et d’une liste statique complète des perks et prérequis.

Couverture approfondie actuelle :

- Mining ;
- Eating ;
- Fishing ;
- Fighting ;
- Crafting.

Fighting et Crafting utilisent des extracteurs spécialisés ajoutés en v0.3.0. Les quatre autres skills conservent leur couverture structurée en attendant leur lot documentaire dédié.

## Fighting

Les données spécialisées couvrent :

- formules d’XP normale et Baron ;
- conditions et paliers de promotion ;
- scaling de vie, dégâts et récompense ;
- 25 types, compatibilités de créatures et comportements ;
- récompenses et cinq objets légendaires ;
- autorité serveur en solo et multijoueur.

## Crafting

Les données spécialisées couvrent :

- Craft Score et formules d’XP ;
- stations, durabilités et propriétés d’interface ;
- bonus de fabrication et états persistants ;
- recyclage ;
- transformations ;
- Lucky Blocks, événements pondérés et infusions ;
- autorité serveur.

## Sécurité d’affichage

Les interactions utilisent `createElement`, `textContent`, `replaceChildren` ou des nœuds Astro générés statiquement. Aucune affectation à `innerHTML` n’est utilisée pour injecter les données extraites.

## GitHub Pages

Le `base` est calculé depuis `GITHUB_REPOSITORY`. Le workflow exécute l’extraction, les tests, `npm ci`, le build Astro et publie `website/dist/`.
