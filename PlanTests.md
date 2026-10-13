# Plan de gestion des tests

## Objectif

Couvrir d'abord les parcours valides, puis les limites de validation, de stock, de statut et d'autorisation. Ce document planifie les tests; il ne modifie pas le code applicatif.

## Etat d'avancement

- Etapes 1 et 2 realisees : contrats metier fixes et tests unitaires ajoutes.
- Etapes 3 et 4 realisees : tests MVC avec JWT reels ajoutes dans `ApiSecurityAndValidationTests`.
- Etape 5 realisee : tests d'integration transactionnelle ajoutes dans `OrderTransactionIntegrationTests`.

## 1. Fixer les comportements attendus

Contrats retenus pour les tests :

- Toute quantite de stock doit etre strictement positive, y compris lors d'un appel direct aux methodes metier. Les quantites nulles ou negatives sont rejetees.
- Les noms de categories sont uniques sans distinction de casse, a la creation comme a la mise a jour. Une categorie peut conserver son propre nom.

Ces regles evitent les stocks majores ou decrements de maniere incorrecte et les doublons de categories.

## 2. Couvrir les regles metier isolement

Ajouter des tests unitaires autour des regles des services et entites, sans demarrer Spring :

- **Produits et stock** : reservation valide, stock exactement suffisant, stock insuffisant, quantites minimales et limites, liberation apres annulation.
- **Commandes** : creation simple et multi-articles, total calcule, prix unitaire conserve, echec d'une ligne invalide.
- **Statuts** : chaque transition autorisee et interdite, y compris les statuts terminaux.
- **Clients** : normalisation d'email, doublon avec casse differente, mot de passe hache, role attribue lors de l'inscription.
- **Categories** : creation, doublon insensible a la casse et comportement retenu pour un doublon lors d'une mise a jour.

Les tests existants de `ProductService.findAll` peuvent rester des tests de caracterisation. Les nouveaux scenarios metier peuvent etre separes pour rester lisibles.

## 3. Verifier les limites et erreurs HTTP

Dans des tests MVC ou d'integration, couvrir les entrees invalides et les reponses :

- DTO incomplets ou invalides : chaines vides, email mal forme, longueurs maximales, prix sous le minimum, stock negatif, liste de commande vide et quantite inferieure a 1.
- Ressource absente : 404; conflit metier ou integrite referentielle : 409; erreur de validation : 400.
- Verifier le format commun d'erreur via `GlobalExceptionHandler`.

Realise : payloads d'inscription, categorie, produit et commande invalides; reponses communes 400, 404 et 409, y compris suppression d'une categorie referencee.

## 4. Valider la securite et la propriete des donnees

Tester avec de vrais JWT de roles differents, plutot que de mocker uniquement le service :

- Sans jeton : 401; jeton invalide ou expire : 401.
- CUSTOMER : acces a son profil et a ses commandes; acces aux donnees d'un autre client : 403.
- ADMIN : operations administratives autorisees.
- Routes publiques du catalogue accessibles sans jeton; modifications du catalogue reservees a ADMIN.
- Verifier qu'un client ne peut ni creer une commande au nom d'un autre, ni changer son statut.

Realise : tests avec tokens signes et token expire; acces public au catalogue; CUSTOMER et ADMIN; proprietaire et non-proprietaire pour les ressources client/commande; creation pour un autre client et changement de statut refuses.

## 5. Tester l'integration transactionnelle des commandes

Avec H2 et une base remise a zero par test, verifier le flux complet :

- Creation d'une commande : stock decremente, commande et lignes sauvegardees.
- Echec apres reservation partielle : transaction annulee et stock inchange.
- Annulation autorisee : stock restitue une seule fois.
- Annulation interdite ou seconde annulation : etat et stock restent inchanges.

Realise : creation nominale, rollback apres reservation partielle, annulation puis seconde annulation, avec controle du stock et du nombre de commandes en base.

## Ordre de priorite

1. Securite JWT et propriete des commandes et clients.
2. Reservation, annulation et rollback du stock.
3. Transitions de statut et reponses d'erreur.
4. Validation des DTO, CRUD catalogue et mappers.
5. Filtre de correlation et initialisation des donnees de demonstration.

## Criteres de fin

- Chaque methode metier publique a au moins un cas nominal et un cas limite pertinent.
- Chaque regle de securite, transition de statut et statut d'erreur est verifie.
- Les parcours HTTP de validation et de securite JWT sont couverts par des tests MVC.
- Le flux transactionnel de commande verifie la persistance, le rollback et la restitution unique du stock.
