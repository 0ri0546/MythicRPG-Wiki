# MythicRPG Wiki v0.4.1

Wiki statique généré depuis le code et les ressources de MythicRPG `src(92)`. Les neuf skills disposent d’une documentation approfondie ; la v0.4.1 améliore l’ergonomie des calculateurs, les recettes visuelles, les icônes des 180 perks et les filtres Eating.

## Fonctionnalités v0.4.1

- neuf skills en couverture approfondie avec 180 perks ;
- 188 recettes affichées visuellement : 129 shaped et 59 shapeless ;
- motifs shaped conservés dans une grille Minecraft 3 × 3 ;
- textures du projet et fallbacks accessibles ;
- mapping centralisé des icônes de perks ;
- nodes entièrement cliquables au pointeur et au clavier ;
- calculateurs autorisant une saisie temporairement vide ;
- checkboxes alignées par un style partagé ;
- filtres Eating insensibles à la casse, aux accents et aux espaces ;
- statuts d’objets `confirmed`, `dynamic_probable` et `model_only` conservés ;
- recherche statique, liens objets/recettes et export commun pour l’encyclopédie ;
- GitHub Pages, lockfile, workflow et déployeur automatique conservés.

## Chaîne de génération

```text
mod-source/src (lecture seule)
  -> extracteur Python
  -> catalogue JSON partagé
  -> pages Astro + JavaScript ciblé
  -> GitHub Pages
  -> export JSON filtré pour l’encyclopédie
```

Les valeurs techniques viennent du Java, des JSON, des registres et des traductions. Les pages Astro ne recopient pas les constantes d’équilibrage.

## Prérequis

- Python 3.11 ou supérieur ;
- Node.js 22.12 ou supérieur ;
- npm ;
- PyYAML.

Aucune installation Gradle ou Minecraft n’est nécessaire.

## Validation locale

```bash
python -m pip install -r requirements.txt
python tools/build_catalog.py
python -m unittest discover -s tests -v
python scripts/verify_delivery.py
python scripts/build_all.py
```

Sous Windows, `py` peut remplacer `python`. Le build complet utilise `npm ci` et produit `website/dist/`.

## Développement du site

```bash
cd website
npm ci --no-audit --no-fund
npm run dev
npm run build
npm run preview
```

## Mise à jour du mod

1. remplacer `mod-source/src/` par la nouvelle source complète ;
2. conserver `mod-source/src/** -text` dans `.gitattributes` ;
3. mettre à jour `mod-source/SOURCE.txt` et `config/source_snapshot.json` ;
4. relancer la chaîne de validation.

## Sources manuelles et générées

À éditer :

- `documentation/content/{fr,en}/skills/*.md` ;
- `config/documented_values.yaml` ;
- `config/perk_icons.yaml` ;
- `config/recipe_tag_variants.yaml` ;
- `website/src/` ;
- `tools/mythicwiki/`.

À ne pas éditer :

- `mod-source/src/` ;
- `data/generated/` ;
- `website/src/data/generated/` ;
- `website/public/generated/`.

## Limites honnêtes

L’analyse du mod reste statique : aucun Gradle, aucune compilation Java du mod et aucun lancement Minecraft. Les comportements réseau ou en jeu ne sont pas présentés comme testés lorsqu’ils sont seulement déduits du code.

Les données françaises et anglaises sont prêtes dans le catalogue ; l’interface publique et les routes restent françaises dans cette version.
