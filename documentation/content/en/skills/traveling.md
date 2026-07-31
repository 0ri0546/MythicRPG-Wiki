---
id: traveling
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Explore, discover structures, adopt mounts and unlock specialized travel tools."
key_systems:
  - "Movement, dimensions, structures and treasures"
  - "22 land or flying mounts and their saddles"
  - "Traveler Boat, Traveler Minecart and Fishing Boat"
  - "Monumental Compass and 22 structure modules"
  - "Grappling Hook, death recall, miniaturization and double jump"
xp_sources:
  - "Distance travelled with teleport protection"
  - "First visits to dimensions"
  - "Discovery of configured structures"
  - "Opening treasure chests"
  - "Vehicle movement tracked from the player position"
multiplayer: "XP, discoveries, mounts, compass searches, grappling and death recall are server validated. Client payloads mainly carry input or visual information."
---
# Traveling

Traveling rewards exploration without prescribing a route. Progress comes from real movement, first dimension visits, structure discoveries and treasure. Distance thresholds, rewards and teleport protection are read directly from `src(92)`.

## Movement and discoveries

The manager accumulates travelled distance and also checks direct displacement before granting XP. A very large one-tick movement is treated as a teleport. Dimension and structure discoveries are persistent per player.

## Personal mobility

Double jump is client input validated by the server. Miniaturization applies a periodically checked scale modifier. Soul, dolphin, powder snow and biome-speed perks improve movement while retaining vanilla rules.

## Mounts and vehicles

The catalog lists land and flying mounts, saddles, required perks and healing items. Compatible creatures must be weakened before adoption. Ownership and anchor data are server-persistent.

Traveler Boat and Traveler Minecart use multipliers extracted from their entities. Fishing Boat remains an intentional Traveling XP source because movement tracking follows player position and contains no general vehicle exclusion in the inspected source.

## Exploration tools

The Monumental Compass searches structures with a normal or module-extended radius. Its 22 modules are grouped by realm. Death Recall is owner-bound and searches a safe position around the stored death location. Grappling range, pull speed, arrival and fall protection are server controlled.

## End-tree choice

Flying mounts and Grappling Hook are exclusive final branches. The common tree preserves real positions, parents and fork identifiers.

## Validation

Values and relations are statically inspected. Actual feel, network stability and balance remain in-game validation topics.
