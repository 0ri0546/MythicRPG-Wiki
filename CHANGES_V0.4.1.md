# Changements MythicRPG Wiki v0.4.1

## Périmètre

La v0.4.1 est une mise à jour corrective d’ergonomie. Elle conserve l’architecture Python + catalogue JSON + Astro, la source `src(92)`, le workflow GitHub Pages et les neuf pages de skills approfondies.

## Calculateurs numériques

- ajout d’un utilitaire partagé `website/src/scripts/number-input.ts` ;
- une valeur vide ou invalide reste temporairement vide pendant la frappe ;
- aucune valeur minimale n’est réinjectée pendant l’événement `input` ;
- validation et bornage au `blur` ;
- protection contre `NaN`, `Infinity`, décimales interdites et valeurs hors limites ;
- correction appliquée aux calculateurs Progression, Eating, Fighting et Crafting.

## Recettes visuelles

- ajout du composant partagé `RecipeVisual.astro` ;
- 188 recettes converties : 129 shaped et 59 shapeless ;
- conservation exacte du motif shaped dans une grille 3 × 3 ;
- résultat, quantité, flèche, ingrédients et quantités affichés ;
- recettes shapeless explicitement marquées « Recette sans forme » ;
- ingrédients par tag accompagnés d’une variante représentative et d’une liste accessible des variantes connues ;
- textures extraites du projet utilisées lorsqu’elles existent ;
- fallback textuel accessible lorsque la texture n’est pas exploitable ;
- données textuelles et provenance conservées sous la représentation.

## Icônes des perks

- ajout du mapping central `config/perk_icons.yaml` ;
- les 180 perks possèdent une association visuelle ;
- 95 associations spécifiques et 85 associations de fallback de skill ;
- 134 perks disposent d’une texture réellement rendue ;
- 46 perks utilisent un glyphe de skill lorsque la texture associée n’est pas disponible ;
- aucune image n’est dupliquée en base64 dans le catalogue.

## Arbres de perks

- la zone cliquable reste le bouton complet ;
- l’image, le numéro et les éléments décoratifs utilisent `pointer-events: none` ;
- les lignes SVG ne capturent plus les clics ;
- le déplacement de l’arbre ignore tout clic provenant d’un descendant du node via `closest('[data-node]')` ;
- focus clavier visible et activation native avec Entrée/Espace.

## Formulaires et filtres

- alignement global des checkboxes avec `.form-choice` en `inline-flex` et `align-items: center` ;
- labels complets cliquables et focus visible ;
- ajout de `search-normalize.ts` pour normaliser casse, accents et espaces ;
- recherche Eating étendue aux identifiants et noms français/anglais des ingrédients ;
- recherche des recettes Eating fondée sur les ingrédients réels, pas seulement le titre du plat ;
- boutons de réinitialisation et états « aucun résultat » ajoutés ;
- aucune utilisation de `innerHTML`.

## Extraction et catalogue

- les recettes conservent désormais leur motif, leurs slots et leur nature shaped/shapeless ;
- ajout de `config/recipe_tag_variants.yaml` pour les variantes vanilla non présentes dans les tags du mod ;
- provenance des icônes et des variantes conservée ;
- schéma et versions du projet alignés sur `0.4.1`.

## Validation

- génération du catalogue réussie ;
- 59 tests Python réussis ;
- vérification statique et intégrité de `src(92)` ;
- `npm ci` tenté, mais bloqué par une erreur 404 du miroir npm interne sur `zwitch@2.0.4` ;
- tentative via le registre public restée sans réponse dans l’environnement ;
- aucun build Astro n’est présenté comme réussi ici.
