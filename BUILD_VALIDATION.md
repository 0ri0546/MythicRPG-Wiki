# Validation du build Astro v0.3.0

## Validations exécutées dans cet environnement

- génération du catalogue : réussie ;
- 38 tests Python : réussis ;
- compilation syntaxique des fichiers Python : réussie ;
- vérification syntaxique des scripts navigateur avec `node --check` : réussie ;
- validation statique indépendante : réussie ;
- source `src(92)` : inchangée.

## Limite du build Astro local

Le dossier `node_modules` fourni a été installé sous Windows et ne contient pas le binding natif Linux optionnel `@astrojs/compiler-binding-linux-x64-gnu`. Le registre npm n’est pas accessible depuis cet environnement, donc ce binding ne peut pas être restauré ici.

L’échec local se produit avant l’analyse des pages Astro. Aucun ancien `dist` ne doit être utilisé comme preuve d’un build v0.3.0.

## Commande de validation officielle

Dans l’environnement Windows ou GitHub Actions déjà validé pour le projet :

```bash
python scripts/build_all.py
```

Le script exécute l’extraction, les tests, `npm ci`, `npm run build`, puis `verify_delivery.py --require-build`.
