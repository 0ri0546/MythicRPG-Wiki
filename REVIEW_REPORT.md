# Rapport de seconde relecture — v0.1.1

Date : 30 juillet 2026.

## Contrôles refaits

- empreinte de `mod-source/src` identique au snapshot ;
- nouvelle génération complète du catalogue ;
- 9 skills et 180 perks ;
- 196 modèles classés en 93 confirmés, 40 dynamiques probables et 63 modèles seuls ;
- 34 blockstates, 188 recettes, 11 valeurs Java suivies et 574 entrées de recherche ;
- 0 erreur et 0 avertissement d’extraction ;
- vérification des relations internes et de la symétrie des traductions ;
- contrôle qu’aucune affectation à `innerHTML` ne reste dans le code du site ;
- contrôle que la progression globale n’est plus rattachée à Mining ;
- contrôle de l’absence de commandes Gradle ou Minecraft ;
- contrôle UTF-8, JSON, YAML et syntaxe Python.

## Limite indépendante confirmée

Le registre npm disponible répond 404 pour Astro et empêche la génération du lockfile et le build. Cette limite est laissée visible : aucun résultat de build n’est affirmé et aucun lockfile artificiel n’est fourni.
