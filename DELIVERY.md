# Livraison MythicRPG Wiki v0.2

## Périmètre livré

- couverture structurée des neuf skills ;
- 180 arbres/fiches de perks ;
- courbe d’XP interactive et calculateurs ;
- exploration Mining : Vein Mining et fossiles ;
- exploration Eating : XP, ingrédients et 47 recettes culinaires ;
- exploration Fishing : appâts, raretés, dimensions, météo, jauges et monstres ;
- filtres de recettes et contenus associés ;
- 25 constantes Java documentées ;
- recherche enrichie à 634 entrées ;
- 28 tests Python.

## Mod

Aucun fichier de `mod-source/src/` n’a été modifié. Gradle et Minecraft n’ont pas été lancés.

## Build dans l’environnement de livraison

Le lockfile fourni est conservé. Le registre npm interne de l’environnement de génération ne contient pas toutes les archives nécessaires à `npm ci`, notamment `zwitch` et le binding Linux du compilateur Astro. Le build v0.2 n’est donc pas affirmé comme exécuté ici. Le même lockfile et Astro 7.1.4 sont déjà validés dans l’environnement Windows/GitHub du projet ; la commande de référence reste `python scripts/build_all.py`.
