#!/usr/bin/env python3
"""Find the JDK that matches the Java release of the project and run Maven with it (Linux, macOS, Windows).

  jdk.py level          print the project's Java release (java.compiler.version, e.g. 17)
  jdk.py home           print a JAVA_HOME whose major version == release (exit 1 when none)
  jdk.py home-min N     print the lowest installed JAVA_HOME with major >= N (for tools needing a newer runtime)
  jdk.py env [SHELL]    print the commands that set JAVA_HOME and PATH for SHELL = posix | powershell | cmd
                        (default: posix, powershell on Windows). Use: eval "$(python3 jdk.py env)",
                        or python jdk.py env | Invoke-Expression
  jdk.py check          human-readable diagnostic (default java, selected JDK, Maven); the default command
  jdk.py mvn [args...]  run Maven under the selected JDK

Why: the `java` found first on PATH is often older than the project release (a JDK 11 cannot compile
`release 17`), and Maven silently follows it. Never trust the shell default: go through this script.

Override the search with OPENSILEX_JAVA_HOME=/path/to/jdk. The release is read from opensilex-parent/pom.xml
(first <java.compiler.version>; the opt-in `for-java-11` profile that overrides it further down the file is
ignored on purpose).
"""
import glob
import os
import re
import shlex
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
WINDOWS = os.name == "nt"
EXE = ".exe" if WINDOWS else ""


class JdkError(Exception):
    pass


def _toplevel(path):
    """Git top level of `path` when it is an OpenSILEX checkout, else empty."""
    r = subprocess.run(["git", "-C", path, "rev-parse", "--show-toplevel"], capture_output=True, text=True)
    top = r.stdout.strip() if r.returncode == 0 else ""
    return os.path.normpath(top) if top and os.path.isfile(os.path.join(top, "opensilex-parent", "pom.xml")) else ""


# the OpenSILEX checkout we are run from, else the one holding this skill
REPO = _toplevel(os.getcwd()) or _toplevel(HERE) or os.path.abspath(os.path.join(HERE, "..", "..", "..", ".."))
POM = os.path.join(REPO, "opensilex-parent", "pom.xml")


def level():
    try:
        text = open(POM, encoding="utf-8").read()
    except OSError:
        return ""
    m = re.search(r"<java\.compiler\.version>(\d+)</java\.compiler\.version>", text)
    return m.group(1) if m else ""


def major_of(home):
    """Major version of the JDK installed in `home` (None when `home` is not a JDK)."""
    try:
        text = open(os.path.join(home, "release"), encoding="utf-8", errors="replace").read()
    except OSError:
        return None
    m = re.search(r'^JAVA_VERSION="([^"]+)"', text, re.M)
    if not m:
        return None
    parts = m.group(1).split(".")
    major = parts[1] if parts[0] == "1" and len(parts) > 1 else parts[0]
    digits = re.match(r"\d+", major)
    return int(digits.group(0)) if digits else None


def is_jdk(home):
    javac = os.path.join(home, "bin", "javac" + EXE)
    return os.path.isfile(javac) and os.access(javac, os.X_OK)


def version_key(path):
    """Natural order, like `sort -V`: 17.0.14 after 17.0.8, a name after a number."""
    return [(0, int(p), "") if p.isdigit() else (1, 0, p) for p in re.findall(r"\d+|\D+", path)]


def candidates():
    out = [os.environ.get("OPENSILEX_JAVA_HOME"), os.environ.get("JAVA_HOME")]
    home = os.path.expanduser("~")
    patterns = [os.path.join(os.environ.get("SDKMAN_DIR") or os.path.join(home, ".sdkman"), "candidates", "java", "*"),
                os.path.join(home, ".jdks", "*"),                                   # JDKs downloaded by IntelliJ
                "/usr/lib/jvm/*",
                "/Library/Java/JavaVirtualMachines/*/Contents/Home"]
    if WINDOWS:
        for var in ("ProgramFiles", "ProgramW6432", "ProgramFiles(x86)"):
            if os.environ.get(var):                                                # <vendor>\<jdk>: Java, Eclipse Adoptium,
                patterns.append(os.path.join(os.environ[var], "*", "*"))           # Microsoft, Zulu, Amazon Corretto, ...
        patterns.append(os.path.join(home, "scoop", "apps", "*", "current"))
    for p in patterns:
        out.extend(d for d in sorted(glob.glob(p)) if os.path.isdir(d))
    java = shutil.which("java")
    if java:
        out.append(os.path.dirname(os.path.dirname(os.path.realpath(java))))
    return [c for c in out if c]


def find_home():
    want = level()
    if not want:
        raise JdkError("cannot read java.compiler.version from %s" % POM)
    want = int(want)
    explicit = os.environ.get("OPENSILEX_JAVA_HOME")
    best = None
    # an explicit override wins; otherwise the highest version among the matching JDKs
    for c in candidates():
        if not is_jdk(c) or major_of(c) != want:
            continue
        if explicit and c == explicit:
            return c
        if best is None or version_key(c) > version_key(best):
            best = c
    if best is None:
        hint = "winget install EclipseAdoptium.Temurin.%d.JDK" % want if WINDOWS else "sdk install java %d.0.14-tem" % want
        raise JdkError("no JDK %d found (looked at OPENSILEX_JAVA_HOME, JAVA_HOME, SDKMAN, ~/.jdks, /usr/lib/jvm, macOS JVMs,\n"
                       "        Windows Program Files and Scoop, PATH).\n"
                       "        Install one (e.g. '%s') or set OPENSILEX_JAVA_HOME." % (want, hint))
    return best


def find_home_min(need):
    """Lowest installed JDK whose major version is >= `need`.

    For tools that need a newer runtime than the project release (SonarLint's backend needs 21 while the code is
    compiled for 17); OPENSILEX_TOOLS_JAVA_HOME overrides.
    """
    tools = os.environ.get("OPENSILEX_TOOLS_JAVA_HOME")
    if tools and is_jdk(tools):
        return tools
    best, best_major = None, 0
    for c in candidates():
        if not is_jdk(c):
            continue
        m = major_of(c)
        if m is None or m < need:
            continue
        if best is None or m < best_major or (m == best_major and version_key(c) > version_key(best)):
            best, best_major = c, m
    if best is None:
        raise JdkError("no JDK >= %d found (set OPENSILEX_TOOLS_JAVA_HOME)." % need)
    return best


def maven(args, env=None):
    """Command line and environment that run Maven with `args` under the project JDK."""
    home = find_home()
    env = dict(os.environ if env is None else env)
    env["JAVA_HOME"] = home
    env["PATH"] = os.path.join(home, "bin") + os.pathsep + env.get("PATH", "")
    mvn = shutil.which("mvn", path=env["PATH"])                                  # mvn.cmd on Windows
    if not mvn:
        raise JdkError("mvn not found on PATH")
    return [mvn] + list(args), env


def env_lines(shell):
    home = find_home()
    bin_dir = os.path.join(home, "bin")
    if shell == "powershell":
        def quote(s):
            return "'" + s.replace("'", "''") + "'"
        return ["$env:JAVA_HOME = %s" % quote(home),
                "$env:PATH = %s + [IO.Path]::PathSeparator + $env:PATH" % quote(bin_dir)]
    if shell == "cmd":
        return ['set "JAVA_HOME=%s"' % home, 'set "PATH=%s;%%PATH%%"' % bin_dir]
    return ["export JAVA_HOME=%s" % shlex.quote(home),
            "export PATH=%s" % shlex.quote(bin_dir + os.pathsep + os.environ.get("PATH", ""))]


def first_line(cmd, env=None):
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, env=env)
    except OSError:
        return ""
    lines = (r.stdout + r.stderr).strip().splitlines()
    return re.sub(r"\x1b\[[0-9;]*m", "", lines[0]) if lines else ""


def check():
    want = level()
    print("Project Java release : %s   (opensilex-parent/pom.xml: java.compiler.version)" % (want or "?"))
    java = shutil.which("java")
    print("Default java on PATH : %s" % (first_line([java, "-version"]) if java else "none"))
    try:
        home = find_home()
    except JdkError as e:
        print("Selected JDK         : NONE")
        print("jdk.py: %s" % e, file=sys.stderr)
        return 1
    print("Selected JDK         : %s  (major %s)" % (home, major_of(home)))
    try:
        cmd, env = maven(["-v"])
        print("Maven under that JDK : %s" % (first_line(cmd, env) or "mvn not found"))
    except JdkError:
        print("Maven under that JDK : mvn not found")
    return 0


def main(argv):
    cmd, args = (argv[0], argv[1:]) if argv else ("check", [])
    try:
        if cmd == "level":
            release = level()
            print(release)
            return 0 if release else 1
        if cmd == "home":
            print(find_home())
            return 0
        if cmd == "home-min":
            if len(args) != 1 or not args[0].isdigit():
                print("usage: jdk.py home-min <major>", file=sys.stderr)
                return 2
            print(find_home_min(int(args[0])))
            return 0
        if cmd == "env":
            shell = args[0] if args else ("powershell" if WINDOWS else "posix")
            if shell not in ("posix", "powershell", "cmd"):
                print("usage: jdk.py env [posix|powershell|cmd]", file=sys.stderr)
                return 2
            print("\n".join(env_lines(shell)))
            return 0
        if cmd == "mvn":
            mvn_cmd, env = maven(args)
            try:
                return subprocess.call(mvn_cmd, env=env)
            except KeyboardInterrupt:
                return 130
        if cmd == "check":
            return check()
    except JdkError as e:
        print("jdk.py: %s" % e, file=sys.stderr)
        return 1
    print("usage: jdk.py level|home|home-min N|env [SHELL]|check|mvn [args...]", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
