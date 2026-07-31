---
id: fighting
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Develop combat damage, survival, loot and experience, then face the complete Baron catalogue and their legendary rewards."
key_systems:
  - "Melee damage, reach, attack cadence and on-hit effects"
  - "Resistance, regeneration and permanent protections"
  - "Loot, Fighting experience and vanilla experience"
  - "Baron promotion, scaling and behaviours"
  - "Special rewards and legendary items"
xp_sources:
  - "Death of a living entity credited to a player: gain calculated from maximum health"
  - "Death of a Baron: strengthened base reward multiplied by its spawn level"
  - "Additional perk rewards and bonuses are applied server-side"
multiplayer: "Baron promotion, spawn level, abilities, damage and rewards are server-authoritative. Nearby-player protection prevents an advanced player from spawning a Baron next to a player who is still below the configured threshold."
---
# Fighting

Fighting improves combat without forcing a specific class. Its complete perk tree covers direct attacks, effects applied to enemies, defence, rewards and encounters with Barons.

## Earning experience

The Fighting manager grants experience when a living entity dies with a player identified as its attacker. Normal gain is calculated from the entity's maximum health, using a minimum and a cap extracted from the code. A Baron uses its base health before attribute scaling, then receives a Baron-specific bonus and a reward multiplier based on its spawn level.

The calculator on this page reproduces these formulas from the generated catalogue. It does not simulate Minecraft combat and is not an in-game validation.

## Perk tree

All perks, positions, parents and branch conflicts are read from the Java tree. The simulator checks a route, its total cost and missing prerequisites. Branches cover damage, attack speed, reach, poison, life steal, resistance, loot, experience and permanent effects.

## Baron system

An eligible entity may be promoted when it appears. The server finds a reference player, uses that player's Fighting level for the promotion chance and stores the Baron's spawn level. A second selection decides whether the Baron remains normal or receives a special behaviour compatible with its base entity type.

The page presents shared conditions, chance tiers, health, damage and experience multipliers, and the complete type catalogue. Each entry lists compatible entities, the extracted behaviour, its constants when available and associated rewards.

## Rewards and legendary items

Rewards are resolved server-side when the Baron dies. Some are guaranteed, while others use a probability or a special condition. MythicRPG rewards link to their item pages while retaining their registration-evidence status in the general catalogue.

Legendary items are documented from their specialised classes. Technical constants are displayed when they can be extracted deterministically. A missing value does not imply that the item has no effect; it only means the mechanic is not represented by a simple exported constant.

## Solo and multiplayer

In single-player, the local player naturally acts as the reference player. In multiplayer, the server selects the nearby player used for the spawn level and applies protection for less advanced Fighting players in the configured area. Special attacks, tags, bossbars, damage and rewards remain server decisions.

This documentation describes the state visible through static inspection of `src(92)`. It does not claim to validate real encounter frequency, perceived balance or every ability in game.
