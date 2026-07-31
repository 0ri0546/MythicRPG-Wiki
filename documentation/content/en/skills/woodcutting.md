---
id: woodcutting
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Cut logs, fell trees, obtain rare rewards and expand vanilla chests with persistent modules."
key_systems:
  - "2 XP per minecraft:logs block"
  - "Timber limited to 32 additional logs"
  - "Enchanted Wood, saplings, apples and double drops"
  - "Enchanted Axe and wood consumption"
  - "Chest Modules I–III up to 108 slots"
xp_sources:
  - "Server break events for blocks in minecraft:logs"
  - "Each additional Timber log uses the same XP value"
multiplayer: "Breaks, Timber, projectiles and modular inventories are server-authoritative. Module changes are transactional to prevent loss or duplication."
---
# Woodcutting

Woodcutting progresses on vanilla log-tag blocks. Leaves do not directly grant skill XP. The tree develops yield, rare resources, growth, felling and storage.

## XP and Timber

Every valid log grants the extracted value. Timber requires an axe and searches same-type blocks, up to 32 additional logs, each with its own XP.

## Extra drops

Double-drop tiers, Enchanted Wood chances, random saplings and Golden Apple chance come from the perk tree. Silk Touch disables extra leaf rewards.

## Tree growth

The growth perk detects a sneak transition, applies a cooldown, searches a radius and limits triggered saplings. Growth itself grants no cutting XP.

## Enchanted Axe and Wood Eater

Enchanted Axe affects arrows, snowballs and eggs from the offhand, charges durability and marks duplicates Creative-only pickup. Wood Eater consumes log-tag items for hunger and saturation.

## Chest Modules

Modules I–III expand vanilla chest capacity, including double chests. Hoppers only access active capacity and module slots are not automatable. Shrinking succeeds only when every item fits, followed by an atomic repack. Module and extra storage data are persistent per physical chest and server-managed.
