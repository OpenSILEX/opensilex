# SonarLint, headless

`sonarlint.sh` lints Java files with the **same engine and analyzers as the SonarLint ("SonarQube for IDE") plugins**,
from the command line. SonarSource ships no standalone SonarLint CLI; this tool drives the official SonarLint backend
over JSON-RPC exactly like the IDE plugins do (`SonarLintRunner.java`, a single-file Java program), in standalone mode:
no SonarQube server, no network after the one-time setup, no telemetry (no backend capability is declared), and the
backend's work files live in a temporary directory that is deleted at the end.

```bash
S=.claude/skills/opensilex-java/scripts/sonarlint/sonarlint.sh
$S                                  # Java files changed against develop (+ uncommitted/untracked), CHANGED LINES ONLY
$S path/To/File.java dir/           # explicit files / directories, every line
$S --changed-lines path/File.java   # explicit files, but only what the diff touched
$S --fail-on MAJOR                  # exit 1 when an issue at/above MAJOR is reported (default: exit 0)
$S --summary-only opensilex-core/src/main/java      # counts by severity and top rules
$S --format json | --min-severity MAJOR | --no-tests | --no-classpath | --verbose
```
Output: `relative/path.java:LINE:COL [SEVERITY] java:Sxxxx message` (`BLOCKER > CRITICAL > MAJOR > MINOR > INFO`), then a
summary. Also available through `python3 scripts/lint.py --sonar` (merged with the conventions linter and Checkstyle).

Measured here (SonarLint core 11.6, SonarJava 8.34): 3 files in 5 s, the `core/project` package (7 files) in 8 s with the
Maven classpath, all of `opensilex-core/src/main/java` (419 files) in 32 s: 1,604 issues before the rule overrides.
Triage them on changed lines only; never mass-fix legacy findings (CONTRIBUTING.md).

## What it needs
| Need | How it is found |
|---|---|
| **JDK 21+ to run the engine** (the SonarLint backend is compiled for Java 21) | `scripts/jdk.sh home-min 21` (SDKMAN, `/usr/lib/jvm`, macOS JVMs, `$OPENSILEX_TOOLS_JAVA_HOME`) |
| **JDK 17 = the project release** to analyse against (`sonar.java.source=17`, `sonar.java.jdkHome`) | `scripts/jdk.sh home` (release read from `opensilex-parent/pom.xml`) |
| The SonarLint backend jars + the Java analyzers | looked up in this order: `--lib-dir` / `$SONARLINT_HOME` (an installed SonarLint IDE plugin directory: `sloop/`, `lib/`, `plugins/`), the cache `~/.cache/opensilex-sonarlint/<version>`, an installed **IntelliJ SonarLint plugin of the same version** (auto-detected under `~/.local/share/JetBrains/*/sonarlint-intellij`), and finally a **one-time download from Maven Central** |
| Python 3.8+, Maven (offline, for classpaths) | already required by the project tooling |

The one-time download (about 72 MB: `sonarlint-backend-cli` + dependencies about 50 MB, `sonar-java-plugin` about 20 MB,
`sonar-java-symbolic-execution-plugin` about 1 MB, all from https://repo1.maven.org) **asks first**; pass `--yes` (or
`SONARLINT_ASSUME_YES=1`) to accept, `--offline` to forbid it. It runs `mvn -f scripts/sonarlint/pom.xml
dependency:copy-dependencies` and `dependency:copy`, which verify Maven Central's checksums, into the cache.

> **Verification status.** The runner, the wrapper, the changed-lines filter, the rule overrides and the classpath
> handling were tested here against the jars of an installed IntelliJ SonarLint plugin (core 11.6.0.85952, same
> versions as `pom.xml`). The Maven Central download path (`pom.xml` + `dependency:copy*`) was **not executed** here
> (no download was authorised): the five pinned coordinates were confirmed to exist with HEAD requests, but the flat
> copy of the transitive dependencies has not been validated. If it fails, install the SonarLint plugin in IntelliJ
> (same version) or pass `--lib-dir`.

## How classpath resolution works
Type-aware rules need the compile classpath. Per module (first path segment, e.g. `opensilex-core`) the wrapper runs
`jdk.sh mvn -o dependency:build-classpath -Dmdep.includeScope=test` and caches the result in
`<module>/target/sonarlint/classpath.txt` (ignored by git; refreshed when a `pom.xml` is newer), adds
`target/classes` and `target/test-classes` when they exist, and hands them to the analyzer as `sonar.java.libraries`,
`sonar.java.binaries`, `sonar.java.test.libraries`, `sonar.java.test.binaries`. When Maven cannot resolve offline the
tool warns and continues without classpath (fewer findings: on `core/project`, 20 instead of 34, the missing ones being
unused imports, static-access and injection rules). `--no-classpath` skips this step on purpose.

## Rules: `sonarlint-rules.json`
Applied on top of the default "Sonar way" Java profile through the backend's rule-configuration API.
```json
{ "disabled": ["java:S6813"], "enabled": ["java:S1234"], "parameters": { "java:S107": { "max": "10" } } }
```
Switched off by default because they contradict an idiom the team uses on purpose: `java:S6813` (field injection:
HK2 `@Inject private SPARQLService sparql;`), `java:S3252` (static access through the subclass:
`ProjectModel.NAME_FIELD`), and the INFO reminders `java:S1133` (deprecated code) and `java:S1135` (TODO). Everything
else stays on. Noisy-but-legitimate rules on legacy code (`S112`, `S1192`, `S3776`, `S125`, `S120`...) are handled by the
changed-lines filter, not by disabling them.

## Same rules as the INRAE SonarQube (connected mode, IDE side)
The CI runs `sonar-maven-plugin` against a private INRAE SonarQube whose URL, project key and token are secret CI
variables (`SONARQUBE_URL`, `SONARQUBE_OPENSILEX_PROJECT_KEY`, `SONARQUBE_TOKEN`). This headless tool cannot read them
and uses the default profile plus the overrides above. To lint with the **server's quality profile**, bind the IDE
(a token only the developer can create):
- IntelliJ: Settings > Tools > SonarQube for IDE > add a SonarQube Server connection (URL + personal token), then
  "Bind project to SonarQube" with the OpenSILEX project key.
- VS Code (`SonarSource.sonarlint-vscode`): in the user settings add the connection under
  `sonarlint.connectedMode.connections.sonarqube` and bind the workspace with `sonarlint.connectedMode.project`.
Do not commit URLs or tokens; `.vscode/` and `.idea/` are git-ignored in this repository (`.*/`).

## Bumping the engine
Edit the three properties of `pom.xml` (`sonarlint.core.version`, `sonar.java.plugin.version`,
`sonar.java.se.plugin.version`), delete `~/.cache/opensilex-sonarlint/<old version>`, run the tool on a few files. The
JSON-RPC API of sonarlint-core changes between versions: `SonarLintRunner.java` uses `SloopLauncher`,
`InitializeParams` (20-argument constructor), `ConfigurationScopeDto`, `ClientFileDto`, `DidUpdateFileSystemParams`,
`AnalyzeFilesAndTrackParams`, `AnalyzeFilesResponse.getRawIssues()` and a dynamic proxy of
`SonarLintRpcClientDelegate`; a compile error in the runner names the signature that moved (inspect with
`javap -cp "<lib>/*" org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.InitializeParams`).

## Troubleshooting
| Symptom | Cause / fix |
|---|---|
| `UnsupportedClassVersionError ... class file version 65` | the engine runs on a JDK older than 21: install JDK 21+ or set `OPENSILEX_TOOLS_JAVA_HOME` |
| `jdk.sh: no JDK 17 found` | install a JDK 17 (SDKMAN `sdk install java 17.0.14-tem`) or set `OPENSILEX_JAVA_HOME` |
| `not downloading without confirmation` | non-interactive shell: re-run with `--yes`, or use `--lib-dir` |
| `TimeoutException` / hang | raise `--timeout`; run with `--verbose` to read the backend log |
| `No file to analyze` in the backend log | files must be declared "user defined" (the runner does it); a path outside `src/main/java`/`src/test/java` is skipped |
| few findings, `no Maven classpath` warning | offline resolution failed: run `scripts/jdk.sh mvn -o -pl <module> dependency:build-classpath` to see why |
| a stack trace is needed | `SONARLINT_RUNNER_DEBUG=1` |

Limits: Java only (JS/TS/XML analyzers are not loaded), no quick fixes, no secrets/hotspot analysis, Linux/macOS only.
