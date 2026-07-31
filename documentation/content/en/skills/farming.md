---
id: farming
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Progress through mature harvests and breeding, then develop replanting, growth and agricultural storage."
key_systems:
  - "Four harvest categories worth 2–4 XP"
  - "Breeding confirmed by a matching newborn"
  - "Area harvest limited to 96 blocks"
  - "Living Field, compost and growth bonuses"
  - "Enchanted Seed, Enchanted Flower and Food Backpack"
xp_sources:
  - "Manual harvesting of mature crops"
  - "Supported mushroom blocks, melons and pumpkins"
  - "Animal breeding confirmed by the newborn"
multiplayer: "Break events, breeding matching, growth and Food Backpack inventories are server controlled. Pending breeding actions are runtime-only session data."
---
# Farming

Farming rewards completed agricultural actions. Harvest XP requires maturity, while breeding XP is granted only after a matching newborn is detected.

## Harvest and XP

Standard crops, Nether Wart and cocoa, mushroom blocks, melons and pumpkins use distinct extracted values. Recently replanted crops are briefly protected, and automatic perk growth does not directly grant XP.

## Area harvest

Reach perks extend hoe radius. Area harvesting requires a hoe and mature supported blocks, stops at the extracted maximum and uses a reentrancy guard.

## Breeding

Valid parents are remembered temporarily. The server matches a newborn by type, world, radius and timing before granting XP.

## Growth and rewards

Living Field performs controlled growth attempts around the player across several vanilla plant families. Compost rolls among fixed resources, plantables and vanilla XP.

## Items and persistence

Enchanted Seed chances scale with perks. Enchanted Flower supports offhand smelting and Food Backpack integration. The 54-slot backpack rejects nesting and integrates with Eating preservation. Preserved Farmer restores vanilla XP and backpacks after player copy on death.

## Interactions

Growth Totem originates in Mining archaeology but affects crops. Stored ingredients can feed Eating systems; these are optional cross-skill links.
