---
id: fighting
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Développez les dégâts, la survie, le butin et l’expérience de combat, puis affrontez le catalogue complet des Barons et leurs récompenses légendaires."
key_systems:
  - "Dégâts de mêlée, portée, cadence et effets à l’impact"
  - "Résistances, régénération et protections permanentes"
  - "Butin, expérience Fighting et expérience vanilla"
  - "Promotion, scaling et comportements des Barons"
  - "Récompenses spéciales et objets légendaires"
xp_sources:
  - "Mort d’une créature vivante attribuée à un joueur : gain calculé depuis sa vie maximale"
  - "Mort d’un Baron : base renforcée puis multipliée selon son niveau de naissance"
  - "Les gains et bonus supplémentaires sont appliqués côté serveur par les perks concernés"
multiplayer: "La promotion des Barons, leur niveau de naissance, leurs capacités, leurs dégâts et leurs récompenses sont autoritaires côté serveur. Une protection de proximité évite qu’un joueur avancé déclenche un Baron près d’un joueur encore sous le seuil prévu."
---
# Fighting

Fighting améliore la manière de combattre sans imposer une classe particulière. Son arbre complet couvre les attaques directes, les effets appliqués aux ennemis, la défense, les récompenses et la confrontation avec les Barons.

## Gagner de l’expérience

Le gestionnaire Fighting attribue l’expérience lorsqu’une créature vivante meurt avec un joueur identifié comme attaquant. Le gain normal est calculé depuis la vie maximale de la créature, avec un minimum et un plafond extraits du code. Un Baron utilise sa vie de base, avant le multiplicateur de caractéristiques, puis reçoit un bonus propre aux Barons et un multiplicateur de récompense lié à son niveau de naissance.

Le calculateur de cette page reproduit ces formules depuis le catalogue généré. Il ne simule pas un combat Minecraft et ne remplace pas une validation en jeu.

## Arbre des perks

Les perks, leurs positions, leurs parents et leurs conflits de branches sont lus depuis l’arbre Java. Le simulateur permet de vérifier un parcours, le coût total et les prérequis manquants. Les branches couvrent notamment les dégâts, la vitesse d’attaque, la portée, le poison, le vol de vie, les résistances, le butin, l’expérience et les effets permanents.

## Système des Barons

Une créature éligible peut être promue lors de son apparition. Le serveur recherche le joueur de référence, utilise son niveau Fighting pour la chance de promotion et fixe le niveau de naissance du Baron. Une seconde sélection détermine si le Baron reste normal ou reçoit un comportement spécial compatible avec son type de créature.

La page présente les conditions communes, les paliers de chance, les multiplicateurs de vie, de dégâts et d’expérience, ainsi que le catalogue complet des types. Chaque fiche précise les créatures compatibles, le comportement extrait, ses constantes lorsqu’elles existent et les récompenses associées.

## Récompenses et objets légendaires

Les récompenses sont résolues côté serveur à la mort du Baron. Certaines sont garanties, d’autres utilisent une probabilité ou une condition particulière. Les objets MythicRPG liés aux Barons disposent de liens vers leurs fiches, tout en conservant leur statut de preuve d’enregistrement dans le catalogue général.

Les objets légendaires sont documentés à partir de leurs classes spécialisées : leurs constantes techniques sont affichées lorsqu’elles peuvent être extraites de façon déterministe. Une absence de valeur dans une fiche ne doit pas être interprétée comme une absence d’effet ; elle signifie seulement que la mécanique n’est pas représentée par une constante simple exportée.

## Solo et multijoueur

En solo, le joueur local est naturellement le joueur de référence. En multijoueur, le serveur choisit le joueur proche utilisé pour le niveau de naissance et applique une protection pour les joueurs Fighting moins avancés situés dans la zone prévue. Les attaques spéciales, les tags, les bossbars, les dégâts et les récompenses restent des décisions serveur.

Cette documentation décrit l’état visible statiquement dans `src(92)`. Elle ne prétend pas valider la fréquence réelle des rencontres, l’équilibrage ressenti ou la stabilité de chaque capacité en jeu.
