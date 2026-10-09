#!/usr/bin/env bash
# jdk.sh - find the JDK that matches the Java release of the project and run Maven with it.
#
#   jdk.sh level          print the project's Java release (java.compiler.version, e.g. 17)
#   jdk.sh home           print a JAVA_HOME whose major version == release (exit 1 when none)
#   jdk.sh home-min N     print the lowest installed JAVA_HOME with major >= N (for tools needing a newer runtime)
#   jdk.sh env            print `export JAVA_HOME=...` / `export PATH=...`   (use: eval "$(jdk.sh env)")
#   jdk.sh check          human-readable diagnostic (default java, selected JDK, Maven)
#   jdk.sh mvn [args...]  run Maven under the selected JDK
#
# Why: the `java` found first on PATH is often older than the project release (a JDK 11 cannot compile
# `release 17`), and Maven silently follows it. Never trust the shell default: go through this script.
#
# Override the search with OPENSILEX_JAVA_HOME=/path/to/jdk. The release is read from
# opensilex-parent/pom.xml (first <java.compiler.version>; the opt-in `for-java-11` profile that overrides
# it further down the file is ignored on purpose).
set -eu

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(git -C "$here" rev-parse --show-toplevel 2>/dev/null || (cd "$here/../../../.." && pwd))"

level() {
    sed -n 's:.*<java\.compiler\.version>\([0-9][0-9]*\)</java\.compiler\.version>.*:\1:p' \
        "$repo/opensilex-parent/pom.xml" | head -n 1
}

# major version of the JDK installed in $1 (empty when $1 is not a JDK)
major_of() {
    local v
    v="$(sed -n 's/^JAVA_VERSION="\(.*\)"/\1/p' "$1/release" 2>/dev/null | head -n 1)"
    [ -n "$v" ] || return 1
    case "$v" in
        1.*) printf '%s\n' "$v" | cut -d. -f2 ;;
        *)   printf '%s\n' "$v" | cut -d. -f1 | sed 's/[^0-9].*//' ;;
    esac
}

candidates() {
    [ -z "${OPENSILEX_JAVA_HOME:-}" ] || printf '%s\n' "$OPENSILEX_JAVA_HOME"
    [ -z "${JAVA_HOME:-}" ] || printf '%s\n' "$JAVA_HOME"
    local d
    for d in "${SDKMAN_DIR:-$HOME/.sdkman}"/candidates/java/*; do [ -d "$d" ] && printf '%s\n' "$d"; done
    for d in /usr/lib/jvm/*; do [ -d "$d" ] && printf '%s\n' "$d"; done
    for d in /Library/Java/JavaVirtualMachines/*/Contents/Home; do [ -d "$d" ] && printf '%s\n' "$d"; done
    if command -v java >/dev/null 2>&1; then
        d="$(readlink -f "$(command -v java)" 2>/dev/null || true)"
        [ -z "$d" ] || dirname "$(dirname "$d")"
    fi
}

find_home() {
    local want c m best=""
    want="$(level)"
    [ -n "$want" ] || { echo "jdk.sh: cannot read java.compiler.version from $repo/opensilex-parent/pom.xml" >&2; return 1; }
    # explicit overrides win, in the order candidates() lists them; otherwise the highest patch version
    for c in $(candidates); do
        [ -x "$c/bin/javac" ] || continue
        m="$(major_of "$c" || true)"
        [ "$m" = "$want" ] || continue
        if [ -n "${OPENSILEX_JAVA_HOME:-}" ] && [ "$c" = "$OPENSILEX_JAVA_HOME" ]; then best="$c"; break; fi
        if [ -z "$best" ] || [ "$(printf '%s\n%s\n' "$best" "$c" | sort -V | tail -n 1)" = "$c" ]; then best="$c"; fi
    done
    if [ -z "$best" ]; then
        echo "jdk.sh: no JDK $want found (looked at OPENSILEX_JAVA_HOME, JAVA_HOME, SDKMAN, /usr/lib/jvm, macOS JVMs, PATH)." >&2
        echo "        Install one (e.g. 'sdk install java ${want}.0.14-tem') or export OPENSILEX_JAVA_HOME." >&2
        return 1
    fi
    printf '%s\n' "$best"
}

# lowest installed JDK whose major version is >= $1 (tools that need a newer runtime than the project release,
# e.g. SonarLint's backend needs 21 while the code is compiled for 17); OPENSILEX_TOOLS_JAVA_HOME overrides
find_home_min() {
    local need="$1" c m best="" best_m=0
    if [ -n "${OPENSILEX_TOOLS_JAVA_HOME:-}" ] && [ -x "$OPENSILEX_TOOLS_JAVA_HOME/bin/javac" ]; then
        printf '%s\n' "$OPENSILEX_TOOLS_JAVA_HOME"
        return 0
    fi
    for c in $(candidates); do
        [ -x "$c/bin/javac" ] || continue
        m="$(major_of "$c" || true)"
        [ -n "$m" ] && [ "$m" -ge "$need" ] || continue
        if [ -z "$best" ] || [ "$m" -lt "$best_m" ] || { [ "$m" -eq "$best_m" ] && [ "$(printf '%s\n%s\n' "$best" "$c" | sort -V | tail -n 1)" = "$c" ]; }; then
            best="$c"
            best_m="$m"
        fi
    done
    if [ -z "$best" ]; then
        echo "jdk.sh: no JDK >= $need found (set OPENSILEX_TOOLS_JAVA_HOME)." >&2
        return 1
    fi
    printf '%s\n' "$best"
}

cmd="${1:-check}"
[ $# -eq 0 ] || shift
case "$cmd" in
    level) level ;;
    home)  find_home ;;
    home-min) find_home_min "${1:?usage: jdk.sh home-min <major>}" ;;
    env)
        home="$(find_home)"
        printf 'export JAVA_HOME=%q\nexport PATH=%q\n' "$home" "$home/bin:$PATH"
        ;;
    mvn)
        home="$(find_home)"
        JAVA_HOME="$home" PATH="$home/bin:$PATH" exec mvn "$@"
        ;;
    check)
        want="$(level)"
        echo "Project Java release : ${want:-?}   (opensilex-parent/pom.xml: java.compiler.version)"
        if command -v java >/dev/null 2>&1; then
            dflt="$(java -version 2>&1 | head -n 1)"
            echo "Default java on PATH : $dflt"
        else
            echo "Default java on PATH : none"
        fi
        if home="$(find_home 2>&1)"; then
            echo "Selected JDK         : $home  (major $(major_of "$home"))"
            echo "Maven under that JDK : $(JAVA_HOME="$home" PATH="$home/bin:$PATH" mvn -v 2>/dev/null | head -n 1 | sed 's/\x1b\[[0-9;]*m//g' || echo 'mvn not found')"
        else
            echo "Selected JDK         : NONE"
            printf '%s\n' "$home" >&2
            exit 1
        fi
        ;;
    *)
        echo "usage: jdk.sh level|home|env|check|mvn [args...]" >&2
        exit 2
        ;;
esac
