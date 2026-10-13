# Configuration des Stratégies de Branches (Branch Policies)

Ce guide détaille comment activer les **stratégies de branches obligatoires** (Branch Policies) sur **Azure Repos** et **GitHub** pour garantir l'exécution des Quality Gates et la traçabilité des Pull Requests.

---

## 1. Configuration sur Azure DevOps (Azure Repos)

### Étape 1 : Accéder aux stratégies de la branche `main`
1. Rendez-vous dans votre projet Azure DevOps : **Repos** > **Branches**.
2. Sur la ligne de la branche `main`, cliquez sur les **`...`** (More options) puis **Branch policies**.

### Étape 2 : Activer la validation par les pairs (Reviewers)
- **Require a minimum number of reviewers** : Cocher.
  - Nombre minimum de reviewers : `1` (ou `2`).
  - Cocher : *Allow requestors to approve their own changes* (optionnel selon le cadre pédagogique).
  - Cocher : *Reset code reviewer votes when there are new changes*.

### Étape 3 : Exiger la liaison à un Work Item
- **Check for linked work items** : Cocher.
  - Sélectionner : **Required** (bloque le merge si aucun Work Item `AB#...` n'est associé à la PR).

### Étape 4 : Activer la Validation de Build (Build Validation / Quality Gate)
- Cliquez sur **`+ Add build policy`** :
  - **Build pipeline** : sélectionner le pipeline `azure-pipelines.yml` (Boutique-CI-CD).
  - **Trigger** : `Automatic`.
  - **Policy requirement** : `Required`.
  - **Build expiration** : `Immediately when main is updated`.
  - *Conséquence* : Le pipeline (compilation, 81 tests JUnit, vérification du seuil JaCoCo $\ge 80\%$) doit obligatoirement être au vert pour débloquer le bouton de merge.

### Étape 5 : Résolution des commentaires
- **Check for comment resolution** : Cocher **Required** (tous les fils de discussion de la PR doivent être marqués "Resolved").

---

## 2. Configuration sur GitHub (Branch Protection Rules)

1. Rendez-vous dans votre dépôt GitHub : **Settings** > **Branches**.
2. Cliquez sur **Add branch ruleset** ou **Add branch protection rule**.
3. Définir le motif de branche : `main` (et `develop` si applicable).
4. Activer les règles clés :
   - **Require a pull request before merging** :
     - Nombre d'approbations : `1`.
     - Cocher *Dismiss stale pull request approvals when new commits are pushed*.
   - **Require status checks to pass before merging** :
     - Cocher *Require branches to be up to date before merging*.
     - Sélectionner le check généré par GitHub Actions : `Build & Test (JaCoCo Quality Gate)`.
   - **Require linear history** (recommandé pour un historique Git propre).
   - Cocher *Do not allow bypassing the above settings* (applique la règle aux admins).

---

## 3. Utilisation conjointe du template de Pull Request
Lors de l'ouverture d'une PR :
1. Le template [.azuredevops/pull_request_template.md](.azuredevops/pull_request_template.md) ou [.github/pull_request_template.md](.github/pull_request_template.md) se charge automatiquement.
2. Le développeur renseigne l'ID du Work Item (`Fixes AB#101`).
3. Il déclare les éventuels breaking changes (schéma SQL, contrat d'API, RBAC).
4. La politique de branche lance automatiquement le pipeline de build / test / JaCoCo.
