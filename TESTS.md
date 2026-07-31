# Tests MythicRPG Wiki v0.4.1

La v0.4.1 conserve les 50 tests de la v0.4.0 et ajoute 9 tests ciblés, soit 59 tests Python.

## Nouveaux contrôles

- validation différée et valeur temporairement vide dans les calculateurs ;
- structure visuelle des 188 recettes ;
- distinction shaped/shapeless et présence des quantités ;
- variantes accessibles pour les ingrédients par tag ;
- mapping centralisé des 180 icônes de perks ;
- zone cliquable couvrant tout le node ;
- éléments décoratifs et lignes SVG ne bloquant pas les clics ;
- alignement partagé des checkboxes ;
- recherche Eating dans les identifiants et traductions des ingrédients ;
- normalisation de casse, accents et espaces ;
- boutons de réinitialisation et états sans résultat ;
- alignement des versions sur `0.4.1`.

## Contrôles conservés

- 9 skills, 20 perks uniques par skill ;
- relations internes cohérentes ;
- routes de recherche valides ;
- JSON et traductions cohérents ;
- absence de chemins locaux ;
- absence de `innerHTML` ;
- lockfile reproductible et workflow `npm ci` ;
- empreinte et nombre de fichiers de `src(92)` inchangés ;
- aucun lancement Gradle ou Minecraft.

## Commandes

```bash
python tools/build_catalog.py
python -m unittest discover -s tests -v
python scripts/verify_delivery.py
python scripts/build_all.py
```

Le dernier script exige un accès fonctionnel aux paquets npm verrouillés et valide ensuite le build Astro.
