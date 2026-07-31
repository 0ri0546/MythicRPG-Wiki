# Livraison MythicRPG Wiki v0.4.0

La livraison contient la source complète du wiki, prête pour le déployeur automatique. Elle conserve l’architecture, le workflow GitHub Pages, `package-lock.json`, les scripts multiplateformes et la règle `mod-source/src/** -text`.

## Contenu fonctionnel

Les neuf skills disposent d’une couverture approfondie. Traveling, Building, Farming et Woodcutting utilisent désormais des extracteurs Java spécialisés et des composants Astro dédiés, au même titre que les cinq skills approfondis auparavant.

## Résultats du catalogue

- 9 skills et 180 perks ;
- 196 objets ou modèles, 34 blocs et 188 recettes JSON ;
- 50 valeurs documentées et 752 entrées de recherche ;
- 22 montures et 22 modules Traveling ;
- 156 blocs d’XP, 20 dalles verticales et 32 effets Building ;
- 4 catégories de récolte et 16 familles de blocs Champ vivant Farming ;
- 3 modules de coffre Woodcutting ;
- 0 erreur et 0 avertissement d’extraction.

## Validation

- 50 tests Python réussis ;
- vérification de livraison statique réussie ;
- 401 pages attendues d’après la topologie des routes ;
- build Astro non exécuté ici à cause d’un paquet absent du miroir npm interne.

## Source

- source canonique : `src(92)` ;
- dossier : `mod-source/src` ;
- inspection : statique uniquement ;
- Gradle : non exécuté ;
- Minecraft : non exécuté ;
- modification du mod : aucune.

## Exclusions de l’archive

`.git`, `node_modules`, `website/dist`, `.astro`, caches Python, fichiers temporaires et secrets sont exclus.
