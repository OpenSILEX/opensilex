# Technical documentation : [`tooling`] Claude Code skills

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)         | OpenSILEX version | Comment                                                           |
|------------|-------------------|-------------------|-------------------------------------------------------------------|
| 2026-10-09 | Arnaud Charleroy  | develop           | Document creation: skills `opensilex-java`, `opensilex-review`, `opensilex-mr` |

## Table of contents

<!-- TOC -->
* [Technical documentation : [`tooling`] Claude Code skills](#technical-documentation--tooling-claude-code-skills)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Overview](#overview)
  * [Using the skills](#using-the-skills)
  * [The skills](#the-skills)
    * [opensilex-java](#opensilex-java)
    * [opensilex-review](#opensilex-review)
    * [opensilex-mr](#opensilex-mr)
  * [How the skills work together](#how-the-skills-work-together)
  * [Rules shared by all skills](#rules-shared-by-all-skills)
  * [Maintaining the skills](#maintaining-the-skills)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Claude Code** : the AI coding assistant (terminal, IDE extension or desktop app) that can read the repository, run
  commands and edit files.
- **Skill** : a folder `.claude/skills/{name}/` containing a `SKILL.md` (a front matter with the skill `name` and a
  `description` saying when to use it, then the instructions), and optionally `references/` (documents read on demand),
  `scripts/` (deterministic helpers) and `assets/` (templates). Claude Code loads a skill when the request matches its
  description, or when it is called by name (for example `/opensilex-java`).
- **Project skill** : a skill committed in the repository, so that every developer gets the same conventions and the
  same tools. The three skills below are project skills.

## Overview

| Skill              | Question it answers                                         | Typical requests                                                                          |
|--------------------|-------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| `opensilex-java`   | How do I write, check and fix Java code the OpenSILEX way?  | "add an endpoint", "new model", "review this class", "fix the lint", "run SonarLint", "it does not compile" |
| `opensilex-review` | What should change before this branch or MR is merged?      | "review my branch", "is it mergeable?", "check the test coverage", "any performance issue?" |
| `opensilex-mr`     | How do I describe this change in a merge request?           | "write my MR", "fill the template", "changelog entry", "title of the MR"                  |

Each skill also answers French requests (for example "ajoute un endpoint", "relis ma branche", "rédige ma MR").

## Using the skills

1. Open the repository root with Claude Code. The skills in `.claude/skills/` are discovered automatically; nothing to
   install.
2. Ask for what you want in your own words: the skill that matches is loaded by itself. To force one, call it by name
   (`/opensilex-java`, `/opensilex-review`, `/opensilex-mr`).
3. The scripts bundled with a skill can also be run by hand from the repository root, without Claude Code (examples
   below).

Prerequisites shared by the scripts: `git`, Python 3.8 or later. Per skill:

| Skill              | Additional prerequisites                                                                                     |
|--------------------|--------------------------------------------------------------------------------------------------------------|
| `opensilex-java`   | JDK matching `java.compiler.version` (17), Maven; a JDK 21 or later for the SonarLint engine only            |
| `opensilex-review` | `git`; JaCoCo reports only for the optional coverage check of the changed lines                              |
| `opensilex-mr`     | `git`; read access to `origin` to fetch the target branch                                                    |

## The skills

### opensilex-java

**Purpose** : write, review, fix and lint Java code so that it looks like it was written by the team, for the Java
release defined in the project.

**Modes**

| Mode      | What it does                                                                                                                          |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------|
| Create    | Scaffolds a complete concept (model, search filter, DAO, three DTOs, API, API test, optionally business logic and migration) from templates that compile with JDK 17 |
| Review    | Mechanical lint on the changed lines, then a severity-ranked checklist (update that erases data, missing authentication or credential, layer violations, N+1 on lazy proxies, transactions, swallowed exceptions, migration safety, tests) |
| Fix       | Minimal diffs in severity order, mechanical fixes applied only to the reported lines, then compile and targeted tests                  |
| Lint      | Project conventions, the project Checkstyle configuration and SonarLint, merged in one report                                         |
| Build     | Maven commands that always run under the JDK matching the project release (the default `java` is often too old)                       |

**What it knows** (measured on the code base, not copied from a style guide)

- Naming and layering: `org.opensilex.{module}.{concept}` with `api`, `bll` and `dal` packages, class suffixes
  (`Model`, `DAO`, `SearchFilter`, `Logic`, `API`, `DTO`, `CreationDTO`, `UpdateDTO`...), constants, loggers, test names.
- REST conventions: annotation order, `@ApiOperation` wording, snake_case query parameters, credentials, batch lookups.
- Formatting: 4 spaces, K&R braces, line length, import order, the license header of new files.
- The Java policy: which Java 17 features the code base uses, which ones to avoid, and what must not be modernised.
- SPARQL models and DAOs, update semantics, modules, configuration, migrations, JUnit 4 integration tests.

**Tools** (in `.claude/skills/opensilex-java/scripts/`, run from the repository root)

| Script                       | Role                                                                                                    |
|------------------------------|---------------------------------------------------------------------------------------------------------|
| `jdk.sh`                     | Reads the Java release from `opensilex-parent/pom.xml`, finds the matching JDK and runs Maven with it    |
| `lint.py`                    | Conventions linter + Checkstyle + SonarLint; changed lines only by default; `--fix` for mechanical fixes |
| `scaffold.py`                | Generates a new concept from `assets/templates/`                                                        |
| `sonarlint/sonarlint.sh`     | Headless SonarLint: same engine and Java analyzers as the IDE plugins, no server, no telemetry           |

```bash
python3 .claude/skills/opensilex-java/scripts/lint.py --sonar
python3 .claude/skills/opensilex-java/scripts/scaffold.py --concept SensorKit --prefix skit --dry-run
.claude/skills/opensilex-java/scripts/jdk.sh mvn -o -q -pl opensilex-core compile -DskipFrontBuild
```

**Good to know** : SonarLint uses the default "Sonar way" profile plus a few overrides
(`.claude/skills/opensilex-java/scripts/sonarlint/sonarlint-rules.json`). The CI uses the INRAE SonarQube quality
profile; to get the same rules in an IDE, bind it to that server in connected mode (see the README of the tool). The
first run downloads the SonarLint libraries (about 72 MB) after asking, or reuses an installed SonarLint plugin of
IntelliJ of the same version.

### opensilex-review

**Purpose** : review a change (current branch, a colleague's branch, a commit range, a merged MR or named files) and
tell the author what should change before merging, measured against what the team actually does.

**Five axes** : maintainability, homogeneity with the existing code, clarity, test coverage, performance. Java (api,
bll, dal, SPARQL and Mongo DAOs, JAX-RS, migrations, JUnit 4 tests) and the Vue 2 / TypeScript front (component
guidelines, i18n en/fr) are covered.

**Report** : a verdict first, then confirmed findings ranked *Bloquant*, *Important*, *Suggestion*, *Question*, each
with `path:line`, the norm it breaks (a sibling file or a team guideline) and a concrete fix, then a table of the five
axes and what was not verified. A review only reports; it fixes when asked, by handing over to `opensilex-java` (Java)
or by minimal edits following the front guidelines.

**Tools** (in `.claude/skills/opensilex-review/scripts/`)

| Script                | Role                                                                                                            |
|-----------------------|-----------------------------------------------------------------------------------------------------------------|
| `review_scope.py`     | Fixes the scope, lists the files by layer and module, the siblings to compare with, a map of changed endpoints and the tests that call them, and leads to confirm |
| `coverage_changed.py` | JaCoCo coverage of the changed lines (only for the checked-out sources and the tests that were run)              |

```bash
python3 .claude/skills/opensilex-review/scripts/review_scope.py
python3 .claude/skills/opensilex-review/scripts/review_scope.py --base origin/develop --head HEAD
```

For large changes the skill splits the work (one subagent per axis or module) and re-checks every blocking finding
itself. It uses the lint of `opensilex-java` for the mechanical Java checks. It never posts comments on GitLab and
never pushes.

### opensilex-mr

**Purpose** : write a merge request that passes the GitLab CI check (`merge-request:check`) on the first try and that a
reviewer who did not follow the work understands quickly: why the change exists, what the user sees, where to look first
and what is risky.

**What it produces**

- A Conventional Commit **title in English** (`type(scope): Description`, no space in the scope).
- The YAML **front matter**: a `changelog` entry in English from the user's point of view, or `ignore-changelog: true`
  for refactoring, docs, CI, tests-only changes and ports of a change already in a changelog.
- A **description in French** built on the team templates (`.gitlab/merge_request_templates`, bug fix or feature):
  context, before/after, changes grouped by intent, an honest checklist, review hints, test steps and deployment notes.

**Tools** (in `.claude/skills/opensilex-mr/scripts/`)

| Script         | Role                                                                                                                  |
|----------------|-----------------------------------------------------------------------------------------------------------------------|
| `mr_facts.py`  | Facts from git: branches and CI rules, commits with their bodies, files by module, signals (endpoints, DTO fields, migrations, credentials, configuration...), suggested type, scope and changelog decision |
| `check_mr.py`  | Validates a draft against the CI rules and the template: errors (CI would reject it, placeholders left), warnings (conventions, readability) |

```bash
python3 .claude/skills/opensilex-mr/scripts/mr_facts.py
python3 .claude/skills/opensilex-mr/scripts/check_mr.py .git/mr-drafts/{draft}.md
```

Drafts are saved under `.git/mr-drafts/` (never committed). The skill pushes the branch or opens the MR only when
asked, and each step is confirmed.

## How the skills work together

A change normally goes through the three skills, in this order:

1. **Write** : `opensilex-java` (Create mode) scaffolds or edits the code; its lint and the compile and test commands
   check it.
2. **Review** : `opensilex-review` reviews the branch against `origin/develop` on the five axes, using
   `opensilex-java/scripts/lint.py` for the mechanical Java checks.
3. **Fix** : `opensilex-java` (Fix mode) applies the findings with minimal diffs.
4. **Describe** : `opensilex-mr` writes the title, the changelog front matter and the description, and validates them
   against the CI rules.

Each skill can be used alone.

## Rules shared by all skills

- **Read first, then change** : the nearest sibling file is the reference; no drive-by reformatting, renames or version
  bumps (see `CONTRIBUTING.md`).
- **Outward actions only on request** : no push, no merge request, no GitLab comment unless the user asks, and each
  step is confirmed.
- **Say what was not verified** : tests not run, manual tests unknown, coverage not measured.
- **No secrets** : `~/.m2/settings.xml` and the CI variables (SonarQube token, OSS Index account) must never be printed,
  copied or committed.
- **Generated files are not edited** : `front/src/lib`, `front/types`, `target/`.

## Maintaining the skills

**Location and versioning** : skills live in `.claude/skills/{name}/`. The `.gitignore` ignores `.claude/` except the
folders explicitly re-included (one line `!/.claude/skills/{name}/` per skill), so personal files such as
`.claude/settings.local.json` are never committed.

**Adding a skill**

1. Create `.claude/skills/{name}/SKILL.md` with a front matter (`name`, and a `description` that says what the skill does
   and lists the situations and phrases that should trigger it, in English and in French), then short instructions.
   Put long material in `references/` and deterministic work in `scripts/`; reference every file from `SKILL.md`.
2. Re-include the folder in `.gitignore`.
3. Test the scripts on real data, and say in the skill what was not tested.
4. Add the skill to this page and to the "Claude Code skills" section of the main `README.md`.

**Keeping them true** : the skills describe the code as it is. When a convention changes (Java release, annotation
order, DTO naming, CI rules, merge request template), update the matching reference file in the same merge request. The
Java policy of `opensilex-java` is tied to `java.compiler.version`: when the release changes, re-validate its
`java17-practices.md` reference. `opensilex-mr` reads the current template files at run time instead of copying them.

**Debugging** : each script has `--help`. `lint.py` and `sonarlint.sh` accept explicit file paths, and
`OPENSILEX_LINT_REPO` points them at another checkout.

## Limitations and improvements

- Skills are a Claude Code feature: other assistants ignore the `.claude/skills/` folder, but the scripts run alone.
- The conventions of `opensilex-java` were measured on the code base on 2026-10-09; the code has a long history, so
  the skill distinguishes current practice from legacy and never asks for mass clean-ups.
- The linter rules are heuristics, not a parser: a finding can be a false positive and must be read before acting.
- SonarLint runs with the default profile, not the INRAE SonarQube one; its download path from Maven Central has not
  been executed by the author (the path through an installed IntelliJ plugin has). Linux and macOS only.
- `opensilex-review` cannot read a GitLab MR by number (no API access): it needs the source branch.
- Compiled Python caches (`__pycache__`) can appear in the skill folders when scripts are imported: they must not be
  committed.

## Documentation

- see the general architecture and code organisation: `../architecture/code-organization.md`
- see the tests documentation: `../architecture/tests.md`
- see the contribution rules and the merge request workflow: `CONTRIBUTING.md` at the repository root
- see the merge request templates: `.gitlab/merge_request_templates/`
- see the SonarLint tool: `.claude/skills/opensilex-java/scripts/sonarlint/README.md`
- see each skill entry point: `.claude/skills/opensilex-java/SKILL.md`, `.claude/skills/opensilex-review/SKILL.md`,
  `.claude/skills/opensilex-mr/SKILL.md`
