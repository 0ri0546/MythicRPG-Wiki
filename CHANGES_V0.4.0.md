# Changements MythicRPG Wiki v0.4.0

## Documentation complète des neuf skills

La v0.4.0 termine la couverture approfondie de Traveling, Building, Farming et Woodcutting sans modifier l’architecture générale du site.

### Traveling

- extraction des gains de déplacement, dimensions, structures et trésors ;
- 22 montures, selles, perks, catégories et aliments de soin ;
- Traveler Boat, Traveler Minecart et relation Fishing Boat ;
- Grappling Hook, Monumental Compass, 22 modules et Death Recall ;
- règles serveur, persistance et multijoueur.

### Building

- 156 blocs admissibles répartis en trois valeurs d’XP ;
- règles anti-exploitation par position et matériau ;
- remplacement, restock, aimant, Plans 2D/3D ;
- portée, Compas d’architecte, Builder Wand et réserves ;
- 20 dalles verticales, 113 matériaux Blank Block et 32 effets statiques.

### Farming

- quatre catégories d’XP de récolte et élevage à 8 XP ;
- récolte de zone, protection de replantation et contrôles serveur ;
- chances et portées extraites des perks ;
- Champ vivant, compost, Graine/Fleur enchantées, Food Backpack et persistance.

### Woodcutting

- 2 XP par bûche et Timber limité à 32 blocs supplémentaires ;
- chances de double drop, Bois enchanté, pousses et Pomme dorée ;
- croissance, Hache enchantée et Wood Eater ;
- modules I/II/III, coffres simples/doubles, hoppers et sécurité transactionnelle.

## Technique

- ajout de `tools/mythicwiki/v040_extract.py` ;
- ajout de quatre composants Astro spécialisés ;
- extension du catalogue, de l’encyclopédie et de la recherche ;
- schéma et versions du projet passés à `0.4.0` ;
- tous les tests antérieurs conservés et tests v0.4.0 ajoutés ;
- aucune modification de `mod-source/src`.
