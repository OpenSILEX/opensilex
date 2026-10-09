#!/usr/bin/env python3
"""Headless SonarLint for the OpenSILEX Java code base.

  sonarlint.sh                      Java files changed against develop (+ uncommitted/untracked), changed lines only
  sonarlint.sh path/File.java dir/  explicit files/directories (every line unless --changed-lines)
  sonarlint.sh --fail-on MAJOR ...  exit 1 when an issue at/above the level exists (default: always exit 0)

Output: `relative/path.java:LINE:COL [SEVERITY] java:Sxxxx message` (severity BLOCKER > CRITICAL > MAJOR > MINOR > INFO),
then a summary. The engine is the official SonarLint backend driven over JSON-RPC (what the IDE plugins use), in
standalone mode: no SonarQube server, no network after the one-time setup, no telemetry.

Libraries (pinned in pom.xml) are looked up in this order: --lib-dir / $SONARLINT_HOME, the cache
~/.cache/opensilex-sonarlint, an installed SonarLint IDE plugin (IntelliJ) of the same version, and finally downloaded
from Maven Central (~72 MB, asks first; --yes to accept, --offline to forbid).
"""
import argparse
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPTS = os.path.dirname(HERE)
REPO = os.environ.get("OPENSILEX_LINT_REPO") \
    or subprocess.run(["git", "-C", HERE, "rev-parse", "--show-toplevel"], capture_output=True, text=True).stdout.strip() \
    or os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", ".."))
JDK_SH = os.path.join(SCRIPTS, "jdk.sh")
SEVERITIES = ["INFO", "MINOR", "MAJOR", "CRITICAL", "BLOCKER"]
LINE_RE = re.compile(r"^(\S+\.java):(\d+):(\d+) \[(\w+)\] (\S+) (.*)$")


def pom_property(name):
    text = open(os.path.join(HERE, "pom.xml"), encoding="utf-8").read()
    return re.search(r"<%s>([^<]+)</%s>" % (re.escape(name), re.escape(name)), text).group(1)


CORE_VERSION = pom_property("sonarlint.core.version")
JAVA_PLUGIN_VERSION = pom_property("sonar.java.plugin.version")
JAVA_SE_PLUGIN_VERSION = pom_property("sonar.java.se.plugin.version")
CACHE = os.path.join(os.environ.get("XDG_CACHE_HOME") or os.path.expanduser("~/.cache"), "opensilex-sonarlint", CORE_VERSION)


def log(msg):
    print("sonarlint: " + msg, file=sys.stderr)


def jdk(*args):
    r = subprocess.run(["bash", JDK_SH] + list(args), capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("sonarlint: %s" % (r.stderr.strip() or "jdk.sh failed: " + " ".join(args)))
    return r.stdout.strip()


# ------------------------------------------------------------------------------------------------ libraries
def layout_from_ide(root):
    """An installed SonarLint IDE plugin: sloop/lib (backend), lib (client), plugins (analyzers)."""
    sloop_lib = os.path.join(root, "sloop", "lib")
    backend = glob.glob(os.path.join(sloop_lib, "sonarlint-backend-cli-*.jar"))
    java_plugin = glob.glob(os.path.join(root, "plugins", "sonar-java-plugin-*.jar"))
    client = glob.glob(os.path.join(root, "lib", "sonarlint-rpc-java-client-*.jar"))
    if not (backend and java_plugin and client):
        return None
    version = re.search(r"sonarlint-backend-cli-(.+)\.jar$", backend[0]).group(1)
    utils = glob.glob(os.path.join(root, "lib", "sonarlint-java-client-utils-*.jar"))
    return {
        "sloop_dir": os.path.join(root, "sloop"),
        "plugins_dir": os.path.join(root, "plugins"),
        "classpath": [os.path.join(sloop_lib, "*")] + client + utils,
        "version": version,
        "origin": "IDE plugin " + root,
    }


def layout_from_cache():
    sloop_lib = os.path.join(CACHE, "sloop", "lib")
    if os.path.isfile(os.path.join(CACHE, ".complete")) and glob.glob(os.path.join(sloop_lib, "sonarlint-backend-cli-*.jar")):
        return {"sloop_dir": os.path.join(CACHE, "sloop"), "plugins_dir": os.path.join(CACHE, "plugins"),
                "classpath": [os.path.join(sloop_lib, "*")], "version": CORE_VERSION, "origin": "cache " + CACHE}
    return None


def ide_candidates():
    pats = [os.path.expanduser("~/.local/share/JetBrains/*/sonarlint-intellij"),
            os.path.expanduser("~/Library/Application Support/JetBrains/*/sonarlint-intellij"),
            os.path.expanduser("~/.config/JetBrains/*/sonarlint-intellij")]
    out = []
    for p in pats:
        out.extend(sorted(glob.glob(p), reverse=True))
    return out


def download(assume_yes):
    sys.stderr.write(
        "sonarlint: one-time setup. This will download from Maven Central (https://repo1.maven.org) into %s:\n"
        "  - sonarlint-backend-cli %s + its dependencies (about 50 MB, org.sonarsource.sonarlint.core)\n"
        "  - sonar-java-plugin %s (about 20 MB) and sonar-java-symbolic-execution-plugin %s (about 1 MB)\n"
        % (CACHE, CORE_VERSION, JAVA_PLUGIN_VERSION, JAVA_SE_PLUGIN_VERSION))
    if not assume_yes:
        if sys.stdin.isatty():
            if input("Proceed? [y/N] ").strip().lower() not in ("y", "yes"):
                sys.exit("sonarlint: aborted (use --lib-dir with an installed SonarLint IDE plugin, or --yes)")
        else:
            sys.exit("sonarlint: not downloading without confirmation: re-run with --yes (or set SONARLINT_ASSUME_YES=1)")
    os.makedirs(os.path.join(CACHE, "sloop", "lib"), exist_ok=True)
    os.makedirs(os.path.join(CACHE, "plugins"), exist_ok=True)
    mvn = ["bash", JDK_SH, "mvn", "-B", "-q"]
    steps = [
        mvn + ["-f", os.path.join(HERE, "pom.xml"), "dependency:copy-dependencies",
               "-DoutputDirectory=" + os.path.join(CACHE, "sloop", "lib"), "-DincludeScope=runtime"],
        mvn + ["-f", os.path.join(HERE, "pom.xml"), "dependency:copy",
               "-Dartifact=org.sonarsource.java:sonar-java-plugin:%s" % JAVA_PLUGIN_VERSION,
               "-DoutputDirectory=" + os.path.join(CACHE, "plugins")],
        mvn + ["-f", os.path.join(HERE, "pom.xml"), "dependency:copy",
               "-Dartifact=org.sonarsource.java:sonar-java-symbolic-execution-plugin:%s" % JAVA_SE_PLUGIN_VERSION,
               "-DoutputDirectory=" + os.path.join(CACHE, "plugins")],
    ]
    for cmd in steps:
        r = subprocess.run(cmd, capture_output=True, text=True)
        if r.returncode != 0:
            sys.exit("sonarlint: download failed:\n" + "\n".join((r.stdout + r.stderr).strip().splitlines()[-15:]))
    open(os.path.join(CACHE, ".complete"), "w").write(CORE_VERSION + "\n")
    layout = layout_from_cache()
    if not layout:
        sys.exit("sonarlint: download finished but the expected jars are missing in " + CACHE)
    return layout


def find_libs(a):
    explicit = a.lib_dir or os.environ.get("SONARLINT_HOME")
    if explicit:
        layout = layout_from_ide(explicit) or None
        if not layout:
            sys.exit("sonarlint: %s has no sloop/lib, lib and plugins directories of a SonarLint IDE plugin" % explicit)
        return layout
    layout = layout_from_cache()
    if layout:
        return layout
    for root in ide_candidates():
        layout = layout_from_ide(root)
        if layout and layout["version"] == CORE_VERSION:
            return layout
    if a.offline:
        sys.exit("sonarlint: libraries not installed and --offline given (run once without --offline, or pass --lib-dir)")
    return download(a.yes or os.environ.get("SONARLINT_ASSUME_YES") == "1")


# ------------------------------------------------------------------------------------------------ files and git
def git(*args):
    return subprocess.run(["git", "-C", REPO] + list(args), capture_output=True, text=True).stdout


def merge_base(base):
    for ref in ([base] if base else []) + ["origin/develop", "develop", "HEAD"]:
        r = subprocess.run(["git", "-C", REPO, "merge-base", "HEAD", ref], capture_output=True, text=True)
        if r.returncode == 0 and r.stdout.strip():
            return r.stdout.strip()
    return "HEAD"


def is_java_source(rel):
    return rel.endswith(".java") and ("/src/main/java/" in rel or "/src/test/java/" in rel) \
        and not any(s in "/" + rel for s in ("/target/", "/node_modules/", "/.kilo/", "/graft/", "/.claude/", "/front/"))


def changed_ranges(rel, mb):
    ranges = []
    for m in re.finditer(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", git("diff", "-U0", mb, "--", rel), re.M):
        start, count = int(m.group(1)), int(m.group(2)) if m.group(2) is not None else 1
        if count > 0:
            ranges.append((start, start + count - 1))
    return ranges


def select_files(a, mb):
    if a.paths:
        out = []
        for p in a.paths:
            ap = p if os.path.isabs(p) else os.path.join(os.getcwd(), p)
            if os.path.isdir(ap):
                for dp, dns, fns in os.walk(ap):
                    dns[:] = [d for d in dns if d not in ("target", "node_modules", ".git", ".kilo", "graft", "front")]
                    out += [os.path.relpath(os.path.join(dp, f), REPO) for f in fns]
            elif os.path.isfile(ap):
                out.append(os.path.relpath(ap, REPO))
            else:
                log("no such file or directory: " + p)
        files = sorted(set(f for f in out if is_java_source(f)))
        new = set()
    else:
        names = set(git("diff", "--name-only", "--diff-filter=ACMR", mb).splitlines())
        names |= set(git("ls-files", "--others", "--exclude-standard").splitlines())
        files = sorted(n for n in names if is_java_source(n) and os.path.isfile(os.path.join(REPO, n)))
        new = set(git("diff", "--name-only", "--diff-filter=A", mb).splitlines()) | set(git("ls-files", "--others", "--exclude-standard").splitlines())
    if a.no_tests:
        files = [f for f in files if "/src/test/java/" not in f]
    return files, new


# ------------------------------------------------------------------------------------------------ classpath
def module_classpath(module, java_home, offline_log):
    mod_dir = os.path.join(REPO, module)
    out_dir = os.path.join(mod_dir, "target", "sonarlint")
    cp_file = os.path.join(out_dir, "classpath.txt")
    poms = [os.path.join(mod_dir, "pom.xml"), os.path.join(REPO, "opensilex-parent", "pom.xml")]
    fresh = os.path.isfile(cp_file) and all(os.path.getmtime(cp_file) >= os.path.getmtime(p) for p in poms if os.path.isfile(p))
    if not fresh:
        os.makedirs(out_dir, exist_ok=True)
        cmd = ["bash", JDK_SH, "mvn", "-o", "-B", "-q", "-pl", module, "dependency:build-classpath",
               "-Dmdep.outputFile=" + cp_file, "-Dmdep.includeScope=test", "-DskipFrontBuild"]
        r = subprocess.run(cmd, cwd=REPO, capture_output=True, text=True)
        if r.returncode != 0 or not os.path.isfile(cp_file):
            offline_log("no Maven classpath for %s (offline resolution failed): type-based rules degrade\n    %s" % (
                module, "\n    ".join((r.stdout + r.stderr).strip().splitlines()[-3:])))
            return [], [], []
    libs = [p for p in open(cp_file, encoding="utf-8").read().strip().split(os.pathsep) if p and os.path.exists(p)]
    binaries = [d for d in (os.path.join(mod_dir, "target", "classes"),) if os.path.isdir(d)]
    test_binaries = [d for d in (os.path.join(mod_dir, "target", "test-classes"),) if os.path.isdir(d)]
    return libs, binaries, test_binaries


# ------------------------------------------------------------------------------------------------ main
def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paths", nargs="*")
    ap.add_argument("--base", help="base ref (default: merge-base with origin/develop or develop)")
    ap.add_argument("--changed-lines", "--new-code", action="store_true", dest="changed_lines",
                    help="report only added/modified lines (default when no path is given)")
    ap.add_argument("--all-lines", action="store_true", help="report every line (default when paths are given)")
    ap.add_argument("--format", choices=("text", "json"), default="text")
    ap.add_argument("--min-severity", choices=SEVERITIES, default="INFO")
    ap.add_argument("--fail-on", choices=SEVERITIES, help="exit 1 when an issue at/above this severity is reported")
    ap.add_argument("--rules-file", default=os.path.join(HERE, "sonarlint-rules.json"))
    ap.add_argument("--no-classpath", action="store_true", help="skip the Maven classpath (faster, less precise)")
    ap.add_argument("--lib-dir", help="installed SonarLint IDE plugin directory (contains sloop/, lib/, plugins/)")
    ap.add_argument("--offline", action="store_true", help="never download")
    ap.add_argument("--yes", action="store_true", help="accept the one-time download")
    ap.add_argument("--no-tests", action="store_true")
    ap.add_argument("--summary-only", action="store_true")
    ap.add_argument("--timeout", type=int, default=900,
                    help="seconds before the analysis is abandoned (default 900: a wide margin over the 32 s "
                         "measured for all of opensilex-core)")
    ap.add_argument("--verbose", action="store_true")
    a = ap.parse_args()

    mb = merge_base(a.base)
    files, new = select_files(a, mb)
    if not files:
        print("sonarlint: no Java file to analyse (nothing changed against %s)." % mb[:10])
        return 0
    use_ranges = (not a.paths and not a.all_lines) or a.changed_lines
    ranges = {f: (None if f in new else changed_ranges(f, mb)) for f in files} if use_ranges else {}

    libs = find_libs(a)
    if a.verbose:
        log("engine from " + libs["origin"] + " (version %s)" % libs["version"])
    if libs["version"] != CORE_VERSION:
        log("warning: tested with SonarLint core %s, found %s: the RPC API may differ" % (CORE_VERSION, libs["version"]))
    jdk_project = jdk("home")
    jdk_tools = jdk("home-min", "21")          # the SonarLint backend is compiled for Java 21
    release = jdk("level")

    by_module = {}
    for f in files:
        by_module.setdefault(f.split("/src/")[0], []).append(f)
    groups = []
    for module, mfiles in sorted(by_module.items()):
        group = {"name": module, "files": mfiles, "libraries": [], "binaries": [], "testLibraries": [], "testBinaries": []}
        if not a.no_classpath and os.path.isfile(os.path.join(REPO, module, "pom.xml")):
            cp, binaries, test_binaries = module_classpath(module, jdk_project, log)
            group.update(libraries=cp, binaries=binaries, testLibraries=cp, testBinaries=test_binaries)
        groups.append(group)

    work = tempfile.mkdtemp(prefix="sonarlint-")
    try:
        plan = {"baseDir": REPO, "release": release, "jre": jdk_tools, "jdkHome": jdk_project, "sloopDir": libs["sloop_dir"],
                "pluginsDir": libs["plugins_dir"], "workDir": os.path.join(work, "work"), "verbose": a.verbose,
                "rulesFile": a.rules_file if os.path.isfile(a.rules_file) else None, "timeoutSeconds": a.timeout, "groups": groups}
        plan_file = os.path.join(work, "plan.json")
        json.dump(plan, open(plan_file, "w"))
        cmd = [os.path.join(jdk_tools, "bin", "java"), "-cp", os.pathsep.join(libs["classpath"]),
               os.path.join(HERE, "SonarLintRunner.java"), plan_file]
        r = subprocess.run(cmd, cwd=REPO, capture_output=True, text=True, timeout=a.timeout + 120)
    finally:
        shutil.rmtree(work, ignore_errors=True)

    err_lines = [l for l in r.stderr.splitlines() if not re.search(r"warning: \[(removal|deprecation)\]|^\s+\^|^Note:|^\d+ warnings?$|^\s+(new |\.|List<)", l)]
    if a.verbose:
        print("\n".join(err_lines), file=sys.stderr)
    if r.returncode != 0:
        sys.exit("sonarlint: analysis failed (exit %d):\n%s" % (r.returncode, "\n".join(err_lines[-12:])))

    issues = []
    hidden = 0
    for line in r.stdout.splitlines():
        m = LINE_RE.match(line)
        if not m:
            continue
        path, ln, col, sev, rule, msg = m.group(1), int(m.group(2)), int(m.group(3)), m.group(4), m.group(5), m.group(6)
        if SEVERITIES.index(sev) < SEVERITIES.index(a.min_severity):
            continue
        rg = ranges.get(path, ())
        if use_ranges and rg is not None and not any(lo <= ln <= hi for lo, hi in rg):
            hidden += 1
            continue
        issues.append({"path": path, "line": ln, "col": col, "severity": sev, "rule": rule, "message": msg})

    if a.format == "json":
        print(json.dumps(issues, indent=1))
    else:
        if not a.summary_only:
            for i in issues:
                print("%s:%d:%d [%s] %s %s" % (i["path"], i["line"], i["col"], i["severity"], i["rule"], i["message"]))
        by_sev = Counter(i["severity"] for i in issues)
        by_rule = Counter(i["rule"] for i in issues)
        scope = "changed lines only, %d hidden" % hidden if use_ranges else "all lines"
        print("\nsonarlint: %d file(s), engine %s, %s -> %d issue(s): %s" % (
            len(files), libs["version"], scope, len(issues), ", ".join("%s=%d" % (s, by_sev[s]) for s in reversed(SEVERITIES) if by_sev[s]) or "clean"))
        if by_rule and (a.summary_only or len(issues) > 20):
            print("sonarlint: top rules: " + ", ".join("%s=%d" % kv for kv in by_rule.most_common(12)))
    if a.fail_on and any(SEVERITIES.index(i["severity"]) >= SEVERITIES.index(a.fail_on) for i in issues):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
