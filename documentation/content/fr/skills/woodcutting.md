---
id: woodcutting
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Coupez les bûches, abattez des arbres, obtenez des récompenses rares et agrandissez les coffres vanilla avec des modules persistants."
key_systems:
  - "2 XP par bûche du tag minecraft:logs"
  - "Timber limité à 32 bûches supplémentaires"
  - "Bois enchanté, pousses, pommes et doubles drops"
  - "Hache enchantée et consommation de bois"
  - "Modules de coffre I, II et III jusqu’à 108 emplacements"
xp_sources:
  - "Cassure serveur de blocs appartenant au tag minecraft:logs"
  - "Chaque bûche supplémentaire cassée par Timber utilise la même valeur"
multiplayer: "Les cassures, Timber, projectiles et inventaires modulaires sont autoritaires côté serveur. Les changements de modules utilisent une opération transactionnelle pour éviter perte et duplication."
---
# Bûcheronnage

Woodcutting progresse sur les bûches reconnues par le tag vanilla. Les feuilles ne donnent pas directement d’XP de skill. L’arbre développe les quantités récoltées, les ressources rares, la croissance des arbres, l’abattage et le stockage.

## XP et Timber

Chaque bûche validée donne la valeur extraite du code. Timber exige une hache et recherche des blocs du même type, jusqu’à 32 bûches supplémentaires. Chaque bloc secondaire attribue sa propre XP. La recherche et les cassures sont effectuées côté serveur.

## Butins supplémentaires

Les trois paliers de double drop, les quatre chances de Bois enchanté, la pousse aléatoire et la Pomme dorée sont lus dans l’arbre des perks. Le pool de pousses comprend les essences vanilla reconnues par le gestionnaire. Silk Touch désactive les récompenses supplémentaires issues des feuilles afin de conserver un comportement cohérent avec la collecte du bloc lui-même.

## Croissance des arbres

Le perk dédié détecte une transition en position accroupie, applique un cooldown, recherche dans un rayon et limite le nombre de pousses déclenchées. Il accélère la croissance sans attribuer directement l’XP de coupe.

## Hache enchantée et Wood Eater

La Hache enchantée agit depuis la main secondaire sur les flèches, boules de neige et œufs. Les projectiles dupliqués ne peuvent être ramassés qu’en créatif, et la hache paie un coût de durabilité. Wood Eater consomme des objets du tag des bûches pour restaurer faim et saturation lorsque le joueur peut manger.

## Modules de coffre

Les modules I, II et III ajoutent respectivement des emplacements à un coffre vanilla. Les capacités sont calculées pour les coffres simples et doubles. Les hoppers ne voient que la capacité active ; les emplacements de modules ne sont pas automatisables. Retirer ou réduire un module ne réussit que si tous les objets tiennent dans la nouvelle capacité, puis le contenu est réorganisé atomiquement.

Les données de module et le stockage supplémentaire sont persistants par coffre physique. Les doubles coffres, viewers et états de couvercle sont gérés côté serveur. Ces garanties sont destinées à éviter les pertes, duplications et divergences multijoueur.
