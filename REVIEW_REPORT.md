# Rapport de seconde relecture — v0.2

## Contrôles refaits

- empreinte de `mod-source/src` inchangée ;
- génération complète sans erreur ni avertissement ;
- 9 skills, 180 perks, 196 modèles, 34 blocs et 188 recettes JSON ;
- 25 valeurs suivies et 634 entrées de recherche ;
- 5 familles/5 raretés fossiles ;
- 47 recettes culinaires et 50 sources d’ingrédients ;
- 5 familles/5 raretés Fishing et 3 monstres marins ;
- contenu structuré présent en français et anglais pour les neuf skills ;
- composants interactifs sans `innerHTML` ;
- source et snapshot alignés sur `src(92)` ;
- aucune commande Gradle ou Minecraft.

## Résultat

Les 28 tests Python passent. Le catalogue est propre et les données générées ne contiennent aucun chemin local.

## Limite

Le miroir npm de cet environnement ne permet pas de restaurer les dépendances Linux nécessaires au build Astro. Cette limite est documentée sans réutiliser l’ancien `dist` comme preuve de build v0.2.
