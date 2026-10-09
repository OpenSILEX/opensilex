# Lint: conventions, Checkstyle, SonarLint

## Contents
1. Tools: what each one brings and costs
2. Rule catalogue of the `conventions` tool
3. SonarLint: headless runner, setup, triage of this code base
4. Build sanity next to lint

One entry point, `scripts/lint.py`, runs three tools and merges their output into
`path:line:col: severity: [tool:RULE] message` lines (the format IDEs and Claude read).

```bash
LINT=.claude/skills/opensilex-java/scripts/lint.py
python3 $LINT                              # Java files changed vs develop (+ uncommitted/untracked), changed lines only
python3 $LINT path/File.java dir/          # explicit files/directories, every line
python3 $LINT --changed-lines path/File.java   # explicit files, but only what the diff touched
python3 $LINT --tools conventions          # fast, offline, no Maven (about 3 s for the whole code base)
python3 $LINT --sonar                      # + SonarLint (needs the runner, see below)
python3 $LINT --fix                        # safe mechanical fixes on the reported lines only
python3 $LINT --format json --fail-on warning
```
Exit status 1 when an `error` is reported (`--fail-on warning|never`). Scope rule: with no path the filter is
"changed lines only" so that legacy noise never drowns the review (CONTRIBUTING.md: no drive-by reformatting). New
files count as fully changed.

## 1. Tools
| Tool | What it brings | Cost |
|---|---|---|
| `conventions` (built in) | OpenSILEX rules no generic linter knows: javax vs jakarta, `dal`->`api` imports, test class names surefire would skip, SPARQL model accessors/final/no-arg, endpoint annotations, query-param case, Java release vs features, header on new files, hygiene | pure Python, about 3 s for 1,060 files |
| `checkstyle` | The project's own Checkstyle config (`opensilex-parent/pom.xml`, Sun-style subset, severity warning), run through Maven offline under JDK 17, noise filtered | 3-6 s per module |
| `sonar` | SonarLint rules (Java analyzer) | see section 3 |

Checkstyle noise is dropped on purpose: `JavadocVariable`, `MissingJavadocMethod`, `JavadocMethod/Type/Style`,
`MagicNumber`, `TodoComment`, `FinalClass`, `HideUtilityClassConstructor`, `MethodLength`, `ParameterNumber`: configured
but violated everywhere, the team does not follow them. Kept: imports, whitespace, braces, naming, tabs, trailing
spaces, final newline, line length, `EqualsHashCode`, `MissingSwitchDefault`, `InnerAssignment` and friends. Rules
reported by both tools are shown once.

## 2. Rule catalogue of the `conventions` tool
| Rule | Sev | Meaning / fix |
|---|---|---|
| `FMT-TAB` | error | tab indentation (Checkstyle `FileTabCharacter`): 4 spaces. `--fix` |
| `FMT-TRAILING-WS` | warn | trailing blanks. `--fix` |
| `FMT-EOF-NEWLINE` | warn | file must end with a newline. `--fix` |
| `FMT-KEYWORD-SPACE` | warn | `if(`, `for(`, `while(`, `switch(`, `catch(`, `synchronized(`: add a space. `--fix` |
| `FMT-BRACE-OWN-LINE` | warn | K&R braces (`{` ends the previous line) |
| `FMT-LINE-LENGTH` | warn >150 / info >120 | wrap; URLs and imports exempt |
| `IMP-STAR` | warn | explicit imports (Checkstyle `AvoidStarImport`, Sonar S2208) |
| `IMP-UNUSED` | warn | remove (heuristic: whole-word search, Javadoc references count as use) |
| `IMP-JAKARTA` | error | use `javax.ws.rs/validation/inject`; only `jakarta.json` exists in the project |
| `IMP-ORDER` | info | new files: others, `javax`, `java`, static (IntelliJ layout) |
| `HDR-MISSING` | warn | new file without the license header: `scaffold.py` generates it |
| `NAME-PACKAGE` | warn | new package must be lowercase |
| `NAME-LAYER` | error/warn | `*DTO`/`*API` in `dal`; `*DAO`/`*Model` in `api` |
| `NAME-TEST-SUFFIX` | error | class with `@Test` not named `Test*`/`*Test`/`*Tests`/`*TestCase`: never executed |
| `NAME-CONSTANT` | warn | `static final` String/primitive/URI/Pattern must be `UPPER_SNAKE_CASE` |
| `NAME-LOGGER` | info | static logger is called `LOGGER` |
| `ARCH-DAL-API` | error | `dal` imports an `api` class (11 historical violations) |
| `LOG-SYSOUT` / `LOG-STACKTRACE` / `LOG-CONCAT` | warn/warn/info | use the SLF4J `LOGGER` and `{}` placeholders |
| `EXC-EMPTY-CATCH` | warn (info if commented) | log, rethrow, or comment why ignoring is safe |
| `API-PROTECTED` | warn | endpoint without `@ApiProtected` (public) |
| `API-OPERATION` / `API-RESPONSES` | warn/info | Swagger documentation missing |
| `API-CREDENTIAL` | warn | PUT/DELETE without `@ApiCredential` |
| `API-GET-BY-URIS` | warn | use `POST by_uris` |
| `API-QUERYPARAM-CASE` | warn | snake_case query params (BrAPI/FAIDARE exempt) |
| `MODEL-FINAL` / `MODEL-NOARG` | error | SPARQL model must be a non-final class with a public no-arg constructor |
| `MODEL-ACCESSORS` | warn | mapped field without getter/setter in the class or a superclass |
| `MODEL-FIELD-CONSTANT` | info | no `XXX_FIELD` constant for a mapped field |
| `JAVA-RELEASE` | error | feature newer than `java.compiler.version` (table in the script, driven by the pom) |
| `JAVA-LEGACY-DATE` / `JAVA-LEGACY-COLLECTION` | info | `java.time`, `ArrayList`/`StringBuilder` |
| `JAVA-OPTIONAL-MISUSE` | warn | `Optional` only as a return type |
| `JAVA-FINAL-PARAM` / `JAVA-SUPPRESS-ALL` | info/warn | legacy `final` parameter; never `@SuppressWarnings("all")` |
| `TEST-JUNIT5` / `TEST-SLEEP` / `TEST-IGNORE` / `TEST-SYSOUT` | error/warn/info/info | JUnit 4 only; no sleeping; give a reason; assert instead of printing |

Heuristics, not a parser: read the line before acting, and tell the user when you judge a finding a false positive.
`OPENSILEX_LINT_REPO=/path` points the tool at another checkout (used for testing it on a scratch repository).

## 3. SonarLint (headless, same engine as the IDE plugins)
SonarSource ships no standalone SonarLint ("SonarQube for IDE") CLI, so `scripts/sonarlint/` drives the official
SonarLint backend (core 11.6, SonarJava 8.34) over JSON-RPC like the IDE plugins do: standalone mode, no server, no
network after setup, no telemetry. Setup, versions, troubleshooting: `scripts/sonarlint/README.md`.
```bash
S=.claude/skills/opensilex-java/scripts/sonarlint/sonarlint.sh
$S                                # changed files, changed lines only (the default, like lint.py)
$S path/File.java                 # explicit files, every line
$S --summary-only opensilex-core/src/main/java      # counts by severity and top rules
python3 .claude/skills/opensilex-java/scripts/lint.py --sonar          # merged with conventions + Checkstyle
```
- Needs **JDK 21+ for the engine** (`jdk.sh home-min 21`) and analyses against the project JDK 17. Libraries come from
  `~/.cache/opensilex-sonarlint`, from an installed IntelliJ SonarLint plugin of the same version (auto-detected), or
  from a one-time ~72 MB download from Maven Central that **asks first** (`--yes`). The Maven Central path was not
  executed when the tool was written; the IDE-plugin path was.
- Module classpaths come from `jdk.sh mvn -o dependency:build-classpath` (cached in `<module>/target/sonarlint`):
  type-aware rules (unused imports, static access...) need them; `--no-classpath` is faster and finds about 40 % fewer issues.
- Rule overrides live in `scripts/sonarlint/sonarlint-rules.json`: `S6813` (field injection), `S3252` (static access
  through `XxxModel.NAME_FIELD`), `S1133`/`S1135` (reminders) are off because they contradict team idioms.
- Speed: 5 s for a few files, 32 s for all of `opensilex-core` (419 files, 1,604 issues before the filter).
- In `lint.py` the severities map BLOCKER/CRITICAL -> error, MAJOR -> warning, MINOR/INFO -> info.

Triage on this code base (counts over `opensilex-core`):
- **Real defects, fix when the line is touched**: `S2201` ignored return value, `S4973` `==` on strings/boxed types,
  `S2387` field hides an inherited field (BLOCKER, 18), `S2095` unclosed resources, `S1854`/`S1481` dead assignments,
  `S1128` unused imports (92), `S4507` debug feature left on, `S6204` modifying a `Stream.toList()` result.
- **Team conventions to align with**: `S6355` + `S1123` (a `@Deprecated` needs `since`/`forRemoval` and a Javadoc
  `@deprecated` naming the replacement), `S1161` missing `@Override`, `S1124` modifier order.
- **Modernisation hints, not defects**: `S6204` `collect(Collectors.toList())` -> `toList()` (126 hits; mind mutability,
  see `java17-practices.md`), `S8688` `.now()` without a zone.
- **Frequent and mostly accepted in legacy code**: `S112` generic exceptions (97), `S3776` cognitive complexity (57),
  `S120` camelCase packages (52), `S1192` duplicated literals (44), `S125` commented-out code (38), `S107` long
  parameter lists (28): report them on changed lines only, never mass-fix.

To lint with the **INRAE SonarQube quality profile** (the CI runs `sonar-maven-plugin` against it; URL, project key and
token are secret CI variables) bind the IDE in connected mode (`scripts/sonarlint/README.md`); this headless tool uses the
default profile plus the overrides above.

## 4. Build sanity next to lint
Compile and tests are part of "verified": `jdk.sh mvn -o -q -pl <module> compile -DskipFrontBuild`, then the targeted
test class. `-DskipFrontBuild` skips the Vue/yarn build (almost always wanted for backend work).
