# Report: severity, wording, formats

The report is read by a developer who has to decide what to change before merging. Every line must help that
decision. Write it in the user's language (French by default in this team), code identifiers unchanged.

## Severity

| Label | Meaning | Typical cases |
|---|---|---|
| **Bloquant** | must change before merge | data loss or wrong behaviour seen in passing, N+1 on a list/search/import/export path, new endpoint with no test, hand-edited generated file, broken front caller, test class that never runs |
| **Important** | should change in this MR; merging as is creates debt or spreads a bad pattern | duplication, logic in the wrong layer, missing failure-path tests, missing translation, `any` or raw `$emit` in a new component, reformatting mixed with the change |
| **Suggestion** | worth doing, the author decides | clearer name inside a method, extraction, cache, lazy loading |
| **Question** | the reviewer cannot judge without the author | intent of a behaviour change, volume tests, why a pattern differs from its siblings |
| **Détail** | cosmetic; at most three, outside the ~15 limit, the rest is "voir lint" | `console.log`, lint warnings |

Raise or lower a default severity from `checklist.md` according to the actual impact on this code path, and say why
when you do.

## Rules
- **Evidence first**: every finding cites `path:line`, quotes the code (one line, shortened) or names what is missing,
  and names the norm: the sibling file that does it right, the guideline, the team document.
- **Confirmed only**: leads from `review_scope.py` are hypotheses. Report one only after reading the code around it.
  Drop false positives silently.
- **Concrete fix**: what to write instead, with the helper or sibling to imitate. For a design choice, give the
  recommended option and at most one alternative.
- **Group repetitions**: "même problème à 6 endroits : A.vue:12, A.vue:40, B.vue:8, ..." is one finding.
- **Bounded**: about 15 findings at most, most severe first. Beyond that, keep the most severe and say how many
  lower-severity items were left out and where they cluster.
- **No padding**: a small clean change can get "rien de bloquant" and two suggestions. Never invent findings to fill
  an axis; write "rien à signaler" for that axis.
- **Fair to legacy**: problems on untouched lines go to a single "Existant" line, never to the findings, unless the
  change makes them worse.
- **Correctness first**: a problem where the changed code does not do what it claims (CORR in `checklist.md`) goes
  in its own "Correction" section, before Bloquant, with an id `CORR-n` and the scenario that shows it. Say how
  you established it (query built, scenario traced, test run) or that it is deduced from reading.
- **Say what was not checked**: tests not run, lint not run, files skipped, front not executed, volumes not tried.

## Default format (chat)

Example on the fictitious `Widget` concept: in a real report, cite siblings you have opened.

```markdown
## Revue : <branch or range> (<N> fichiers, +A -D)

**Verdict** : <Prêt à merger | Prêt après corrections | À retravailler> - <one sentence: the main reason>

### Correction
0. **[CORR-1] <ce qui ne marche pas>** - `path:line`
   <scénario qui le montre, comment c'est établi>
   -> <correctif>

### Bloquant
1. **[PERF-1] N+1 sur la recherche des widgets** - `opensilex-core/.../WidgetLogic.java:84`
   `widgetDAO.get(uri)` est appelé pour chaque URI de la page (jusqu'à `page_size` requêtes SPARQL).
   -> Charger la page en une requête avec `sparql.getListByURIs(WidgetModel.class, uris, lang)`, comme le fait déjà `<voisin réel>`.

### Important
2. **[TEST-1] `getWidgetsByURIs` n'est appelé par aucun test** - `WidgetAPI.java:120`
   -> Ajouter à `WidgetAPITest` un cas nominal et un cas URI inconnue (404), sur le modèle de `<XxxAPITest voisin>`.
3. **[HOMO-5] Traduction fr manquante** - `WidgetForm.vue`, clé `WidgetForm.name-help`
   -> Ajouter la clé dans le bloc `fr:` du `<i18n>`.

### Suggestions
4. **[CLAR-1]** `process()` -> `computeDefaultPeriod()` (`WidgetLogic.java:140`) : le nom dit ce que la méthode calcule.

### Questions
5. **[PERF-8]** La recherche a-t-elle été essayée sur la base de référence ?

### Détails
6. `WidgetForm.vue:88` : `console.log` oublié.

### Par axe
| Axe | Constat |
|---|---|
| Correction (hors axes) | rien de trouvé dans le code modifié |
| Maintenabilité | 1 important (duplication de `toModel`) |
| Homogénéité | conforme aux voisins (`<XxxAPI>`) ; 1 traduction manquante |
| Clarté | rien à signaler |
| Tests | 1 endpoint non testé ; chemins d'erreur 403/409 absents |
| Performance | 1 N+1 bloquant |

**Existant (non demandé dans cette MR)** : `WidgetDAO.search` charge toutes les URIs avant de paginer.
**Non vérifié** : tests non lancés, front non exécuté, SonarLint non installé.
```
Keep the "Par axe" table: the user asked for these five axes; it shows each one was examined.

## GitLab format (on request: "format GitLab", "commentaires de MR")
One block per finding, ready to paste as a thread on the line, with Conventional Comments labels in French:
```markdown
`opensilex-core/.../WidgetLogic.java:84`

**problème (bloquant)** : `widgetDAO.get(uri)` dans la boucle fait une requête SPARQL par élément de la page.

Proposition : charger la page en une fois avec `sparql.getListByURIs(WidgetModel.class, uris, lang)`
(voir `<voisin réel>`).
```
Labels: `problème (bloquant)`, `problème`, `suggestion`, `question`, `détail`, `bravo` (only for something specific
the team should reuse).

## Saving the report
Only when asked: write it to `$(git rev-parse --git-path review-drafts)/<branch-slug>.md` (inside `.git/`, never
committed) and give the path. Posting comments on GitLab is an outward action: never without an explicit request
and confirmation.
