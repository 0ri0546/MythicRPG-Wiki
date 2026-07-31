# Tests MythicRPG Wiki v0.3.0

La v0.3.0 contient 38 tests Python : les 28 tests existants sont conservés et 10 tests couvrent Fighting, Crafting et les contraintes de livraison.

## Invariants généraux

- source `src(92)` inchangée avant et après extraction ;
- empreinte et nombre de fichiers conformes au snapshot ;
- neuf skills et vingt perks par skill ;
- relations internes valides ;
- traductions française et anglaise symétriques ;
- statuts `confirmed`, `dynamic_probable` et `model_only` cohérents ;
- absence de chemins locaux dans les sorties ;
- absence d’exécution Gradle ou Minecraft ;
- absence d’affectation à `innerHTML` ;
- `package-lock.json`, `npm ci` et règle `.gitattributes` conservés.

## Fighting

- couverture approfondie et arbre complet ;
- formules d’XP extraites ;
- 25 types de Barons uniques ;
- paliers de promotion continus ;
- scaling aux niveaux de référence ;
- récompenses valides ;
- cinq objets légendaires liés ;
- autorité serveur documentée.

## Crafting

- couverture approfondie et arbre complet ;
- 55 entrées de Craft Score ;
- trois stations et propriétés d’interface ;
- quatre groupes de recyclage ;
- 48 transformations uniques ;
- 21 événements Lucky Block ;
- poids et pourcentages cohérents ;
- règles d’infusion et autorité serveur.

## Commandes

```bash
python tools/build_catalog.py
python -m unittest discover -s tests -v
python scripts/verify_delivery.py
python scripts/build_all.py
```
