# Architecture MythicRPG Wiki v0.1.1

## Source en lecture seule

`mod-source/src/` contient les 1 206 fichiers de la source reçue. Le nom reçu est `src(92).zip`, mais son SHA-256 et son arbre correspondent à la source canonique documentée `src(91).zip`. Le détail est conservé dans `config/source_snapshot.json`.

## Extracteur Python

- `java_extract.py` : skills, arbres de perks, constantes explicitement suivies et indices d’enregistrement ;
- `resource_extract.py` : traductions, modèles, textures, blockstates et recettes ;
- `editorial.py` : Markdown et frontmatter YAML ;
- `build.py` : fusion, validation, recherche et export encyclopédie ;
- `utils.py` : sérialisation et empreintes.

Le Java est lu statiquement et n’est jamais exécuté.

## Catalogue partagé

Les sorties `catalog.json`, `encyclopedia.json`, `search-index.json` et `extraction-report.json` sont générées. Les valeurs techniques viennent du code ou des JSON ; les explications et statuts viennent du Markdown/YAML.

## Classification des objets

Les modèles d’objets ne sont plus assimilés automatiquement à des objets actifs :

- `confirmed` : enregistrement littéral d’objet ou de bloc détecté ;
- `dynamic_probable` : pas d’enregistrement littéral, mais présence dans une recette MythicRPG ;
- `model_only` : modèle détecté sans preuve statique suffisante.

L’index public affiche les entrées confirmées par défaut.

## Site Astro

Le site statique fournit l’accueil, les skills, 180 perks, objets, recettes, recherche, filtres, provenance et une section Systèmes. La courbe globale se trouve dans `/systems/progression/`, et non dans Mining.

Le JavaScript navigateur est limité à la recherche, aux filtres et aux arbres. Les contenus dynamiques sont créés avec `createElement`, `textContent` et `replaceChildren`, sans affectation à `innerHTML`.

Les données françaises et anglaises sont préparées, mais les routes publiques de cette version restent françaises.

## GitHub Pages

Astro calcule le `base` depuis `GITHUB_REPOSITORY`. Le workflow utilise `npm ci` et publie `website/dist`. La génération d’un lockfile et le build réel restent bloqués dans l’environnement actuel ; voir `BUILD_VALIDATION.md`.
