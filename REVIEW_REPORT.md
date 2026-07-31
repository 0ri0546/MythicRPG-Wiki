# Rapport de seconde relecture — MythicRPG Wiki v0.4.1

## Périmètre relu

- extraction Python et catalogue ;
- rendu des recettes ;
- mapping des icônes de perks ;
- arbres et zones cliquables ;
- calculateurs numériques ;
- filtres Eating ;
- styles partagés de formulaires ;
- versions, lockfile, workflow et scripts ;
- intégrité de `mod-source/src` ;
- exclusions de l’archive finale.

## Origine des corrections

### Zones cliquables des perks

Le gestionnaire de déplacement testait uniquement si `event.target` était directement un `HTMLButtonElement`. Un clic sur le numéro ou l’image produisait un élément enfant comme cible et déclenchait le déplacement au lieu du node. Le test utilise maintenant `closest('[data-node]')`, et les descendants décoratifs ne capturent plus les événements.

### Filtre Eating

Le filtre comparait une chaîne en minuscules contenant principalement identifiants et catégories. Les noms traduits des ingrédients n’étaient pas inclus dans les recettes, et les accents n’étaient pas normalisés. Les données de recherche comprennent désormais identifiant, noms français/anglais et catégories ; la comparaison utilise une normalisation Unicode NFD commune.

### Calculateurs

Les composants bornaient puis réécrivaient la valeur pendant chaque événement `input`. La chaîne vide était immédiatement convertie en minimum. La lecture et l’écriture sont maintenant séparées : lecture non destructive pendant la frappe, commit au `blur`.

### Recettes

L’ancien extracteur conservait les ingrédients mais perdait l’affectation clé → symbole du motif shaped. Le nouvel extracteur normalise le motif, la clé et les slots avant le rendu Astro.

## Résultats attendus de la validation

- 188 recettes visuelles ;
- 180 perks avec mapping ;
- 59 tests Python ;
- `src(92)` inchangée ;
- aucun `innerHTML` ;
- aucun ancien build dans l’archive.

## Limite

Le build Astro n’a pas pu être validé dans cet environnement en raison de l’indisponibilité d’une archive npm verrouillée. Cette limite est explicitement documentée et n’est pas présentée comme un succès.
