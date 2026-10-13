## 🎯 Contexte & Objectif
<!-- Décrivez le but de cette PR et le besoin auquel elle répond -->
- **Work Item / Issue liée** : Closes AB# / Fixes #
- **Description synthétique** : 

---

## 🏷️ Type de Changement
- [ ] 🚀 **Feature** (Nouvelle fonctionnalité)
- [ ] 🐛 **Bug fix** (Correction non bloquante)
- [ ] 💥 **Breaking change** (Modification cassant la compatibilité de l'API / des contrats)
- [ ] ♻️ **Refactoring** (Amélioration du code sans changement de comportement)
- [ ] ⚙️ **CI/CD & DevOps** (Modification des pipelines ou configurations)
- [ ] 📖 **Documentation**

---

## ⚠️ Breaking Changes & Impacts d'Architecture
<!-- Si aucun breaking change, cochez "Aucun" -->
- [ ] **Aucun breaking change**

Si présent, détaillez l'impact :
- **Changement de contrat d'API / DTO :** *(ex: champ renommé, nouveau statut HTTP, endpoint déprécié)*
- **Modèle de données & Persistance :** *(ex: modification de tables, `data.sql`, contraintes d'intégrité)*
- **Sécurité & JWT :** *(ex: claims modifiés, nouvelles permissions requises)*
- **Variables d'environnement / Configuration :** *(ex: nouvelle propriété `application.properties`)*

---

## 🧪 Stratégie de Test & Couverture
- [ ] **Tests Unitaires** exécutés (`./mvnw test`)
- [ ] **Tests d'Intégration** validés (Order flow, Transactions, Sécurité MVC)
- [ ] **Couverture JaCoCo vérifiée** ($\ge 80\%$)
- [ ] **Collection Postman** mise à jour si nouveaux endpoints

Résultats obtenus en local :
```
Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
Couverture JaCoCo : XX%
```

---

## ✅ Checklist de Revue (DoD)
- [ ] Le code respecte l'architecture en couches (`Controller -> Service -> Repository -> Entity`).
- [ ] Les entités JPA ne sont jamais exposées directement (usage strict des DTOs).
- [ ] La gestion des erreurs s'appuie sur `GlobalExceptionHandler` avec le format standardisé.
- [ ] Les mots de passe et données sensibles ne sont jamais tracés dans les logs.
- [ ] Le ticket Azure DevOps / GitHub Issue est associé.
