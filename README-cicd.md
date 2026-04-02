# Pipeline CI/CD DevSecOps — Orion UCRM

Ce document décrit le pipeline CI/CD mis en place pour le projet **Orion UCRM** (Angular 17 + Spring Boot 3.2).

---

## Vue d'ensemble

```
┌──────────┐   ┌──────────┐   ┌───────────┐   ┌──────────┐   ┌───────────┐   ┌──────────┐
│  LINT    │ → │  TEST    │ → │   BUILD   │ → │   SCAN   │ → │  PUBLISH  │ → │  DEPLOY  │
│ ESLint   │   │ Karma    │   │ Docker ×3 │   │  Trivy   │   │ :latest   │   │ Staging  │
│ SpotBugs │   │ JUnit 5  │   │ + push SHA│   │ CVE gate │   │ semver    │   │ SSH+DC   │
└──────────┘   └──────────┘   └───────────┘   └──────────┘   └───────────┘   └──────────┘
```

---

## Détail des stages

### Stage 1 — Lint (analyse statique)

| Job | Outil | Cible | Bloquant |
|-----|-------|-------|----------|
| `lint-frontend` | ESLint + @angular-eslint | `front/src/**/*.ts` et `*.html` | Oui |
| `lint-backend` | SpotBugs (via Gradle) | `back/src/main/**/*.java` | Oui |

**ESLint (Angular)**
- Configuration : `front/.eslintrc.json`
- Règles appliquées : `@angular-eslint/recommended` + `@typescript-eslint`
- Intégré dans Angular CLI via la cible `lint` dans `angular.json`
- Rapport exporté en artefact (`lint-frontend.txt`)

**SpotBugs (Java)**
- Plugin Gradle : `com.github.spotbugs` version 6.0.9
- Analyse le code compilé à la recherche de bug patterns courants :
  - Déréférencement de null potentiel
  - Mauvaise implémentation de `equals()` / `hashCode()`
  - Ressources non fermées, etc.
- Rapports HTML et XML dans `back/build/reports/spotbugs/`

---

### Stage 2 — Test

| Job | Framework | Couverture | Rapport GitLab |
|-----|-----------|------------|----------------|
| `test-frontend` | Karma + Jasmine | karma-coverage (Cobertura) | Onglet "Coverage" MR |
| `test-backend` | JUnit 5 | JaCoCo | Onglet "Tests" MR |

**Tests frontend**
- Runner : Chrome Headless (sans sandbox pour compatibilité Docker)
- Couverture générée via `--code-coverage` dans `front/coverage/microcrm/`
- Formats exportés : HTML, lcov.info, **cobertura-coverage.xml** (lu par GitLab)

**Tests backend**
- Runner : JUnit Platform (JUnit 5)
- Couverture JaCoCo générée dans `back/build/reports/jacoco/`
- Format XML lu par GitLab pour afficher le % de couverture dans la MR
- Rapport JUnit XML dans `back/build/test-results/test/*.xml`

---

### Stage 3 — Build

Construit les **3 images Docker** depuis le `Dockerfile` multi-stage :

| Image | Target Docker | Contenu |
|-------|--------------|---------|
| `frontend` | `front` | Caddy + dist Angular compilé |
| `backend` | `back` | JRE Alpine + JAR Spring Boot |
| `standalone` | `standalone` | Supervisor + frontend + backend |

**Stratégie de tagging sémantique :**
- Si le pipeline est déclenché par un **git tag** (ex: `v1.2.3`) → tag = `v1.2.3`
- Sinon → tag = SHA court du commit (ex: `a1b2c3d`)

Les images sont poussées vers le **GitLab Container Registry** avec le tag SHA uniquement à ce stade. Le tag `:latest` est appliqué après validation par Trivy (stage publish).

**Labels OCI** appliqués à chaque image :
- `org.opencontainers.image.revision` → SHA complet du commit
- `org.opencontainers.image.source` → URL du projet GitLab
- `org.opencontainers.image.created` → timestamp de construction

---

### Stage 4 — Scan (DevSec)

Les 3 images sont analysées **en parallèle** par **Trivy** (aquasecurity).

**Comportement :**
1. Première passe : rapport complet `HIGH + CRITICAL` → exporté en artefact JSON
2. Seconde passe : exit-code 1 si au moins une **CVE CRITIQUE** est trouvée → **pipeline bloqué**

```
CVE CRITIQUE détectée → job scan-* échoue → publish et deploy ne s'exécutent pas
```

Les rapports JSON sont visibles dans l'onglet **Security** de GitLab (si licence Ultimate) ou téléchargeables comme artefacts.

**Base de données CVE :** Trivy télécharge automatiquement la base de données de vulnérabilités. Elle est mise en cache entre les jobs pour accélérer les exécutions suivantes.

---

### Stage 5 — Publish

Exécuté **uniquement** si :
- Les 3 scans Trivy ont passé (pas de CVE CRITIQUE)
- Le pipeline tourne sur la **branche principale** (`main`/`master`) ou sur un **git tag**

Actions :
1. Pull des 3 images taguées avec le SHA
2. Retag avec `:latest`
3. Si git tag présent : retag avec le tag semver (ex: `:v1.2.3`)
4. Push vers le GitLab Container Registry

---

### Stage 6 — Deploy (staging)

Déploiement automatique sur le serveur de staging via SSH.

**Prérequis :**
1. Serveur de staging avec Docker + Docker Compose installés
2. Répertoire `/opt/orion-ucrm/` avec `docker-compose.staging.yml` présent
3. Clé SSH du pipeline autorisée dans `~/.ssh/authorized_keys` de l'utilisateur de déploiement

**Variables CI/CD à configurer** dans GitLab (`Settings → CI/CD → Variables`) :

| Variable | Type | Description |
|----------|------|-------------|
| `STAGING_SSH_PRIVATE_KEY` | File, Protected | Clé SSH privée pour connexion au serveur |
| `STAGING_SSH_KNOWN_HOSTS` | Variable, Protected | Sortie de `ssh-keyscan <STAGING_HOST>` |
| `STAGING_USER` | Variable | Utilisateur SSH (ex: `deploy`) |
| `STAGING_HOST` | Variable | Hostname ou IP du serveur de staging |

---

## Fichiers modifiés / créés

| Fichier | Type | Description |
|---------|------|-------------|
| `.gitlab-ci.yml` | Modifié | Pipeline complet 6 stages |
| `back/build.gradle` | Modifié | Ajout SpotBugs + JaCoCo |
| `front/package.json` | Modifié | Ajout dépendances ESLint + @angular-eslint |
| `front/.eslintrc.json` | Créé | Configuration ESLint pour Angular |
| `front/angular.json` | Modifié | Ajout cible `lint` pour `ng lint` |
| `front/karma.conf.js` | Modifié | Ajout reporters `lcovonly` + `cobertura` |
| `docker-compose.staging.yml` | Créé | Environnement de staging |
| `README-cicd.md` | Créé | Ce document |

---

## Première mise en place

### 1. Installer les dépendances ESLint Angular

```bash
cd front
npm install
```

### 2. Vérifier que ESLint fonctionne en local

```bash
cd front
npx ng lint
```

### 3. Vérifier que SpotBugs fonctionne en local

```bash
cd back
./gradlew spotbugsMain
# Rapport : back/build/reports/spotbugs/main.html
```

### 4. Vérifier que JaCoCo fonctionne en local

```bash
cd back
./gradlew test jacocoTestReport
# Rapport : back/build/reports/jacoco/test/html/index.html
```

### 5. Configurer les variables CI/CD dans GitLab

Aller dans `Settings → CI/CD → Variables` et ajouter les 4 variables de déploiement.

### 6. Générer la paire de clés SSH pour le déploiement

```bash
ssh-keygen -t ed25519 -C "gitlab-ci-deploy" -f gitlab-deploy-key
# Ajouter la clé publique sur le serveur de staging :
ssh-copy-id -i gitlab-deploy-key.pub deploy@<STAGING_HOST>
# Récupérer les known_hosts :
ssh-keyscan <STAGING_HOST>
```

### 7. Déployer manuellement le docker-compose sur le serveur de staging

```bash
scp docker-compose.staging.yml deploy@<STAGING_HOST>:/opt/orion-ucrm/
```

---

## Environnement de staging local (développement)

Pour tester le staging localement sans serveur dédié :

```bash
# Définir les variables
export REGISTRY_IMAGE=registry.gitlab.com/mon-groupe/orion-ucrm
export IMAGE_TAG=latest

# Se connecter au registry
docker login registry.gitlab.com

# Lancer l'environnement
docker compose -f docker-compose.staging.yml up -d

# Voir les logs
docker compose -f docker-compose.staging.yml logs -f

# Arrêter
docker compose -f docker-compose.staging.yml down
```

---

## Points d'attention

### Port du backend (Dockerfile)

Le `Dockerfile` contient `EXPOSE 4200` pour le backend alors que Spring Boot démarre sur le port **8080** par défaut. Cette instruction `EXPOSE` est documentaire et n'affecte pas le port réel d'écoute, mais peut prêter à confusion. Le `docker-compose.staging.yml` mappe correctement le port 8080.

### Images Alpine et CVEs

Les images `alpine:3.19` peuvent contenir des CVEs HIGH/CRITICAL selon l'état de la base de données Trivy. Si le scan bloque sur des CVEs provenant de l'image de base :
- Mettre à jour la version Alpine (`alpine:3.20` ou `alpine:latest`)
- Ou ajouter une liste d'exceptions Trivy : `.trivyignore`

### Cache Trivy

La base de données CVE de Trivy est mise en cache dans `.trivy-cache/`. Ce cache est partagé entre les 3 jobs de scan via la clé `trivy-db-cache`. En cas de problème, supprimer le cache depuis `CI/CD → Pipelines → Clear Runner Caches`.
