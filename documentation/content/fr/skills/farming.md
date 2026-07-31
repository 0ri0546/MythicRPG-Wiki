---
id: farming
status: stable
introduced_in: v0.1
coverage: deep
visibility: [website, encyclopedia]
spoiler: false
summary: "Progressez par les récoltes mûres et l’élevage, puis développez la replantation, la croissance et le stockage agricole."
key_systems:
  - "Quatre catégories de récoltes donnant 2 à 4 XP"
  - "Élevage validé par la naissance d’un bébé compatible"
  - "Récolte de zone limitée à 96 blocs"
  - "Champ vivant, compost et bonus de croissance"
  - "Graine enchantée, Fleur enchantée et Food Backpack"
xp_sources:
  - "Récolte manuelle de cultures arrivées à maturité"
  - "Récolte de blocs de champignons, melons et citrouilles pris en charge"
  - "Reproduction animale confirmée par l’apparition du bébé"
multiplayer: "Les cassures, correspondances d’élevage, croissances et inventaires du Food Backpack sont contrôlés côté serveur. Les actions d’élevage en attente sont temporaires et propres à la session."
---
# Agriculture

Farming récompense des actions agricoles terminées plutôt que la simple présence d’une plantation. L’XP de récolte exige un état mûr et l’XP d’élevage n’est accordée qu’après la détection d’un nouveau bébé correspondant à une action valide.

## Récoltes et XP

Les cultures standard, le Nether Wart et le cacao, les blocs de champignons, puis les melons et citrouilles utilisent des valeurs distinctes extraites du gestionnaire. La replantation récente est protégée pendant quelques ticks pour éviter une double attribution immédiate. Les croissances automatiques provoquées par les perks ne donnent pas directement d’XP : la récolte reste nécessaire.

## Récolte de zone

Les perks de portée augmentent le rayon des houes. Une récolte de zone exige une houe, un bloc agricole mûr et s’arrête à la limite maximale extraite. Un garde de réentrance empêche que les cassures secondaires relancent récursivement le système.

## Élevage

Une action d’élevage mémorise temporairement les parents lorsque l’objet utilisé et leur état sont valides. À l’apparition d’un bébé, le serveur recherche une action compatible dans le même monde, avec le bon type d’entité et dans le rayon prévu. Cette méthode limite les gains attribués sans naissance réelle.

## Croissance et récompenses

Champ vivant effectue un nombre contrôlé de tentatives de croissance autour du joueur et prend en charge plusieurs familles de plantes vanilla. Les récompenses de compost utilisent cinq résultats possibles, dont des ressources fixes, une plante issue d’un pool ou de l’expérience vanilla.

## Objets et conservation

La Graine enchantée utilise des chances progressives issues des perks. La Fleur enchantée peut participer à la cuisson depuis la main secondaire et interagit avec le Food Backpack. Celui-ci possède 54 emplacements, accepte les ressources agricoles prévues, refuse les sacs imbriqués et s’intègre aux règles de conservation Eating. Fermier préservé conserve l’expérience vanilla et les sacs lors de la copie du joueur après une mort.

## Interactions

Le Growth Totem provient de l’archéologie Mining mais agit sur la croissance agricole. Les ingrédients et aliments stockés peuvent ensuite servir au skill Eating. Ces relations sont affichées comme interactions entre systèmes, pas comme obligation de progression.
