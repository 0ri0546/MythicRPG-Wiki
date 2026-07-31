---
id: crafting
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Transformez la valeur des ingrédients en XP Crafting, utilisez les stations portables, recyclez, transformez et manipulez la chance des Lucky Blocks."
key_systems:
  - "Craft Score et attribution d’XP Crafting"
  - "Fabrication portable, table vanilla et table infinie"
  - "Économie de ressources, renforcement, charges et maîtrise"
  - "Recyclage et transformations instantanées"
  - "Lucky Blocks, événements pondérés et infusion de chance"
xp_sources:
  - "Fabrications éligibles : score total des ingrédients converti en XP"
  - "Bonus contextuels appliqués dans l’ordre défini par le gestionnaire Crafting"
  - "Bonus de première fabrication, atelier de minuit et inspiration mythique lorsqu’ils sont débloqués"
multiplayer: "Le résultat, la consommation des ingrédients, le Craft Score, l’XP, les charges, les états persistants et les événements de Lucky Block sont décidés côté serveur. Les écrans ne sont que des vues synchronisées."
---
# Crafting

Crafting étend la fabrication vanilla avec une progression basée sur la valeur des ingrédients, plusieurs stations, des bonus de rendement et des systèmes de conversion. Son arbre complet relie fabrication portable, économie de ressources, renforcement, Lucky Blocks, recyclage, transformations et maîtrise.

## Craft Score et expérience

Chaque ingrédient explicitement reconnu possède un Craft Score. Les objets MythicRPG et les autres objets utilisent des valeurs de repli distinctes lorsque le code ne fournit pas de score spécifique. Le score total de la fabrication est converti en XP Crafting avec un multiplicateur et un plafond par action.

Les systèmes exclus du calcul sont listés séparément afin d’éviter les boucles de conversion, le spam décoratif et les gains provenant de mécaniques qui possèdent déjà leurs propres règles. Le calculateur de la page applique les bonus dans le même ordre que le gestionnaire Java.

## Arbre des perks

Les perks, leurs prérequis et leurs conflits sont extraits de l’arbre Java. Le simulateur permet de sélectionner un parcours et signale les parents absents ou les branches incompatibles. Les fiches individuelles relient les perks aux objets, recettes et systèmes associés.

## Stations et interfaces

Le système distingue la fabrication portable, la table de fabrication et la table infinie. Le catalogue précise l’identifiant interne de chaque station, sa durabilité lorsqu’elle est finie et l’organisation des propriétés ou emplacements synchronisés de l’interface.

La fabrication portable ouvre une grille complète depuis l’inventaire, mais le résultat reste calculé et validé par le serveur. Les charges, la durabilité et les états persistants ne sont donc pas de simples données clientes.

## Bonus de fabrication

Les perks peuvent économiser des ressources, renforcer le résultat, générer des charges, transférer une partie de la progression ou modifier l’XP obtenue. Toutes les valeurs affichées dans les cartes et calculateurs viennent du catalogue extrait ; elles ne sont pas écrites dans les composants Astro.

## Recyclage

Le recyclage convertit des groupes d’outils ou d’équipements compatibles en une ressource de base. Les groupes, les entrées et les résultats sont extraits du gestionnaire dédié. Cette action possède son propre prérequis et ne doit pas être confondue avec une recette vanilla classique ni avec une source automatique d’XP Crafting.

## Transformations

Le système de transformation relie des paires d’objets et consomme la charge définie par élément transformé. La page fournit une recherche sur toutes les paires extraites afin de répondre rapidement à « que devient cet objet ? » et « comment obtenir ce résultat ? ».

## Lucky Blocks

Le Bloc chanceux utilise une valeur interne bornée. Cette valeur répartit d’abord le tirage entre catégories positive, neutre et négative, puis un événement est sélectionné selon son poids dans la catégorie. Le simulateur affiche cette répartition et le catalogue liste les événements pondérés réellement présents dans le code.

L’infusion utilise les ingrédients requis autour du Bloc chanceux pour modifier sa valeur interne. Les groupes d’objets et les variations appliquées sont extraits du registre des recettes d’infusion.

## Solo et multijoueur

Le serveur valide le résultat de craft, la consommation, l’XP, les changements de durabilité, les charges, les transformations et les événements de Lucky Block. En multijoueur, les interfaces portables synchronisent cet état ; elles ne décident pas localement du résultat.

Cette page décrit l’état de `src(92)` par inspection statique. Elle ne prétend pas avoir exécuté les interfaces, cassé un Lucky Block ou testé la concurrence de plusieurs joueurs.
