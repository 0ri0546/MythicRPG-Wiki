# Livraison MythicRPG Wiki v0.3.0

## Périmètre

Cette version rend Fighting et Crafting entièrement documentés selon la structure déjà utilisée par Mining, Eating et Fishing. L’architecture générale, GitHub Pages, le lockfile, le lanceur automatique et la source `src(92)` sont conservés.

## Résultats d’extraction

- 9 skills et 180 perks ;
- 25 types de Barons ;
- 5 objets légendaires Fighting ;
- 55 valeurs explicites de Craft Score ;
- 48 transformations ;
- 4 groupes de recyclage ;
- 21 événements Lucky Block ;
- 50 valeurs Java explicitement documentées ;
- 685 entrées de recherche ;
- 0 erreur et 0 avertissement.

## Validation

- 38 tests Python réussis ;
- génération statique des données réussie ;
- scripts navigateur vérifiés syntaxiquement ;
- source du mod inchangée ;
- aucun Gradle, aucune compilation Java du mod et aucun lancement Minecraft.

Le build Astro doit être relancé par le déployeur automatique ou dans l’environnement Windows validé, car le binding Linux optionnel n’est pas présent dans le `node_modules` fourni et le registre npm est inaccessible ici.

## Rapport de fichiers

La comparaison exacte avec la v0.2.1 est fournie dans `FILES_CREATED_MODIFIED_V0.3.0.md`. Le manifeste `FILE_MANIFEST.txt` référence chaque fichier livré avec sa taille et son SHA-256, hors exclusions explicitement documentées.
