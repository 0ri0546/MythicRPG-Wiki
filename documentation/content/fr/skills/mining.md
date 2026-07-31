---
id: mining
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Progressez en minant, automatisez les filons et explorez un système complet d’archéologie fossile."
key_systems:
  - "Vein Mining et Minage 3×3"
  - "Cinq familles et cinq raretés de fossiles"
  - "Nettoyage, incubation, analyse et expéditions"
  - "Bonus de butin, d’XP, de célérité et de confort"
xp_sources:
  - "Casser les blocs reconnus par le gestionnaire Mining"
  - "Les blocs supplémentaires de Vein Mining et du 3×3 réutilisent les règles d’XP du bloc ciblé"
multiplayer: "Les effets de zone et les toggles sont contrôlés côté serveur. Vein Mining conserve une préférence cliente synchronisée à chaque connexion."
---
# Minage

Mining associe une progression classique par extraction de blocs à une branche d’archéologie beaucoup plus large. L’arbre commence par **Filon minier**, puis se divise entre fossiles, surbrillance, butin, expérience et avantages permanents.

## Deux effets de zone indépendants

**Vein Mining** recherche les blocs identiques connectés dans les six directions et casse au maximum la limite extraite du code. Son option d’accessibilité peut le désactiver sans retirer le perk. **Minage 3×3** possède son propre toggle et ne casse que les blocs identiques au bloc d’origine dans le plan déterminé par l’orientation du joueur.

## Archéologie

Les fossiles sont classés par famille et rareté. La rareté contrôle notamment le poids de génération, le temps de nettoyage et le temps d’incubation. Le wiki affiche ces valeurs depuis les enums Java et permet de les comparer sans recopier les chiffres.

## Validation

Les arbres, constantes, familles et raretés sont inspectés statiquement. Les comportements réseau et les résultats en jeu restent distingués de cette inspection.
