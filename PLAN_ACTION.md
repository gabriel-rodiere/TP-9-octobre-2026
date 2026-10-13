# Plan d'Action DevOps & Qualité – Projet Fil Rouge

Ce document récapitule l'ensemble des étapes et des artefacts mis en place pour le projet Fil Rouge (**Boutique en ligne – Spring Boot 4, Java 17+, Maven, JaCoCo, JWT, H2**), synchronisé entre **GitHub** et **Azure DevOps**.

---

## 📋 Synthèse des Tâches et Artefacts

| Tâche | Artefact / Fichier | Description |
|---|---|---|
| **User Stories & Work Items** | [WORK_ITEMS.md](WORK_ITEMS.md) | Backlog structuré (Epics, Features, US avec critères Gherkin et DoD). |
| **Pull Requests Intelligentes** | [.github/pull_request_template.md](.github/pull_request_template.md)<br>[.azuredevops/pull_request_template.md](.azuredevops/pull_request_template.md) | Modèles de PR standardisés : contexte AB#, détection des breaking changes, checklist DoD. |
| **Quality Gate (Couverture)** | [pom.xml](pom.xml#L111-L148) | Intégration de `jacoco-maven-plugin` avec vérification du seuil minimal de 80% de couverture. |
| **Conteneurisation** | [Dockerfile](Dockerfile) | Build multi-stage optimisé (cache Maven + JRE alpine minimale et sécurisée en non-root). |
| **Pipeline Azure DevOps** | [azure-pipelines.yml](azure-pipelines.yml) | Pipeline complet en 5 stages (CI, Quality Gate, Package ACR, Deploy Dev, Deploy Prod). |
| **Pipeline GitHub Actions** | [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml) | Pipeline miroir CI/CD pour GitHub. |

---

## 🚀 Détail des Étapes du Plan d'Action

### Étape 1 : Structuration des Work Items & User Stories Agile
1. **Identification des grands domaines fonctionnels et techniques** :
   - *EPIC 1 : Sécurité & Gestion des Utilisateurs* (inscription, login JWT, contrôle d'accès RBAC).
   - *EPIC 2 : Gestion du Catalogue* (consultation publique filtrée, administration des catégories/produits).
   - *EPIC 3 : Gestion des Commandes & Transactions* (réservation atomique de stock, transitions de statut, rollback).
   - *EPIC 4 : Qualité Logicielle & DevOps* (tests automatisés, couverture JaCoCo, intégration continue).
2. **Rédaction formalisée des User Stories** :
   - Utilisation du canevas standard : *En tant que... Je veux... Afin de...*
   - Spécification des critères d'acceptance au format **Gherkin (`Given-When-Then`)**.
   - Alignement direct avec les contrats métier de [PlanTests.md](PlanTests.md).
3. **Formalisation de la Definition of Done (DoD)** :
   - Règles strictes pour considérer un ticket terminé (tests au vert, $\ge 80\%$ de couverture JaCoCo, revue de code, critères BDD validés).

---

### Étape 2 : Standardisation des Pull Requests Intelligentes
1. **Implémentation des templates de PR** pour GitHub et Azure DevOps.
2. **Sections clés du modèle** :
   - **Traçabilité** : liaison automatique aux tickets (`Fixes AB#` ou `Closes #`).
   - **Classification des changements** : *Feature, Bug fix, Breaking change, Refactor, DevOps, Doc*.
   - **Détection des Breaking Changes** : vérification explicite des ruptures de contrats (DTOs, endpoints HTTP, schémas relationnels JPA/`data.sql`, claims JWT).
   - **Checklist Qualité (DoD)** : respect de l'architecture en couches, non-exposition des entités JPA, logs sans secrets.

---

### Étape 3 : Configuration des Gates Qualité (JaCoCo)
1. **Ajout du plugin JaCoCo dans [pom.xml](pom.xml)** :
   - Configuration de l'agent d'exécution (`prepare-agent`).
   - Génération automatique des rapports au format XML et HTML (`report`).
   - Définition de la règle d'intégrité minimale (`check`) : ratio de lignes couvertes (`COVEREDRATIO`) fixé à **80% minimum**.
2. **Verrouillage du pipeline** :
   - Tout commit ou PR faisant chuter la couverture globale sous les 80% échoue automatiquement lors de l'exécution du goal `jacoco:check`.

---

### Étape 4 : Conteneurisation & Build Multi-Stage
1. **Conception du [Dockerfile](Dockerfile)** :
   - **Stage 1 (Builder)** : Image `eclipse-temurin:17-jdk-alpine`, mise en cache de la couche dépendances via le wrapper Maven `./mvnw dependency:go-offline`, puis compilation.
   - **Stage 2 (Runtime)** : Image allégée `eclipse-temurin:17-jre-alpine`, création d'un groupe et d'un utilisateur non-root (`appuser`), injection du JAR produit, exposition du port 8080.

---

### Étape 5 : Construction du Pipeline Complet (CI/CD Multi-Stage)
1. **Mise en œuvre du pipeline [azure-pipelines.yml](azure-pipelines.yml)** :
   - **Stage CI (`BuildAndTest`)** :
     - Provisionnement du JDK 17 (`JavaToolInstaller@0`).
     - Mise en cache du dossier `~/.m2/repository`.
     - Exécution de `./mvnw clean test`.
     - Publication des résultats JUnit (`surefire-reports`) et du rapport de couverture JaCoCo.
   - **Stage QualityGate (`VerifyQualityGates`)** :
     - Exécution de `./mvnw jacoco:check` pour bloquer la progression en cas de non-respect du seuil de couverture.
   - **Stage Package (`BuildAndPushDocker`)** :
     - Déclenché uniquement sur la branche `main` après validation des tests.
     - Build de l'image Docker et publication sur Azure Container Registry (ACR).
     - Publication de la collection Postman en tant qu'artefact de build.
   - **Stage Deploy Dev (`DeployToDev`)** :
     - Déploiement du conteneur sur l'environnement de développement/recette (`filrouge-dev`).
     - Exécution automatique des smoke tests via Newman (`postman/boutique-en-ligne.postman_collection.json`).
   - **Stage Deploy Prod (`DeployToProd`)** :
     - Déploiement en production (`filrouge-production`) conditionné par l'approbation manuelle définie sur l'environnement Azure DevOps.
2. **Configuration miroir pour GitHub Actions** via [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml).
