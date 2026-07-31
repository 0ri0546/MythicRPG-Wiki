# Architecture MythicRPG Wiki v0.4.1

La v0.4.1 conserve l’architecture validée : extracteur Python, catalogue JSON commun, site Astro statique et export filtré pour la future encyclopédie.

## Sources de vérité

- `mod-source/src` : valeurs, registres, recettes, traductions et assets du mod ;
- `documentation/content` : contenu éditorial ;
- `config/documented_values.yaml` : constantes Java explicitement suivies ;
- `config/perk_icons.yaml` : mapping maintenable des icônes de perks ;
- `config/recipe_tag_variants.yaml` : variantes représentatives des tags externes au namespace du mod.

## Données générées

L’extracteur construit :

- les neuf skills et leurs 180 perks ;
- objets, blocs et recettes ;
- données spécialisées des systèmes ;
- informations visuelles des 188 recettes ;
- provenance et mapping des 180 icônes de perks ;
- index de recherche et export encyclopédie.

Le schéma du catalogue est `0.4.1`.

## Recettes visuelles

Les recettes shaped conservent le motif et sont normalisées vers neuf slots. Les recettes shapeless conservent une liste sans signification positionnelle. Chaque ingrédient possède un descripteur item ou tag, une variante représentative, les variantes connues et une indication de complétude.

Le composant Astro `RecipeVisual.astro` rend ces données sans bibliothèque graphique et sans JavaScript client.

## Icônes des perks

L’extracteur applique les règles de `perk_icons.yaml` une seule fois. Les pages consomment ensuite l’icône normalisée du catalogue. Une texture du projet est utilisée si elle existe ; sinon l’arbre affiche un glyphe de skill cohérent.

## JavaScript ciblé

Le JavaScript navigateur reste limité aux interactions utiles : recherche, filtres, arbres et calculateurs. La v0.4.1 ajoute deux utilitaires partagés :

- `number-input.ts` pour la validation différée des champs numériques ;
- `search-normalize.ts` pour les recherches insensibles aux accents, à la casse et aux espaces.

## Déploiement

Astro produit des fichiers statiques compatibles GitHub Pages. `package-lock.json`, `npm ci`, le workflow GitHub Actions et les scripts multiplateformes sont conservés.
