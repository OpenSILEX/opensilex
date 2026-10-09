# Writing a merge request people want to read

Sources: the team's `mr_redaction.md` (dev-tools repository, `docs/workflow/conventions/`), the templates in
`.gitlab/merge_request_templates/`, the `merge-request:check` job of `.gitlab-ci.yml`, `CHANGELOG.md`.
A finished example: `example-fix.md` (+ `example-fix.title`), written as the original MR of the fix (!1570). Its port
to `develop` (commit `43a08ce95`) would instead use `ignore-changelog: true`, see "Ports".

## Readers and languages
Three readers, in this order:
1. **The reviewer**, who did not follow the work: what problem, what changed, where to look first, what is risky.
2. **The developer who will `git blame` this in a year**: why it was done this way.
3. **Users and administrators**, through the changelog entry only.

| Part | Language | Why |
|---|---|---|
| Title | English | CI pattern, becomes the merge commit subject |
| `changelog` front matter | English | copied into `CHANGELOG.md` |
| Body (Contexte, Changements, Autres) | French, like the template | team language; follow the user if they want English |
| Code identifiers, paths, endpoints | unchanged, in backticks | searchable |

## Principles
- **Why before what.** The diff already says what. The description says why, what the user sees, and what was
  decided and why (a rejected alternative is worth one sentence).
- **Observable and verifiable.** "La liste des germplasms s'affiche en 2 s au lieu de 12 s sur la base de référence"
  only if it was measured. Never invent figures, test results, card links, reviewers or reasons. Unknown -> ask, or
  leave an `<!-- À compléter : ... -->` marker that `check_mr.py` refuses until it is resolved.
- **Group by intent, not by file.** Sub-sections named after what they change ("Requête de suppression", "Formulaire
  de création", "Migration"), files only where they need explaining. Files needing no comment are grouped in one
  bullet ("traductions en/fr", "imports").
- **Short.** Counted in non-empty lines after the front matter: small fix 20 to 40, feature 40 to 90. Over 120, the
  MR is probably too big: say so.
- **Plain style.** No filler ("Cette MR a pour but de..."), no marketing, no emojis, no paraphrase of each hunk.
  One idea per sentence. Bold for the one word that matters in a sentence, not for whole lines.

## Title
`type(scope): Description`, checked by the CI with a regex: type in `build chore ci docs feat fix perf refactor
revert style test`, optional scope **without spaces** (`[A-Za-z0-9._-]`), `!` for a breaking change, then `: ` and a
description starting with a letter or digit, ASCII only. `Draft: ` in front while not ready (the CI then skips the
build).
- **Type**: `fix` for a bug (branch `feature/<dev>/fix/...` or `hotfix/...`), `feat` for a feature, `docs`, `test`,
  `refactor`, `perf`, `ci`, `build`.
- **Scope**: the domain, one word as in recent history (`facility`, `germplasm`, `sparql`, `API`, `Variables`,
  `Migration`, `OS`...). `mr_facts.py` proposes candidates from the commits and the touched concepts.
- **Description**: precise, ideally the user-visible effect, under ~90 characters, no final period. Team examples:
  - bad: `Fix import`
  - good: `fix(data): Fixed target column being able to be put at end during data import`
  - good: `fix(facility): Adding custom relations to facilities now works again`
  - good: `feat(Front): Added support for Matomo analytics`

## Front matter (`changelog` / `ignore-changelog`)
The description must **start** with it (the CI reads it from the first line). Exactly one of:
```yaml
---
changelog: |-
  You can now search scientific objects by applying filters on their associated data, for example
  "all objects with air_temperature between 10 and 20 °C".
ignore-changelog: false
---
```
```yaml
---
ignore-changelog: true
---
```
- **Fill it** for anything a user, an administrator or an API client can notice: feature, fix, behaviour change,
  performance gain, configuration or migration to run, API contract change.
- **Ignore it** (`ignore-changelog: true`, no `changelog` key) for refactoring, developer documentation, CI, tests
  only, internal tooling. `mr_facts.py` suggests it for `docs/ci/test/refactor/style/chore/build`.
- **Ports** (a hotfix of `master` cherry-picked to `develop`, a change re-applied from another branch): when
  `mr_facts.py` finds its `(!NNNN)` already in a `CHANGELOG.md`, ignore the changelog and say in "Points d'attention"
  which MR is ported and in which version it was released. A port mixed with new work gets an entry for the new work
  only.
- English, **from the user's point of view**: "You can now ...", "... now works correctly", "... no longer ...".
  One to three sentences; a short Markdown list when several things change. Use `|-` and wrap lines.
- Do not add the `(!1234) [Scope]` prefix: it is added when `CHANGELOG.md` is compiled for a release.
- API-only change: name the service or the field, API clients are the audience (`CHANGELOG.md` has
  "Changed the format of `LocationObservationDTO` dates from number to string").
- Remove the template's YAML comments (`# Remplir cette section...`).

## Checklist
Keep every item of the template, in order, with its links (the reviewers rely on it; `check_mr.py` compares). An
item that does not apply stays **unticked**, with a short note after the template text (`(non concerné)`): the box
then reads as "nothing to do", not as "done". `check_mr.py` accepts any note appended after the template text.

| Item | Tick it when | Does not apply |
|---|---|---|
| Relecture MR | never: the reviewer ticks it | - |
| Tests écrits et OK | tests were added or updated for the change **and** ran green (in this session, or the user says so) | front-only change: `(pas de tests front automatisés)` + "Comment tester" |
| Documentation technique | documentation was written or updated | no behaviour, configuration or API change: `(non concerné)` |
| Specifications fonctionnelles validées | the user says the spec was validated | a fix that restores the specified behaviour, no functional rule changed: `(non concerné)` |
| Testé | the user says they tested it manually | - (leave it for the user) |
| Remplir l'entrée changelog ou la marquer comme ignorée | once the front matter is done | - |
| Les composants Vue suivent les bonnes pratiques (**feature template only**) | `.vue` files changed and were checked against the guidelines | no `.vue` changed: `(non concerné)` |

The bug-fix template has no Vue item: do not add one; a fix touching `.vue` files gets a "Comment tester" section.
Never tick on assumption. An unticked item the user should act on goes in "Points d'attention".

## Contexte
Two to five sentences: the need or the bug, who is affected, and for a fix **the cause**. Then the Trello card / issue /
spec link if the user gave one (otherwise remove the placeholder link; never leave `https://trello.com/`). For a
feature, the template's "Sous partie 1" becomes a sub-section **named after its topic** ("## Modèle de données",
"## Choix techniques") or disappears.

## Avant / Après (bug fix template)
Mirrored bullets per layer, observable behaviour only: **Front**, **API** (with the endpoint), **Migration**,
**Performance** (with a measured figure, or not at all). The "Avant" of a fix is the bug report, one line per layer.

## Changements
- One sub-section per intent; inside, `` `File.java` `` : what changed and why it was needed, one or two lines.
- Name the decisive method when it helps the reviewer (`SPARQLClassQueryBuilder#getDeleteBuilder`).
- Tests get their own sub-section: which scenario each new test covers.
- About 15 file bullets at most: beyond that, describe components, not files.

## Autres (only the sub-sections that have content)
- **Points d'attention pour la relecture**: where to start reading, the risky part, what is deliberately out of scope,
  known limits, any unticked checklist item that matters.
- **Comment tester**: numbered steps with the data to use and the expected result; required when the front changes
  (there are no automated front tests).
- **Déploiement**: migration to run (class name, `opensilex-doc/src/main/resources/how-to/migration_command.md`),
  configuration keys added (and their documentation under `installation/configuration`), credentials to grant.
- **Liens**: real links only (spec, issue, external doc). Remove the template's example links.
- **Glossaire**, **Carte trello liées**: only when there is something to put in them; otherwise remove the heading.

## Facts from `mr_facts.py` that must appear in the description

| Signal | Where it goes |
|---|---|
| `endpoints_paths_added/removed`, `endpoints_http_verbs` | Changements (API) + Points d'attention: contract change for API clients and the generated TypeScript client; breaking -> `!` in the title, and say it in the changelog |
| `dto_fields_added/removed`, `dto_json_names_*` | Changements (API): field names; removed or renamed fields are breaking |
| `sparql_mapping_changed` | Changements + Points d'attention: the stored data shape changes; is a migration needed? |
| `migrations_added` | Déploiement: class and command; the changelog mentions the manual operation |
| `credentials_added` | Déploiement: which profiles must receive the new credential |
| `config_options_added`, `config_files_changed` | Déploiement + documentation checklist item |
| `ontology_changed` | Contexte or Changements: which term changed and why |
| `dependencies_changed` | Changements: version and reason (security, feature, bug) |
| `tests_added` / no test file changed | "Tests" checklist item + Tests sub-section; a fix without test -> say why in Points d'attention |
| `tests_ignored` | Points d'attention: why each `@Ignore` |
| `front_touched` | Comment tester; Vue guidelines checklist item (feature template only) |
| `ported` (cherry-pick, `(!NNNN)` already released) | `ignore-changelog: true` + Points d'attention: ported MR and release version |
| `generated_files_touched` | should not happen: tell the user before writing |
| `changelog_md_edited` | `CHANGELOG.md` is compiled at release time: ask whether this edit belongs in the MR |
| `docs_changed` | "Documentation technique" item + Liens |

## The difference, in one example
Hard to review (paraphrases files, no why):
> - `SPARQLService.java` : modification de la méthode deleteCustomRelations
> - `SPARQLClassQueryBuilder.java` : ajout d'un paramètre
> - Tests modifiés

Readable (cause, effect, where to look):
> Mettre à jour une ressource supprimait aussi les relations que d'autres ressources avaient vers elle : après la
> modification d'une unité, les variables qui l'utilisaient perdaient leur lien `hasUnit`.
> - `SPARQLClassQueryBuilder.java` : la partie « relations inverses » de la requête de suppression est restreinte
>   aux prédicats gérés par le modèle.
