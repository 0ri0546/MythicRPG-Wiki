# Validation du build Astro v0.4.0

## Environnement disponible

- Node.js : `v22.16.0`
- npm : `10.9.2`
- lockfile : présent, version de projet `0.4.0`

## Commande réellement exécutée

```bash
cd website
npm ci --no-audit --no-fund
```

## Résultat réel

La commande s’est arrêtée avec le code `1` avant le build Astro. Le miroir npm interne disponible dans l’environnement ne fournit pas l’archive verrouillée suivante :

```text
404 Not Found
zwitch-2.0.4.tgz
```

Le journal brut est conservé dans `validation/build-attempt-v0.4.0.log`.

`npm run build` n’a donc pas été exécuté et aucun dossier `website/dist` n’est livré. Aucun ancien build n’est utilisé comme preuve.

## État de la source

La topologie des routes produit **401 pages attendues** : 8 pages statiques, 9 pages de skills, 196 pages d’objets et 188 pages de recettes. Ce nombre est vérifié statiquement, mais n’est pas présenté comme un build Astro réussi dans cet environnement.

L’archive reste compatible avec la chaîne validée du projet :

```bash
py scripts/build_all.py
```

Cette commande relancera l’extraction, les tests, `npm ci`, le build Astro et la vérification exigeant un `dist` à jour dans un environnement disposant du registre npm complet.
