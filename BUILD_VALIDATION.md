# Validation du build Astro v0.4.1

## État des prérequis

- `website/package-lock.json` : présent, version de projet `0.4.1` ;
- workflow GitHub Pages : conservé ;
- script multiplateforme `scripts/build_all.py` : conservé ;
- ancien `website/dist` : supprimé avant livraison.

## Commandes réellement tentées

### Miroir npm de l’environnement

```bash
cd website
npm ci --no-audit --no-fund
```

Résultat : échec `E404` sur l’archive verrouillée `zwitch@2.0.4` du miroir npm interne.

### Registre npm public

```bash
npm ci --registry=https://registry.npmjs.org --no-audit --no-fund
```

Résultat : la commande n’a produit aucune réponse exploitable avant l’expiration du délai de l’environnement et a été interrompue. Le répertoire partiel `node_modules` a été supprimé.

## Statut honnête

- `npm ci` : non terminé ;
- `npm run build` : non exécuté après installation propre ;
- build Astro v0.4.1 : non validé dans cet environnement ;
- archive : compatible avec la chaîne validée `python scripts/build_all.py` sur Windows ou GitHub Actions disposant du registre npm.

Les journaux sont conservés dans `validation/npm-ci-v0.4.1.log` et `validation/build-attempt-v0.4.1.log`.
