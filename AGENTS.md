# OpenSILEX

## Project goal

OpenSILEX is an open-source, collaborative information system for managing scientific research data, with a focus on
plant phenotyping. It is developed by the MISTEA joint research unit (INRAE). It stores experiments, scientific
objects, variables, germplasm, devices and measured data, and exposes them through a REST API (also BrAPI and
FAIDARE compatible) and a Vue.js web interface.

Stack: multi-module Maven project in Java 17; business data in an RDF4J triplestore through the in-house SPARQL
mapping layer (`opensilex-sparql`); time series and files in MongoDB; Jersey and Swagger for the API; Vue 2 and
TypeScript for the front, with a client generated from the Swagger output. Setup and build: `README.md`; contribution
rules: `CONTRIBUTING.md`.

## Skills to use

Project skills live in `.claude/skills/`. Use the one that matches the task instead of working from memory.

| Task                                                                                           | Skill              |
|------------------------------------------------------------------------------------------------|--------------------|
| Write, change, fix or lint Java (model, DAO, endpoint, DTO, migration, test); build or JDK problem | `opensilex-java`   |
| Review a branch, a commit range or a merge request                                             | `opensilex-review` |
| Write or check a merge request (title, changelog entry, description)                           | `opensilex-mr`     |

Usual order for a change: `opensilex-java` (write), `opensilex-review` (review), `opensilex-java` (fix),
`opensilex-mr` (merge request). There is no skill for the Vue front yet: follow
`opensilex-doc/src/main/resources/technical-documentation/opensilex-front/component-guidelines-template.md`.
What each skill does, prerequisites and maintenance:
`opensilex-doc/src/main/resources/technical-documentation/claude-code-skills/index.md`.

## Ground rules

- Read the closest sibling file first and imitate it; no drive-by reformatting, renames or version bumps.
- Run Maven through `.claude/skills/opensilex-java/scripts/jdk.sh mvn ...` (the default `java` is often older than the
  Java 17 the project requires); use `-DskipFrontBuild` for backend work.
- Do not push, open a merge request or post a GitLab comment unless asked.
- Never print or copy secrets (`~/.m2/settings.xml`, CI variables, tokens).
- Do not edit generated files (`**/front/src/lib`, `**/front/types`, `target/`).
- Say what was compiled or tested, and what was not.
