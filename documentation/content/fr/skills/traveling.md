---
id: traveling
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Explorez, découvrez des structures, adoptez des montures et débloquez des outils de déplacement spécialisés."
key_systems:
  - "Déplacement, dimensions, structures et trésors"
  - "22 montures terrestres ou volantes et leurs selles"
  - "Traveler Boat, Traveler Minecart et Fishing Boat"
  - "Boussole monumentale et 22 modules de structures"
  - "Grappin, rappel mortel, miniaturisation et double saut"
xp_sources:
  - "Distance parcourue, avec contrôle anti-téléportation"
  - "Première visite de dimensions"
  - "Découverte de structures configurées"
  - "Ouverture de coffres au trésor"
  - "Déplacements en véhicule suivis par la position du joueur"
multiplayer: "L’XP, les découvertes, les montures, la boussole, le grappin et le rappel mortel sont validés côté serveur. Les payloads clients transmettent surtout des intentions ou des informations visuelles."
---
# Voyage

Traveling récompense l’exploration sans imposer un itinéraire. Le skill progresse par le déplacement réel, les premières visites de dimensions, la découverte de structures et les trésors. Les seuils de distance, les récompenses et les protections contre les téléportations sont lus directement dans `src(92)`.

## Déplacement et découvertes

Le gestionnaire accumule la distance parcourue et vérifie aussi une distance directe avant d’accorder le gain. Un déplacement trop important sur un seul tick est traité comme une téléportation et ne nourrit pas normalement le compteur. Les découvertes de dimensions et de structures sont persistantes par joueur afin d’éviter de récompenser plusieurs fois la même découverte.

## Mobilité personnelle

Le double saut est une action cliente validée par le serveur. La miniaturisation applique un changement d’échelle contrôlé périodiquement. Les perks de marche sur les âmes, de grâce du dauphin, de neige poudreuse et de vitesse de biome modifient le déplacement sans remplacer les règles vanilla.

## Montures et véhicules

Le catalogue contient les montures terrestres et volantes, leur selle, le perk requis et leurs aliments de soin. Une créature compatible doit être suffisamment affaiblie avant l’adoption. Les données de propriétaire et de point d’ancrage sont conservées côté serveur.

Le Traveler Boat et le Traveler Minecart utilisent un multiplicateur de vitesse extrait de leurs entités. Le Fishing Boat reste une source volontaire d’XP Traveling : le suivi repose sur la position du joueur et ne contient pas d’exclusion générale des véhicules dans l’état inspecté.

## Outils d’exploration

La Boussole monumentale recherche des structures dans un rayon normal ou étendu par un module. Les 22 modules sont classés par royaume et reliés aux structures qu’ils ciblent. Le Death Recall Token est lié à son propriétaire, possède une durée de vie et un délai d’utilisation, puis recherche une position sûre autour du lieu de mort. Le grappin valide sa portée, sa vitesse de traction, son arrivée et la protection temporaire contre la chute côté serveur.

## Choix de fin d’arbre

Les montures volantes et le grappin appartiennent à des branches exclusives de fin d’arbre. L’arbre commun conserve les positions, parents et identifiants de conflit réels ; le simulateur signale donc les parcours incompatibles sans proposer de construction obligatoire.

## Validation

Les constantes, registres, enums et relations présentées sont issus d’une inspection statique. La sensation de vitesse, la stabilité réseau et l’équilibrage des montures doivent rester distingués d’un test en jeu.
