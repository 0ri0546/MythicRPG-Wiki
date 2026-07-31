# Tests MythicRPG Wiki v0.2

## Suite Python

La v0.2 contient 28 tests couvrant :

- préservation de la source `src(92)` ;
- neuf skills et vingt perks par skill ;
- unicité des identifiants et relations d’arbre ;
- symétrie des traductions ;
- provenance des valeurs Java ;
- classification des objets ;
- snapshot versionné ;
- courbe d’XP et cumuls de référence ;
- fossiles Mining ;
- 47 recettes culinaires et 50 sources d’ingrédients ;
- familles, raretés, distributions et monstres Fishing ;
- couverture éditoriale des neuf skills ;
- absence de chemins locaux et de `innerHTML` ;
- configuration GitHub Pages et installation npm reproductible.

## Commandes

```bash
python tools/build_catalog.py
python -m unittest discover -s tests -v
python scripts/verify_delivery.py
python scripts/build_all.py
```

`build_all.py` exécute aussi `npm ci`, le build Astro et une vérification exigeant une sortie `dist` plus récente que les sources.

## Interdictions contrôlées

Les scripts et workflows ne lancent ni Gradle, ni `runClient`, ni `runServer`, ni Minecraft.
