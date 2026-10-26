# AGENTS.md — OpenSILEX

## Skills

Versionnés dans `.claude/skills/` (exceptions explicites du `.gitignore` ; le reste de `.claude/` reste ignoré).

| Skill | Usage |
|---|---|
<!-- | `opensilex-java` | Écrire, corriger, linter du Java (api/bll/dal, DAO SPARQL, JAX-RS, DTO, migrations, JUnit 4) |
| `opensilex-vue` | Créer, migrer Vue 2 → 3 ou relire un composant `.vue` |
| `opensilex-vue3` | Plateforme front : modules, routes, `$opensilex`, i18n, build UMD |
| `opensilex-review` | Revue de code d'une branche, d'une MR ou d'un commit | -->
| `opensilex-mr` | Rédiger la MR (titre Conventional Commit, front matter changelog, template) |
<!-- | `graft`, `codebase-memory` | Navigation dans le code via graphe | -->

## Setup

- JDK 17 (`java.compiler.version`), Maven 3.9.9, Docker + compose.
- Node v22.15.1 / npm 11.1.0 (`opensilex-parent/pom.xml`). Gestionnaire de paquets : npm (`package-lock.json`).
- Pas de variable d'env obligatoire hormis `JAVA_HOME` → JDK 17. La configuration se fait dans `opensilex-dev-tools/src/main/resources/config/opensilex.yml`, où `file-system.storageBasePath` est obligatoire.

## Commands

```bash
mvn clean install                                   # build complet + tests (requis avant review)
mvn clean install -DskipTests
mvn install -DskipFrontBuild=true -DskipFrontLibInstall=true -DskipFrontTypesGeneration=true   # sans front
mvn -pl opensilex-sparql,opensilex-core test -Dtest='FooTest' -Dsurefire.failIfNoSpecifiedTests=false
docker compose up -d                                # dans opensilex-dev-tools/src/main/resources/docker
opensilex dev install [-r]                          # init des données (-r = reset)
opensilex dev start [--no-front-dev]                # serveur (+ hot reload front par défaut)
cd opensilex-front/front && npm run serve           # front seul ; npm run build pour le bundle
python3 ~/.claude/skills/opensilex-java/scripts/lint.py [--fix]        # lint Java
.claude/skills/opensilex-vue/scripts/check-vue-conventions.sh          # lint Vue
```

Il n'y a ni typecheck ni lint front fiable : `npm run lint` appelle `vite lint`, qui n'est pas un linter.

## Code style

- Java : formatage Google Java Style ; classes en PascalCase, membres en camelCase ; couches `api/` (`*API`, `*DTO`), `bll/` (`*Logic`), `dal/` (`*Model`, `*DAO`).
- JSON en snake_case : les champs des DTO restent en camelCase et portent `@JsonProperty("snake_case")`.
- Vue 3 : `<script setup lang="ts">`, composants en PascalCase enregistrés en `opensilex-<Name>`, Naive UI (pas Bootstrap), i18n en/fr, aucun `any`.
- Imports front : alias `@/…` et `opensilex-core/…` plutôt que des chemins relatifs. Guillemets : suivre le fichier.
- Interdit : code mort ou commenté, `console.log`, modifier un DTO sans régénérer le client TS.

## Repo structure

- `opensilex-parent/pom.xml` : versions et plugins.
- `opensilex-main` (socle, config), `-sparql` (ORM RDF), `-nosql` (Mongo), `-fs`, `-security` (auth), `-core` (métier).
- Concept métier : `<module>/src/main/java/org/opensilex/<module>/<concept>/{api,bll,dal}`. Les routes API sont les `*API.java` de `api/`.
- Tests : `<module>/src/test/java` (JUnit 4).
- Front : `opensilex-front/front/src/components`, routes et menus dans `opensilex.front.yml`, i18n dans `src/lang/message-{en,fr}.json`.
- Migrations : `opensilex-migration/src/main/java/org/opensilex/migration/`.
- Outils de dev (Install, StartServer, docker, config) : `opensilex-dev-tools`. Documentation VitePress : `opensilex-doc`.
- Modules d'extension : `opensilex-brapi`, `-faidare`, `-graphql`, `-phis`, `-ai-import`, `inrae-*`, `module-skeleton`.

## Commit workflow

- Commits et titre de MR au format Conventional Commits, en anglais : `type(scope): message` (types autorisés : build, chore, ci, docs, feat, fix, perf, refactor, revert, style, test). La CI vérifie le titre de MR.
- Branches : `feature/<nom>` → `develop` (imposé par la CI), `hotfix/<nom>` → `master`.
- La description de MR suit `.gitlab/merge_request_templates/` et son front matter YAML contient `changelog:` (en anglais) ou `ignore-changelog: true`.
- Avant push : `mvn clean install` vert, lint propre.

## Do not touch

- Fichiers générés : `**/front/src/lib` (client TS issu du swagger), `**/front/types`, `**/front/dist`, `target/`, `opensilex-doc/src/main/resources/generated/`, `dependency-reduced-pom.xml`, `graft/`.
- Secrets et configuration locale : `opensilex-dev-tools/src/main/resources/config/*.yml`, `opensilex-front/front/src/opensilex.dev.ts`.
- Migrations déjà livrées : ne pas les modifier, en ajouter une nouvelle.
- Versions figées (Java, Node, npm, dépendances) dans `opensilex-parent/pom.xml` et `package-lock.json` : ne pas les changer sans demande explicite.
