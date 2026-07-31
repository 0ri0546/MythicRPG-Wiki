---
id: building
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Construisez plus efficacement avec des plans, réserves, outils de placement et systèmes décoratifs contrôlés côté serveur."
key_systems:
  - "Catalogue de 156 blocs donnant 3, 4 ou 5 XP"
  - "Anti-exploitation par position et matériau"
  - "Plans 2D 8×8/12×12 et Plans 3D 8×8×8"
  - "Réserve de chantier, portée et Compas d’architecte"
  - "20 dalles verticales, 113 matériaux vierges et 32 effets statiques"
xp_sources:
  - "Placement de blocs reconnus par BuildingBlockCatalog"
  - "Dalles verticales enregistrées avec leur propre valeur"
  - "Blocs custom ajoutés explicitement au catalogue Building"
multiplayer: "L’XP, les remplacements, plans, réserves, miniatures et décorations sont autoritaires côté serveur. Les aperçus et interfaces utilisent des données clientes sans décider du placement final."
---
# Construction

Building améliore la préparation et l’exécution d’un projet tout en laissant la construction vanilla entièrement possible. Les systèmes automatisent des opérations répétitives, mais consomment les ressources du joueur et restent soumis aux contrôles serveur.

## XP et protection anti-exploitation

Le catalogue Building regroupe 156 blocs vanilla en trois niveaux de récompense. Les blocs inconnus ne donnent pas automatiquement d’XP, sauf s’ils sont enregistrés explicitement par un système custom. Le gestionnaire conserve un historique borné des positions et matériaux utilisés. Replacer rapidement au même endroit réduit fortement le gain, tandis qu’utiliser excessivement le même matériau applique un multiplicateur qui récupère progressivement avec la diversité des placements.

## Confort de placement

Le remplacement rapide exige un ancien et un nouveau bloc admissibles, refuse les block entities et restitue l’ancien bloc hors mode créatif. Le restock automatique cherche une pile compatible avec l’objet et ses composants. L’aimant décoratif attire périodiquement un nombre limité d’objets proches et peut être désactivé dans les préférences client, mais son application reste serveur.

## Plans 2D et 3D

Les Plans 2D utilisent une taille de base puis une taille améliorée. Les travaux sont découpés en tâches et soumis à une limite globale de blocs par tick. Le Plan 3D possède son propre volume maximal. Les aperçus ont une durée limitée et sont visuels ; les ressources, positions et placements réels sont vérifiés côté serveur.

## Outils et réserves

Les perks de portée augmentent la distance de construction. Le Compas d’architecte conserve son rayon dans les données de l’objet. La Builder Wand possède une durabilité finie et copie uniquement des propriétés de bloc considérées comme sûres. Les Réserves de chantier sont persistantes, limitées par joueur et utilisent des rayons croissants selon les perks 15 à 17.

## Décoration et miniaturisation

La branche décorative regroupe les dalles verticales, le Blank Block, les projets miniatures et le Static Decoration Generator. Les matériaux et effets disponibles sont extraits de leurs registres. Les décorations statiques sont protégées par propriétaire avec une exception créative. Les miniatures sont contrôlées par leur propriétaire, peuvent être tournées et récupérées.

## Solo et multijoueur

Les opérations capables de poser, remplacer ou consommer des blocs sont décidées par le serveur. Les tâches persistantes et réserves sont propres aux joueurs ou au monde. Les clients reçoivent les aperçus et états d’interface nécessaires, sans autorité sur le résultat final.
