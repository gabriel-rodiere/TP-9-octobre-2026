# Boutique en ligne – API REST Spring Boot

API de gestion de catégories, produits, clients et commandes, sécurisée par JWT.

**Stack :** Java 17+, Spring Boot 4.1.1, H2 (en mémoire), JWT, Maven (wrapper fourni).

## Lancer

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.7"   # sur ce poste, le JDK 17 est incomplet
.\mvnw.cmd spring-boot:run      # démarrer -> http://localhost:8080
.\mvnw.cmd test                 # tests
```

Console H2 : http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:boutique`, utilisateur `sa`, sans mot de passe).

## Architecture

Un package par entité, et dans chaque package les mêmes couches :

```
com.boutique
├── category/  product/  customer/  order/
│     Controller  -> HTTP + validation des DTO
│     Service     -> logique métier + transactions
│     Repository  -> accès base (Spring Data JPA)
│     Entity      -> données + règles propres (stock, statut)
│     Mapper      -> Entité <-> DTO
│     dto/        -> XxxRequest / XxxResponse
├── security/          -> login, register, JWT, règles d'accès
└── common/exception/  -> gestion centralisée des erreurs
```

Règles :
- L'API n'expose jamais les entités, uniquement des DTO.
- Un service n'utilise que son propre repository. Pour une autre entité, il passe par le service de cette entité.
- Toutes les erreurs passent par `GlobalExceptionHandler` et ont le même format JSON :
  400 (validation), 401 (non authentifié), 403 (interdit), 404 (introuvable), 409 (conflit métier).

## Authentification

1. `POST /api/auth/login` avec `{"email": "...", "password": "..."}` → renvoie un `token`.
2. Envoyer ensuite l'en-tête `Authorization: Bearer <token>` à chaque requête.

| Compte | Mot de passe | Rôle |
|---|---|---|
| alice@example.com | alice1234 | CUSTOMER |
| bob@example.com | bob12345 | CUSTOMER |
| admin@boutique.com | admin1234 | ADMIN |

- **Public :** login/register, lecture des produits et des catégories.
- **ADMIN :** modifier le catalogue, lister ou supprimer des clients, changer le statut d'une commande.
- **CUSTOMER :** uniquement son propre profil et ses propres commandes.

En production : fournir la variable d'environnement `JWT_SECRET` (32 caractères minimum) et mettre `app.demo-data.enabled=false`.

## Endpoints

| Ressource | Routes |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Catégories | `GET/POST /api/categories`, `GET/PUT/DELETE /api/categories/{id}` |
| Produits | `GET/POST /api/products`, `GET/PUT/DELETE /api/products/{id}` |
| Clients | `GET /api/customers/me`, `GET/POST /api/customers`, `GET/PUT/DELETE /api/customers/{id}` |
| Commandes | `GET/POST /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel`, `PATCH /api/orders/{id}/status` |

Exemple de création de commande :

```json
POST /api/orders
{ "customerId": 1, "items": [{ "productId": 1, "quantity": 2 }] }
```

Cycle de vie d'une commande : `PENDING → PAID → SHIPPED → DELIVERED`. Une commande peut être annulée tant qu'elle est `PENDING` ou `PAID`, et l'annulation remet le stock.

## Tester avec Postman

Importer `postman/boutique-en-ligne.postman_collection.json` dans Postman, lancer l'API, puis faire **Run collection** dans l'ordre (base fraîche).
En ligne de commande : `npx newman run postman/boutique-en-ligne.postman_collection.json`

## 🛠️ DevOps, Intégration Continue & Qualité

Le projet intègre un ensemble d'outils et de configurations pour **GitHub** et **Azure DevOps** :

- **Plan d'action global :** [PLAN_ACTION.md](PLAN_ACTION.md)
- **Work Items & User Stories :** [WORK_ITEMS.md](WORK_ITEMS.md) et fichier importable [azure-devops-work-items.csv](azure-devops-work-items.csv)
- **Modèles de Pull Request :**
  - GitHub : [.github/pull_request_template.md](.github/pull_request_template.md)
  - Azure DevOps : [.azuredevops/pull_request_template.md](.azuredevops/pull_request_template.md)
- **Pipelines CI/CD & Gates Qualité :**
  - Azure DevOps : [azure-pipelines.yml](azure-pipelines.yml) (Stages CI, JaCoCo check $\ge 80\%$, Docker build/push, Staging & Prod)
  - GitHub Actions : [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml)
- **Guide des Politiques de Branches :** [BRANCH_POLICIES.md](BRANCH_POLICIES.md)
- **Conteneurisation :** [Dockerfile](Dockerfile) multi-stage avec image Eclipse Temurin 17 JRE non-root.


## Reste à faire

- [ ] Réécrire les tests d'intégration avec authentification JWT.