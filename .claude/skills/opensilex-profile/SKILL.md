---
name: opensilex-profile
description: Measure the memory and CPU footprint of a running OpenSILEX server (JVM plus its RDF4J and MongoDB containers) while a scenario runs, phase by phase, and compare runs before and after a fix - JFR allocations and CPU by REST endpoint and OpenSILEX method, RSS, heap, metaspace, generated classes, GC, container growth. Use when the user asks to profile the server, measure memory, find what allocates or leaks, explain a slow or memory-hungry operation (import, generation, deletion), or compare before/after an optimisation; also in French "profilage", "profile le serveur", "empreinte mémoire", "fuite mémoire", "mesure la mémoire", "qu'est-ce qui consomme", "compare avant/après".
---

# OpenSILEX profiling

Records what a running server does during a scenario the user drives (data generator, import, UI actions), then
says where memory and CPU go. Paths are relative to `.claude/skills/opensilex-profile/`.

## Run
```bash
P=.claude/skills/opensilex-profile/scripts
$P/profile.sh find                       # server JVM on port 8666: main class, start time, checkout
$P/profile.sh start [PID] generation     # JFR + GC log + samplers -> ~/memprofile/<date>_<time>_generation/
$P/profile.sh mark suppression           # between phases: each phase gets its own JFR file
$P/profile.sh status
$P/profile.sh stop [--histogram] [--pmap]  # --histogram forces a FULL GC: only at the very end
python3 $P/analyze.py <run>              # figures per phase + JFR top lists
python3 $P/analyze.py <after> --compare <before>
```
Start, then tell the user to run the scenario and to say when each phase is over; `mark` / `stop` on their word.
Settings: `OPENSILEX_PROFILE_DIR` (runs, default `~/memprofile`), `OPENSILEX_PORT` (8666),
`OPENSILEX_CONTAINER_PORTS` ("8667 8668"), `OPENSILEX_SAMPLE_SECONDS` (2).

## Before measuring
- **Is it the code you think?** `find` prints the checkout of the server's working directory; check the branch and
  that the changed classes were rebuilt and are loaded (a class not loaded yet will load the new version).
- **Dev launcher** (`org.opensilex.dev.StartServer...`): `DevModule` forces `DEBUG=true`, so logging costs are in
  every figure (HttpClient request logs, SPARQL dumps). Compare only runs started the same way.
- **Comparable runs**: same scenario, same data volume (a quadratic cost disappears on small volumes), one change
  between the two runs, commit noted. A server restart mid-run invalidates the run (JFR is still saved on exit).

## Reading the results
- **Allocation by REST endpoint, then by first `org.opensilex` frame**: the method to read first. Allocation is a
  JFR estimate; compare shares and orders of magnitude, not exact bytes.
- **Classes loaded / unloaded** rising during a phase: classes generated at runtime (proxies, ByteBuddy); metaspace
  follows. Tens of thousands per phase is a bug, not a warm-up.
- **RSS far above reserved heap + metaspace**: native memory (threads, malloc arenas, direct buffers): `--pmap`.
- **Containers**: read the gain during the run, never the absolute size (it grows with the stored data).
- **Live objects** (`histogram_after_full_gc.txt`): what stays after a full GC, i.e. leaks and caches.
Report with numbers per phase, the top allocators with their share, the method to read, and what was not measured.

## Files of a run
`run.txt` (pid, JDK, main class, checkout, times), `sample.csv` (every 2 s: RSS, threads, CPU, heap, metaspace, GC,
classes), `containers.csv`, `phases.csv`, `run.jfr`, `jfr/NN_<phase>.jfr`, `gc.log`, optional `pmap.txt` and
`histogram_after_full_gc.txt`. Older runs in `~/memprofile` (before this skill) are read as a single phase.

## Rules
- Nothing goes to the repository: runs live outside it, nothing to commit.
- The JVM command line is never printed or stored (it may carry passwords); only the main class is.
- `jcmd` must attach as the server's user; JDK tools come from the server's own JDK, else `JAVA_HOME`.
