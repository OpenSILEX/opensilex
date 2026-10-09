---
name: opensilex-mr
description: Write and prepare an OpenSILEX GitLab merge request that a reviewer can read in two minutes - a Conventional Commit title in English that passes the CI check, the YAML front matter (`changelog` in English from the user's point of view, or `ignore-changelog: true`), and a description built on the team template (.gitlab/merge_request_templates bug_fix1.md / feature1.md, in French) with context, before/after, changes grouped by intent, an honest checklist, review hints, test steps and deployment notes. Gathers facts from git (commits, files, endpoints, DTO fields, migrations, credentials, config), validates the draft against the CI rules and the template, saves it under .git/mr-drafts and only pushes or opens the MR when asked. Use whenever the user asks to write, draft, prepare, fill, rewrite or check a merge request, MR, pull request, PR description or changelog entry, e.g. "rédige ma MR", "prépare la merge request", "description de MR", "remplis le template", "titre de MR", "entrée changelog", "ouvre la MR", even when GitLab is not named.
---

# OpenSILEX merge request

Goal: a merge request that passes the GitLab CI check (`merge-request:check`) on the first try and that a reviewer
who did not follow the work understands quickly: **why** the change exists, **what the user sees**, **where to look
first** and **what is risky**. Writing rules and examples: `references/writing-guide.md`; a finished draft:
`references/example-fix.md` with `references/example-fix.title`.

All paths are relative to `.claude/skills/opensilex-mr/`; run the scripts from the repository root.

## 1. Gather the facts
```bash
git fetch origin develop        # updates origin/* refs only (network, working tree untouched); real target:
                                # master for hotfix/*, vue3/main for vue3/*
python3 .claude/skills/opensilex-mr/scripts/mr_facts.py [--target <branch>] [--json]
# another branch or a past commit, without switching the checkout:
python3 .claude/skills/opensilex-mr/scripts/mr_facts.py --base <base> --head <ref> --source <branch-name>
```
It reports: source and target branches, the CI rules the branch breaks (only `feature/*` into `develop`/`release`,
only `hotfix/*` into `master`), push state, commits of the target missing from the branch, uncommitted changes (not
part of the MR), commits **with their bodies**, **ports** (cherry-picks, and changes whose `(!NNNN)` is already in a
`CHANGELOG.md`), files by module and kind, **signals** (endpoints and HTTP verbs, DTO fields, SPARQL mapping,
migrations, credentials, configuration, ontology, dependencies, tests, generated files, front), a suggested type,
scope and changelog decision, the template to use, the draft paths and the "new MR" URL.

Stop and tell the user before writing when:
- a **CI rule** is broken (wrong branch prefix for the target): renaming the branch is their call;
- there are **uncommitted changes** that look part of the work: ask whether to commit them first;
- the target has moved ahead: suggest merging it into the branch (team rule: merge, not rebase), only do it if asked;
- **generated files** were edited by hand (`front/src/lib`, `front/types`).

## 2. Understand the change
Read the diff by intent (`git diff <base> <head>`): tests first (they state the expected behaviour), then the main
code. Use the signals table of `references/writing-guide.md`: each signal has a place in the description. If the user
wants a review, the `opensilex-review` skill does it. A problem you notice while writing (a bug, an inconsistency) is
not hidden in the MR: tell the user in the hand-over; it goes into "Points d'attention" only when the user keeps it
as a known limit of this change.

What git cannot tell you, ask in **one** short message (only what is missing): the bug report or the need, the Trello
card or issue link, whether they tested manually, whether the spec was validated. Do not invent any of it. If the user
prefers not to answer, leave `<!-- À compléter : ... -->` markers: `check_mr.py` keeps the draft red until they are
resolved.

## 3. Write
1. **Template**: read the current file the facts point to (`bug_fix1.md` for `fix`, `feature1.md` otherwise). Never
   reproduce the template from memory: it changes.
2. **Title** (English, CI pattern): `type(scope): Description`: the precise user-visible effect, scope without spaces,
   ASCII, under ~90 characters. Prefix `Draft: ` when the user says it is not ready.
3. **Front matter** first in the file: `changelog: |-` + 1 to 3 English sentences from the user's point of view, or
   `ignore-changelog: true` alone for refactor/docs/CI/tests-only changes **and for ports of a change already in a
   `CHANGELOG.md`** (`mr_facts.py` says so; explain it in Points d'attention). Remove the template's YAML comments.
4. **Checklist**: every template item kept, in order; tick only what is true (`Relecture MR` never; tests only if
   they ran green). An item that does not apply stays **unticked** with a note after the template text:
   `(non concerné)`, `(pas de tests front automatisés)`. Full rules: writing guide, "Checklist".
5. **Body in French**, following the template sections: Contexte (need, cause for a fix, link), Avant/Après (fix),
   Changements grouped by intent with sub-sections named after their topic (never "Sous-partie 1"), and under Autres
   only what has content: Points d'attention pour la relecture, Comment tester, Déploiement, Liens. Remove every
   example line, example link and empty optional heading of the template.
6. Length (non-empty lines after the front matter): small fix 20-40, feature 40-90. Over 120, say the MR is probably
   too big.

## 4. Save and validate
Save the description and the title to the two paths printed by `mr_facts.py` under "draft" (the branch name is
slugified, e.g. `.git/mr-drafts/feature_dev_fix_name.md` and `.title`; inside `.git/`, never committed). Create the
folder first: `mkdir -p "$(git rev-parse --git-path mr-drafts)"`. Then:
```bash
python3 .claude/skills/opensilex-mr/scripts/check_mr.py <draft>.md            # title read from <draft>.title
# for another branch or a past commit: add --base <base> --head <ref> --source <branch-name>
```
ERROR = the CI would reject it, or placeholders/markers remain; WARN = team conventions or readability. Fix and re-run
until there is no ERROR; fix WARNs unless you explain why one stays. An INFO about a cited file outside the diff is
fine when the file is deliberate context (a sibling, the ontology); otherwise it is a typo.

## 5. Hand over
Show the user, in chat: the title in a code block, then the full description in a ```markdown block (ready to copy),
then the draft path, what you did not verify (tests not run, manual test unknown...) and the open questions. Offer
(do not do it unasked) to copy it: `xclip -selection clipboard < <draft>.md`.

## 6. Publish (only on explicit request, each step confirmed)
Pushing and creating the MR are outward actions: ask before each, show what will happen, never force-push.
1. Push: `git push -u origin <branch>`.
2. Create: open the "new MR" URL printed by `mr_facts.py`, paste the title and the description, and leave the
   template selector empty (choosing a template replaces the pasted text). No `glab`/`gh` is installed here, and the
   GitLab API needs a token you must not handle.
   Alternative in one push: `git push -u origin <branch> -o merge_request.create -o merge_request.target=develop
   -o merge_request.title="<title>" -o merge_request.draft`, then paste the description in the MR (git push options
   cannot carry multi-line text). The CI check reads the description when it runs: if it ran before the description
   was pasted, re-run the `merge-request:check` job.
3. Updating an existing MR: rewrite the draft from the current facts; the user pastes it.

## Traps
- The CI parses the front matter only when it is the **very first line** of the description (`---`).
- `changelog` filled **and** `ignore-changelog: true` fails the CI; both empty fails too.
- The CI title regex allows no space in the scope: `fix(Scientific Objects)` fails. It tolerates accents, but titles
  are English and ASCII by team convention (`check_mr.py` warns).
- A port of a released hotfix (cherry-pick, `!NNNN` in the commit body) must not add a second changelog entry.
- The MR contains **committed** work only: `mr_facts.py` reads `<merge-base>..HEAD`.
- Both templates link the spec template at `../../opensilex-doc/src/main/resources/specs/template/spec_template.md`;
  the file is now `opensilex-doc/src/main/resources/functional-specifications/template/spec-template.md`. Keep the
  checklist line as the template has it (the team owns the template); mention it if the user asks.
- `CHANGELOG.md` is compiled at release time from the MR front matter: a feature MR normally does not edit it.

## Files
`scripts/mr_facts.py` (facts from git), `scripts/check_mr.py` (CI rules, template, placeholders, readability),
`references/writing-guide.md` (how to write each part, signal-to-section map, examples),
`references/example-fix.md` + `example-fix.title` (a complete bug-fix MR that passes `check_mr.py`).
