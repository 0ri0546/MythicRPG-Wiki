# MythicRPG Wiki v0.3.0

Wiki statique généré depuis le code et les ressources de MythicRPG `src(92)`, avec une documentation approfondie de Fighting et Crafting.

## Fonctionnalités v0.3.0

- neuf skills documentés et navigables ;
- 180 perks extraits, avec arbres zoomables, filtrables, simulateur de build et tableau statique complet ;
- documentation approfondie de Mining, Eating, Fishing, Fighting et Crafting ;
- 25 types de Barons, comportements, conditions, scaling et récompenses extraits ;
- cinq objets légendaires Fighting reliés à leurs fiches ;
- Craft Score, stations, interfaces et bonus Crafting extraits ;
- 48 transformations, quatre groupes de recyclage et 21 événements Lucky Block ;
- objets conservant les statuts `confirmed`, `dynamic_probable` et `model_only` ;
- recherche statique enrichie ;
- catalogue JSON commun au site et à la future encyclopédie ;
- déploiement GitHub Pages par GitHub Actions et lanceur automatique conservés.

## Chaîne de génération

```text
mod-source/src (lecture seule)
  -> extracteur Python
  -> catalogue JSON partagé
  -> pages Astro + modules TypeScript ciblés
  -> GitHub Pages

  -> export JSON filtré pour l’encyclopédie
```

Les valeurs techniques ne sont pas recopiées dans les pages. Les constantes et formules sélectionnées sont relues dans le Java, les recettes dans les JSON ou registres Java, les noms dans les traductions et les arbres dans les classes `*SkillTree.java`.

## Prérequis

- Python 3.11 ou supérieur ;
- Node.js 22.12 ou supérieur ;
- npm ;
- PyYAML.

Aucune installation Gradle ou Minecraft n’est nécessaire pour générer le wiki.

## Validation locale

```bash
python -m pip install -r requirements.txt
python tools/build_catalog.py
python -m unittest discover -s tests -v
python scripts/verify_delivery.py
python scripts/build_all.py
```

Sous Windows, `py` peut remplacer `python`. Le build complet utilise `npm ci`, puis produit le site dans `website/dist/`.

## Développement du site

```bash
cd website
npm ci --no-audit --no-fund
npm run dev
npm run build
npm run preview
```

## Mise à jour du mod

1. remplacer `mod-source/src/` par le nouveau dossier `src/` complet ;
2. conserver `mod-source/src/** -text` dans `.gitattributes` ;
3. mettre à jour `mod-source/SOURCE.txt` et `config/source_snapshot.json` ;
4. relancer la chaîne de validation complète.

## Sources manuelles et générées

À éditer :

- `documentation/content/{fr,en}/skills/*.md` ;
- `config/documented_values.yaml` ;
- `config/source_snapshot.json` ;
- `website/src/` ;
- les extracteurs spécialisés dans `tools/mythicwiki/`.

À ne pas éditer :

- `mod-source/src/` ;
- `data/generated/` ;
- `website/src/data/generated/` ;
- `website/public/generated/`.

## Limites honnêtes

L’analyse du mod reste statique : aucun Gradle, aucune compilation Java du mod et aucun lancement Minecraft. Les comportements réseau ou en jeu ne sont pas présentés comme testés lorsqu’ils sont seulement déduits du code.

Les données françaises et anglaises sont prêtes dans le catalogue ; l’interface publique et les routes restent françaises dans cette version.
