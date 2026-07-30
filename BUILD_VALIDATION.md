# Validation npm et Astro

## Commandes tentées

```bash
npm view astro@7.1.4 version
npm install --package-lock-only --ignore-scripts
```

## Résultat

Le registre configuré dans l’environnement répond `404 Not Found` pour `astro`, `vite` et `typescript`. Le registre public npm n’est pas joignable depuis le conteneur.

Par conséquent :

- aucun `package-lock.json` n’a été inventé ou copié depuis un projet différent ;
- `npm ci` n’a pas été exécuté ;
- le build Astro n’est pas déclaré comme validé ;
- `website/dist` n’est pas livré comme résultat de build.

## Validation à effectuer dans un environnement avec npm accessible

```bash
cd website
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm ci --no-audit --no-fund
npm run build
cd ..
python -m unittest discover -s tests -v
python scripts/verify_delivery.py
```

Après génération, `website/package-lock.json` doit être versionné. Le workflow GitHub Pages est déjà configuré pour utiliser `npm ci`.
