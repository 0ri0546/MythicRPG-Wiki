---
id: building
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Build more efficiently with plans, reserves, placement tools and server-controlled decorative systems."
key_systems:
  - "156 XP blocks worth 3, 4 or 5 XP"
  - "Position and material anti-exploit tracking"
  - "2D Plans 8×8/12×12 and 3D Plans 8×8×8"
  - "Construction Reserve, reach and Architect Compass"
  - "20 vertical slabs, 113 blank materials and 32 static effects"
xp_sources:
  - "Placing blocks recognized by BuildingBlockCatalog"
  - "Vertical slabs with their dedicated value"
  - "Custom blocks explicitly registered in the Building catalog"
multiplayer: "XP, replacements, plans, reserves, miniatures and decorations are server-authoritative. Previews and interfaces are client-facing but do not decide final placement."
---
# Building

Building improves project preparation and execution while preserving free vanilla construction. Automated operations consume player resources and remain server checked.

## XP and anti-exploit protection

The catalog groups 156 vanilla blocks into three XP tiers. Unknown blocks grant no automatic XP unless explicitly registered. Bounded position and material histories reduce repeated placement rewards and recover through varied building activity.

## Placement comfort

Quick Replace checks both blocks, refuses block entities and recovers the old block outside Creative. Auto Restock matches item components. Decorative Magnet processes a limited number of nearby items at intervals and is applied server-side.

## 2D and 3D Plans

2D Plans have base and upgraded sizes, per-job and global tick limits. 3D Plans have a separate maximum volume. Previews expire; real resources and placements are server validated.

## Tools and reserves

Reach perks extend placement distance. Architect Compass stores its radius on the item. Builder Wand has finite durability and copies only safe state properties. Construction Reserves are persistent, player-limited and use increasing ranges for perks 15–17.

## Decoration and miniaturization

Vertical slabs, Blank Block, miniatures and Static Decoration Generator are registry-driven. Decorations are owner protected with a Creative override; miniatures can be rotated and retrieved by their owner.

## Multiplayer

All block-changing and resource-consuming operations are server decisions. Clients receive previews and UI state only.
