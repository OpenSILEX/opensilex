---
name: opensilex-review
description: Review OpenSILEX code changes - the current branch, a colleague's branch, a commit range, a merged MR or named files - on five axes (maintainability, homogeneity with the existing code, clarity, test coverage, performance), plus correctness problems found in the changed code, and report confirmed findings ranked Bloquant / Important / Suggestion / Question with path:line, the norm they break (sibling file, team guideline) and a concrete fix. Covers Java (api/bll/dal, SPARQL and Mongo DAOs, JAX-RS, migrations, JUnit 4 tests, JaCoCo coverage of the changed lines) and the Vue 2 / TypeScript front (component guidelines, i18n en/fr). Use whenever the user asks for a review, a code review, a "relecture", a "revue de code", "relis ma branche / ma MR", "est-ce que c'est mergeable", "qu'est-ce que tu penses de ces changements", "vérifie la couverture de tests", "y a-t-il des problèmes de perf", before opening a merge request, or to review a colleague's branch. Reports only; fixes only when asked.
---

# OpenSILEX review

A review answers one question for the author: **what must change before this is merged, and why**, measured against
what the team actually does (sibling files, Vue guidelines, team workflow rules), not a generic style guide.
Paths are relative to `.claude/skills/opensilex-review/`; run the scripts from the repository root.

## 1. Scope
| The user says | `review_scope.py` arguments |
|---|---|
| nothing / "ma branche" / "mes changements" (uncommitted + untracked included) | none |
| "ce qui est commité" | `--base origin/develop --head HEAD` |
| a colleague's branch | `git fetch origin <branch>`, then `--base $(git merge-base origin/develop origin/<branch>) --head origin/<branch>` |
| a merged MR or a commit | `--base <sha>~1 --head <sha>` (merge commit: `--base <sha>^1 --head <sha>`) |
| files or a directory | `--paths <paths>` |
| a hotfix | `--base $(git merge-base HEAD origin/master)` |

Never switch the user's checkout. A GitLab MR number alone cannot be read (no API access): ask for the branch.

## 2. Facts, then triage
```bash
python3 .claude/skills/opensilex-review/scripts/review_scope.py [args]          # Markdown (--json, --leads-only)
python3 .claude/skills/opensilex-java/scripts/lint.py --base <base>             # Java mechanics, working tree only
```
`review_scope.py` reads every file **at `--head`** and splits the change into **functional files** (read them) and
**mechanical files** (only call arguments, imports or whitespace change: skim them, then judge the cascade as a whole,
lead `MAINT-SIGNATURE-CASCADE`). It lists the siblings to compare with, a **test map** (changed endpoints and public
methods, a private method's changes attributed to its public callers, and the tests that reach them, package-checked)
and **leads**. Leads are hypotheses: confirm each one in the code or drop it.

Past commit or other branch: read code with `git show <head>:<path>` and search with `git grep <pattern> <head>`
(the code graph and the lint see the working tree only; lint another branch in a worktree, or list it as not run).
If a shell hook shortens `git diff` output, write the diff to a file and read that.

## 3. Size
`small` / `medium`: review alone, functional files one by one with their sibling open. `large` (> 1200 functional
lines, or functional changes in 3+ modules): launch subagents in one message, one per axis or per module, each with the
`--json` output, its section of `references/checklist.md` and `references/report.md`, returning confirmed findings
only; then merge, deduplicate and re-check every Bloquant yourself.

## 4. Judge
For each functional file, read the hunks with context (`git diff -U15 <base> <head> -- <file>`) and the sibling
listed. When no sibling is listed or it is not the right norm, search **methods of the same role**
(`git grep -n "delete\w*(" <head> -- '*Logic.java'`): the norm is how the team already solves the same problem.
Then walk `references/checklist.md`:
- **Correctness (CORR)**: does the changed code do what its name, its MR and its tests claim? Trace one realistic
  scenario through it (data that exists in production, an inverse relation, an empty list); check that a built query
  filters what it says. Found in the changed code: report it, first.
- **Maintainability (MAINT)**, **Homogeneity (HOMO)**, **Clarity (CLAR)**, **Test coverage (TEST)**,
  **Performance (PERF)**: the five axes asked for.

Judge changed lines; a legacy problem on untouched lines is one "Existant" line, unless the change makes it worse.
Before calling something duplicated or missing, search for it; before citing a sibling, open it.
Test evidence escalates only when needed (`references/coverage.md`): static map, then related test classes, then
JaCoCo coverage of the changed lines (`scripts/coverage_changed.py`).

## 5. Report
Format and wording: `references/report.md`. Verdict first; then Correction, Bloquant, Important, Suggestions,
Questions, Détails; the axis table; "Existant"; "Non vérifié". User's language. About 15 findings (Détails apart, at
most 3); never pad. On request, the GitLab format (one paste-ready comment per finding).

## 6. Fix (only when asked)
Java: `opensilex-java` skill, FIX mode (minimal diffs, lint, compile, targeted tests). Front: minimal edits following
the Vue guidelines. Re-run `review_scope.py` and the related tests; say what ran. MR text: `opensilex-mr` skill.

## Traps
- Without `--head`, the review includes uncommitted and untracked files: say which state you reviewed.
- Coverage covers only the tests that ran, on the checked-out sources (`coverage_changed.py` refuses another
  `--head`); delete stale `target/jacoco*.exec` first.
- No automated front tests exist: ask for a manual test plan instead of "not covered".
- `inrae-*` root directories may be nested repositories: skipped unless passed with `--paths`.
- `SCRIPT-NO-ENDPOINT`: endpoint detection failed for an API class; read it by hand.
- Never post on GitLab, push or commit as part of a review.

## Files
`scripts/review_scope.py` (CLI) with `scope_git.py` (snapshot, diff, change profile), `scope_code.py` (methods,
endpoints, nesting), `scope_leads.py` (lead detectors); `scripts/coverage_changed.py` (JaCoCo, changed lines);
`references/checklist.md` (ids, norms, severities), `references/coverage.md`, `references/report.md`.
