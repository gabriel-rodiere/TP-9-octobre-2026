## 🎯 Contexte & Objectif
- **Azure DevOps Work Item** : Fixes AB#
- **Résumé de la PR** : 

---

## 🏷️ Type de Changement
- [ ] 🚀 **Feature** (Nouvelle fonctionnalité)
- [ ] 🐛 **Bug fix** (Correction de bogue)
- [ ] 💥 **Breaking change** (Rupture de compatibilité API / BD)
- [ ] ♻️ **Refactoring**
- [ ] ⚙️ **Build / CI/CD**
- [ ] 📖 **Documentation**

---

## ⚠️ Impacts & Breaking Changes
- [ ] **Aucun impact bloquant identifié**
- **Contrats d'API (DTO / Endpoints)** : 
- **Persistance & Données (JPA / SQL)** : 
- **Sécurité (JWT / Rôles RBAC)** : 
- **Configuration requise (Variables d'environnement)** : 

---

## 🧪 Validation & Tests
- [ ] Tests Maven exécutés avec succès (`mvn test`)
- [ ] Couverture de code JaCoCo conforme aux critères ($\ge 80\%$)
- [ ] Tests de non-régression de sécurité validés (tests d'intégration JWT)
- [ ] Collection Postman synchronisée si nécessaire

---

## ✅ Checklist Qualité (Definition of Done)
- [ ] Le code suit les normes de l'équipe (pas d'entités exposées, mappers DTO stricts).
- [ ] Les exceptions métier sont traitées par le gestionnaire d'erreur global.
- [ ] Aucun secret ni jeton n'est commité en clair.
- [ ] Tous les checks de pipeline de validation de build passent avec succès.
