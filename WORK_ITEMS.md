# Backlog Agile – Projet Fil Rouge (Boutique en ligne)

Ce document répertorie l'ensemble des **Epics**, **Features**, **User Stories** (format Agile) avec leurs **Critères d'Acceptance (Given-When-Then)** et leurs tâches techniques d'implémentation associées pour le projet Spring Boot.

---

## Structure du Backlog

```
EPIC 1: Sécurité & Gestion des Utilisateurs
  ├── FEAT-1.1: Authentification & Jetons JWT
  └── FEAT-1.2: Gestion du profil client & Contrôle d'accès (RBAC)

EPIC 2: Gestion du Catalogue (Produits & Catégories)
  ├── FEAT-2.1: Consultation publique du catalogue
  └── FEAT-2.2: Administration des catégories & stocks produits

EPIC 3: Gestion des Commandes & Transactions
  ├── FEAT-3.1: Passation de commande & Réservation de stock
  └── FEAT-3.2: Suivi & Changement de statut des commandes

EPIC 4: Qualité Logicielle & DevOps
  ├── FEAT-4.1: Tests continus & Couverture JaCoCo
  └── FEAT-4.2: Intégration & Déploiement Continus (CI/CD)
```

---

## EPIC 1 : Sécurité & Gestion des Utilisateurs

### US-01 : Inscription d'un nouveau client
- **En tant que** visiteur du site,  
- **Je souhaite** pouvoir créer un compte client avec mon nom, prénom, email et mot de passe,  
- **Afin de** pouvoir passer des commandes en ligne.

#### Critères d'Acceptance (Gherkin)
```gherkin
Scénario: Inscription réussie d'un nouveau compte
  Etant donné que l'adresse email "nouveau.client@example.com" n'est pas encore enregistrée
  Quand j'envoie une requête POST vers "/api/auth/register" avec des données valides
  Alors le code de réponse HTTP est 201 Created
  Et un compte client est créé avec le mot de passe haché (BCrypt)
  Et le rôle attribué par défaut est "CUSTOMER"
  Et un token JWT valide est renvoyé

Scénario: Échec de l'inscription si l'email existe déjà (insensible à la casse)
  Etant donné qu'un compte existe déjà avec l'email "alice@example.com"
  Quand j'envoie une requête POST vers "/api/auth/register" avec l'email "ALICE@EXAMPLE.COM"
  Alors le code de réponse HTTP est 409 Conflict
  Et un message d'erreur standardisé est retourné
```

#### Tâches techniques
- [ ] Valider le format de l'email et la complexité du mot de passe dans `RegisterRequest`.
- [ ] Hacher le mot de passe via `PasswordEncoder`.
- [ ] Test unitaire sur `CustomerService` pour la casse d'email et l'unicité.

---

### US-02 : Authentification et obtention de JWT
- **En tant que** client ou administrateur,  
- **Je souhaite** m'authentifier avec mes identifiants,  
- **Afin d'** obtenir un jeton d'accès sécurisé pour mes requêtes suivantes.

#### Critères d'Acceptance (Gherkin)
```gherkin
Scénario: Connexion avec identifiants corrects
  Etant donné un compte existant avec l'email "alice@example.com" et mot de passe "alice1234"
  Quand j'envoie une requête POST vers "/api/auth/login" avec ces identifiants
  Alors le code de réponse HTTP est 200 OK
  Et le corps de la réponse contient un "token" JWT signé avec les claims d'identité et de rôles

Scénario: Refus d'accès avec mot de passe incorrect
  Etant donné un compte existant avec l'email "alice@example.com"
  Quand j'envoie une requête POST vers "/api/auth/login" avec un mauvais mot de passe
  Alors le code de réponse HTTP est 401 Unauthorized
```

---

### US-03 : Isolation des données clients (RBAC)
- **En tant que** client connecté (`CUSTOMER`),  
- **Je souhaite** accéder uniquement à mes informations personnelles,  
- **Afin que** mes données restent confidentielles vis-à-vis des autres clients.

#### Critères d'Acceptance (Gherkin)
```gherkin
Scénario: Accès à son propre profil
  Etant donné un client connecté avec l'ID 1
  Quand il envoie une requête GET vers "/api/customers/1" avec son token
  Alors la réponse HTTP est 200 OK et ses informations sont renvoyées

Scénario: Tentative d'accès au profil d'un autre client
  Etant donné un client connecté avec l'ID 1
  Quand il envoie une requête GET vers "/api/customers/2"
  Alors la réponse HTTP est 403 Forbidden
```

---

## EPIC 2 : Gestion du Catalogue (Produits & Catégories)

### US-04 : Consultation publique du catalogue de produits
- **En tant que** visiteur anonyme ou client,  
- **Je souhaite** consulter la liste des produits avec filtres (mot-clé, catégorie),  
- **Afin de** trouver les articles que je désire acheter.

#### Critères d'Acceptance (Gherkin)
```gherkin
Scénario: Consultation sans authentification
  Etant donné une requête HTTP anonyme sans en-tête Authorization
  Quand j'appelle GET "/api/products" ou GET "/api/categories"
  Alors la réponse HTTP est 200 OK
  Et la liste des éléments actifs du catalogue est renvoyée

Scénario: Recherche par mot-clé et catégorie
  Quand j'appelle GET "/api/products?keyword=clavier&categoryId=1"
  Alors seuls les produits correspondants et disponibles sont retournés
```

---

### US-05 : Gestion du catalogue par un Administrateur
- **En tant qu'** administrateur (`ADMIN`),  
- **Je souhaite** créer, modifier ou supprimer des catégories et des produits,  
- **Afin de** maintenir l'offre commerciale à jour.

#### Critères d'Acceptance (Gherkin)
```gherkin
Scénario: Création d'une catégorie par un ADMIN
  Etant donné un token avec le rôle "ADMIN"
  Quand j'envoie un POST "/api/categories" avec le nom "Périphériques"
  Alors la réponse HTTP est 201 Created

Scénario: Rejet de création si nom en doublon (insensible à la casse)
  Etant donné qu'une catégorie "Informatique" existe déjà
  Quand un ADMIN tente de créer "INFORMATIQUE"
  Alors la réponse HTTP est 409 Conflict

Scénario: Refus de modification pour un rôle CUSTOMER
  Etant donné un token avec le rôle "CUSTOMER"
  Quand j'envoie un POST "/api/products"
  Alors la réponse HTTP est 403 Forbidden
```

---

## EPIC 3 : Gestion des Commandes & Transactions

### US-06 : Création d'une commande et décrémentation atomique du stock
- **En tant que** client connecté,  
- **Je souhaite** valider un panier de commande comprenant plusieurs articles,  
- **Afin de** réserver immédiatement le stock et finaliser mon achat.

#### Critères d'Acceptance (Gherkin)
```gherkin
Scénario: Commande acceptée avec stock suffisant
  Etant donné un produit "Souris sans fil" avec un stock initial de 10 unités
  Et un client connecté "alice@example.com"
  Quand le client passe une commande pour 2 unités de "Souris sans fil"
  Alors la réponse HTTP est 201 Created
  Et le statut de la commande est "PENDING"
  Et le stock du produit en base est désormais de 8 unités
  Et le prix unitaire est figé sur la ligne de commande

Scénario: Échec de commande en cas de stock insuffisant
  Etant donné un produit ayant un stock de 1 unité
  Quand un client tente de commander 2 unités
  Alors la réponse HTTP est 409 Conflict (ou 400 Bad Request)
  Et aucune commande n'est créée
  Et le stock initial reste inchangé (Rollback transactionnel)
```

---

### US-07 : Cycle de vie et changement de statut de la commande
- **En tant qu'** administrateur,  
- **Je souhaite** faire évoluer le statut des commandes (VALIDATED, SHIPPED, DELIVERED, CANCELLED),  
- **Afin de** suivre le traitement logistique.

#### Critères d'Acceptance (Gherkin)
```gherkin
Scénario: Transition de statut valide
  Etant donné une commande à l'état "PENDING"
  Quand l'administrateur envoie un PATCH "/api/orders/1/status" avec "VALIDATED"
  Alors la réponse est 200 OK et le statut est mis à jour

Scénario: Restitution du stock en cas d'annulation
  Etant donné une commande "PENDING" contenant 2 unités du produit P
  Quand la commande passe au statut "CANCELLED"
  Alors le stock du produit P est ré-incrémenté de 2 unités

Scénario: Transition interdite depuis un état terminal
  Etant donné une commande à l'état "CANCELLED" ou "DELIVERED"
  Quand on tente de modifier son statut
  Alors la réponse HTTP est 409 Conflict
```

---

## EPIC 4 : DevOps & Qualité de Code

### US-08 : Validation continue des tests et couverture de code minimale
- **En tant que** Tech Lead / Équipe de développement,  
- **Je souhaite** que chaque commit et PR exécute l'intégralité des tests et vérifie la couverture de code,  
- **Afin d'** empêcher toute régression fonctionnelle sur les contrats d'API.

#### Critères d'Acceptance
- [ ] 100% des tests unitaires et d'intégration Maven sont exécutés sans erreur.
- [ ] Le rapport JaCoCo est produit à la fin de la phase de test.
- [ ] Le seuil minimal global de couverture de lignes est fixé à $\ge 80\%$.
- [ ] Si la couverture est inférieure à $80\%$, le build est en échec (Quality Gate).

---

## Matrice de Définition de Fini (Definition of Done - DoD)

Pour qu'un Work Item / User Story soit considéré comme **Terminé (Done)** :
1. **Code complet & documenté** selon les conventions du projet.
2. **Tests unitaires et d'intégration** écrits et validés avec `mvn test`.
3. **Couverture JaCoCo** $\ge 80\%$ respectée sans baisse sur les packages critiques.
4. **Revue de code via Pull Request** approuvée par au moins un pair.
5. **Critères d'acceptance Gherkin** vérifiés avec succès sur l'environnement de staging.
