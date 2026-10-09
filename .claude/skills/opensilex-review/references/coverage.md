# Test coverage: static map, targeted run, measured coverage

Three levels, cheapest first. Stop at the level that answers the question; say which level the report is based on.

## Level 1: static map (always)
`review_scope.py` lists, for the changed code:
- changed test files and how many `@Test` / `@Ignore` were added or removed;
- each endpoint with functional changes and the tests that call it through `XxxAPI.class.getMethod("method", ...)`
  (the way every API test builds its calls); endpoints that only gained an argument are not listed;
- each public method that is new, changed (>= 3 lines) or calls a changed private method, and the tests that call it
  directly or through an endpoint of the same name;
- each functional class and the tests that use it (same package, import or qualified name: a test of a class with the
  same simple name in another module does not count).

Limits: a test that mentions a class may not exercise the changed lines; an endpoint reached through a helper or
another service is not seen. Treat "NOT called by any test" as strong evidence and "referenced by X" as weak evidence:
open X and check that a test exercises the changed behaviour, failure paths included (TEST-2).

## Level 2: run the related tests (when the user asks whether tests pass, or a finding depends on it)
Integration tests start an embedded RDF4J and an embedded MongoDB (no Docker). Run classes, not modules:
```bash
J=.claude/skills/opensilex-java/scripts/jdk.sh
$J mvn -o -pl opensilex-core test -Dtest='SiteAPITest,SiteDAOTest' -Dsurefire.failIfNoSpecifiedTests=false -DskipFrontBuild
```
- After a change in `opensilex-sparql`, `-main` or `-security`, install them first (`-am`, or
  `$J mvn -o -pl opensilex-sparql install -DskipTests -DskipFrontBuild`), otherwise the run uses stale jars.
- A colleague's branch: run it in a worktree (`git worktree add ../review-<name> origin/<branch>`), never by switching
  the user's checkout.
- Report: classes run, `Tests run / Failures / Errors / Skipped`, duration; quote the first assertion error.

## Level 3: measured coverage of the changed lines (on request, or when a business branch is in doubt)
The `with-test-report` profile wires JaCoCo into surefire (it also runs Checkstyle, SpotBugs and PMD: skip them).
Measured on this repository: one API test class plus the report in about 30 s.
```bash
rm -f opensilex-core/target/jacoco*.exec        # stale .exec files of earlier runs are merged otherwise
.claude/skills/opensilex-java/scripts/jdk.sh mvn -o -pl opensilex-core -Pwith-test-report verify \
    -Dtest='SiteAPITest' -Dsurefire.failIfNoSpecifiedTests=false -DskipFrontBuild \
    -Dcheckstyle.skip -Dspotbugs.skip -Dpmd.skip -Dcpd.skip
python3 .claude/skills/opensilex-review/scripts/coverage_changed.py            # same base as review_scope.py
```
- The report lands in `site/<module>/jacoco/jacoco.xml` (`site/` is build output). `coverage_changed.py` reads every
  `site/**/jacoco.xml`, keeps the executable changed lines and prints covered / uncovered line ranges per file and a
  total.
- It refuses a `--head` that is not the checked-out commit: JaCoCo line numbers describe the sources that were run.
- Coverage only reflects the tests of that run. A 0 % file whose tests were not run means "not run", not "not tested":
  pick the test classes from the Level 1 map, and run every module the change spans (`-pl a,b`).
- Report the uncovered **changed branches that carry behaviour** (a condition, an error path), not a percentage
  target: the project has no coverage threshold.

## Front
No automated front tests exist (`opensilex-front/front/package.json` has no Jest, Vitest or Playwright). Coverage of a
front change is a manual test plan: ask for it (or write it) as concrete steps in the MR, with the data needed. A
`data-testid` on new interactive elements prepares the planned Playwright tests (dev workflow document).
