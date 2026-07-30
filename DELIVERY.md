# Livraison MythicRPG Wiki v0.1.1 — état de fiabilisation

## Modifications réalisées

- traçabilité distinguant l’archive reçue `src(92).zip` de la source canonique `src(91).zip` ;
- snapshot versionné séparé des tests d’invariants ;
- page `/systems/progression/` et retrait des valeurs globales de Mining ;
- classification `confirmed`, `dynamic_probable`, `model_only` des modèles d’objets ;
- filtre public affichant les objets confirmés par défaut ;
- suppression des affectations à `innerHTML` dans la recherche et les arbres ;
- workflow et script de build configurés avec `npm ci` ;
- documentation corrigée : données bilingues préparées, interface encore française ;
- tests divisés entre invariants et snapshot de version.

## Code du mod

Aucun fichier de `mod-source/src/` n’a été modifié. Gradle et Minecraft n’ont pas été lancés.

## Point non terminé

Le registre npm de l’environnement retourne 404 pour Astro. Il n’a donc pas été possible de générer honnêtement `website/package-lock.json`, d’exécuter `npm ci` ou de valider le build Astro. La livraison est une base de fiabilisation complète côté code, mais le verrou npm et le build restent à confirmer. Voir `BUILD_VALIDATION.md`.
