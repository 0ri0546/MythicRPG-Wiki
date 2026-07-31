---
id: crafting
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Turn ingredient value into Crafting XP, use portable stations, recycle, transform and manipulate Lucky Block luck."
key_systems:
  - "Craft Score and Crafting XP attribution"
  - "Portable crafting, vanilla table and infinite table"
  - "Resource saving, reinforcement, charges and mastery"
  - "Recycling and instant transformations"
  - "Lucky Blocks, weighted events and luck infusion"
xp_sources:
  - "Eligible crafts: total ingredient score converted into XP"
  - "Context bonuses applied in the order defined by the Crafting manager"
  - "First-craft, Midnight Workshop and Mythic Inspiration bonuses when unlocked"
multiplayer: "Result creation, ingredient consumption, Craft Score, XP, charges, persistent states and Lucky Block events are server decisions. Screens are synchronised views of that state."
---
# Crafting

Crafting extends vanilla recipes with ingredient-value progression, several stations, yield bonuses and conversion systems. Its complete perk tree connects portable crafting, resource saving, reinforcement, Lucky Blocks, recycling, transformations and mastery.

## Craft Score and experience

Each explicitly recognised ingredient has a Craft Score. MythicRPG items and other items use separate fallback values when the code has no specific score. The total craft score is converted into Crafting XP using an extracted multiplier and a per-action cap.

Excluded systems are listed separately to prevent conversion loops, decorative spam and gains from mechanics that already have their own rules. The page calculator applies bonuses in the same order as the Java manager.

## Perk tree

All perks, prerequisites and conflicts are extracted from the Java tree. The simulator lets readers select a route and reports missing parents or incompatible branches. Individual perk pages link to associated items, recipes and systems.

## Stations and interfaces

The system distinguishes portable crafting, the crafting table and the infinite crafting table. The catalogue records each internal station identifier, finite durability where applicable and the layout of synchronised properties and slots.

Portable crafting opens a complete grid from the inventory, but the result is still calculated and validated by the server. Charges, durability and persistent states are therefore not client-only data.

## Crafting bonuses

Perks can save resources, reinforce results, generate charges, transfer part of progression or modify earned XP. Every value shown in cards and calculators comes from the extracted catalogue rather than being written in Astro components.

## Recycling

Recycling converts compatible groups of tools or equipment into a base resource. Groups, inputs and outputs are extracted from the dedicated manager. This action has its own prerequisite and must not be confused with a vanilla recipe or an automatic Crafting XP source.

## Transformations

The transformation system connects item pairs and consumes the extracted charge amount per transformed item. The page provides a search over every extracted pair to answer both “what does this item become?” and “how can I obtain this result?”.

## Lucky Blocks

A Lucky Block stores a bounded internal luck value. It first distributes the roll between positive, neutral and negative categories, then selects a weighted event inside the chosen category. The simulator displays this distribution and the catalogue lists the weighted events actually present in the code.

Infusion surrounds the Lucky Block with the required identical ingredients to modify its internal value. Ingredient groups and applied changes are extracted from the infusion recipe registry.

## Solo and multiplayer

The server validates craft results, consumption, XP, durability changes, charges, transformations and Lucky Block events. In multiplayer, portable interfaces synchronise this state rather than deciding the result locally.

This page describes `src(92)` through static inspection. It does not claim to have executed the interfaces, broken a Lucky Block or tested concurrent players.
