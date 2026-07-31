# Livraison MythicRPG Wiki v0.4.1

La livraison contient la source complète du wiki, prête pour le déployeur automatique. Elle conserve l’architecture, le workflow GitHub Pages, `package-lock.json`, les scripts multiplateformes et la règle `mod-source/src/** -text`.

## Contenu correctif

- champs numériques modifiables directement sans réinjection immédiate de la valeur minimale ;
- 188 recettes dotées d’une représentation visuelle ;
- 180 perks dotés d’une icône ou d’un fallback centralisé ;
- nodes de perks entièrement cliquables ;
- checkboxes alignées par un style partagé ;
- filtre Eating corrigé pour les ingrédients traduits et normalisés.

## Résultats du catalogue

- 9 skills et 180 perks ;
- 196 objets ou modèles, 34 blocs et 188 recettes JSON ;
- 129 recettes shaped et 59 shapeless ;
- 95 mappings spécifiques de perks et 85 fallbacks de skill ;
- 134 textures de perks rendables et 46 fallbacks graphiques ;
- 50 valeurs documentées et 752 entrées de recherche ;
- 0 erreur et 0 avertissement d’extraction.

## Validation

- génération du catalogue réussie ;
- 59 tests Python réussis ;
- vérification de livraison statique réussie ;
- 401 pages statiques attendues d’après la topologie des routes ;
- build Astro non validé dans cet environnement à cause du miroir npm interne ;
- aucun ancien `website/dist` n’est inclus.

## Source

- source canonique : `src(92)` ;
- dossier : `mod-source/src` ;
- inspection : statique uniquement ;
- Gradle : non exécuté ;
- Minecraft : non exécuté ;
- modification du mod : aucune.

## Exclusions de l’archive

`.git`, `node_modules`, `website/dist`, `.astro`, caches Python, fichiers temporaires et secrets sont exclus.
