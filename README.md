# MythicRPG Wiki v0.1.1

Wiki statique généré depuis le code et les ressources de MythicRPG.

## Chaîne de génération

```text
mod-source/src
  -> extracteur Python
  -> catalogue JSON commun
  -> site Astro statique
  -> GitHub Pages

  -> export JSON de l’encyclopédie en jeu
```

Les valeurs techniques ne sont pas recopiées dans les pages. Les constantes sélectionnées sont relues dans le Java, les recettes dans les JSON, les noms dans les traductions et les arbres dans les classes `*SkillTree.java`. Les textes éditoriaux et les statuts sont écrits en Markdown avec frontmatter YAML.

Les données françaises et anglaises sont préparées dans le catalogue commun. L’interface et les routes publiques de cette version restent en français ; le routage `/fr/` et `/en/` n’est pas encore implémenté.

## Prérequis

- Python 3.11 ou supérieur ;
- Node.js 22.12 ou supérieur ;
- npm ;
- aucune installation Java ou Gradle n’est nécessaire pour le wiki.

## Installation locale

Depuis la racine du projet :

```bash
python -m pip install -r requirements.txt
python tools/build_catalog.py
python -m unittest discover -s tests -v
cd website
npm ci --no-audit --no-fund
npm run dev
```

Construction complète :

```bash
python scripts/build_all.py
```

Le site statique est produit dans `website/dist/`.

## Utiliser une nouvelle source du mod

Remplacer uniquement le dossier `mod-source/src/` par le dossier `src/` complet le plus récent, sans en modifier le contenu, puis mettre à jour volontairement `config/source_snapshot.json` et exécuter :

```bash
python tools/build_catalog.py
```

Une autre source peut être fournie sans copie :

```bash
python tools/build_catalog.py --source /chemin/vers/le/mod/src
```

## Fichiers source et fichiers générés

Édités manuellement :

- `documentation/content/{fr,en}/skills/*.md` : texte et métadonnées ;
- `config/documented_values.yaml` : liste explicite des constantes Java à publier ;
- `config/source_snapshot.json` : snapshot attendu de la source canonique ;
- `website/src/` : composants, layouts et pages Astro ;
- `website/src/styles/global.css` : présentation.

Générés automatiquement, à ne pas modifier :

- `data/generated/catalog.json` ;
- `data/generated/encyclopedia.json` ;
- `data/generated/search-index.json` ;
- `data/generated/extraction-report.json` ;
- `website/src/data/generated/` ;
- `website/public/generated/`.

## GitHub Pages

1. Créer un dépôt GitHub et y pousser ce projet.
2. Dans **Settings > Pages**, sélectionner **GitHub Actions** comme source.
3. Pousser sur la branche `main` ou lancer manuellement le workflow.

Le workflow `.github/workflows/deploy-pages.yml` extrait les données, exécute les tests, installe exactement les dépendances du lockfile avec `npm ci`, construit Astro et publie `website/dist`. **La présente archive de travail ne contient pas encore de lockfile validé**, car le registre npm disponible dans l’environnement de génération retourne 404 pour Astro et ses dépendances. Voir `BUILD_VALIDATION.md`.

Le `base` GitHub Pages est calculé à partir de `GITHUB_REPOSITORY`, ce qui couvre les dépôts de projet publiés sous `https://utilisateur.github.io/depot/` et les dépôts `utilisateur.github.io` publiés à la racine.

## Validation et limites honnêtes

- inspection statique uniquement pour le mod ;
- aucun lancement Gradle ;
- aucune compilation du mod ;
- aucun lancement Minecraft ;
- le code du mod copié dans `mod-source/src` n’est pas modifié ;
- les modèles d’objets forment un catalogue large et chaque entrée porte un statut de preuve : `confirmed`, `dynamic_probable` ou `model_only` ;
- les calculs Java complexes ne sont pas devinés : seules les constantes listées dans `config/documented_values.yaml` sont exportées comme valeurs garanties ;
- l’export `data/generated/encyclopedia.json` est le premier contrat commun avec la future encyclopédie, pas encore son interface Minecraft.

Consulter également `ARCHITECTURE.md`, `TESTS.md`, `DELIVERY.md`, `BUILD_VALIDATION.md` et `REVIEW_REPORT.md`.
