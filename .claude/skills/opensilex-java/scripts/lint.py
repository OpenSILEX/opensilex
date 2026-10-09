#!/usr/bin/env python3
"""OpenSILEX Java lint: project conventions + Checkstyle (project config) + SonarLint (when installed).

  lint.py                       lint the Java files changed against develop (+ uncommitted/untracked), changed lines only
  lint.py path/To.java dir/     lint the given files / directories (every line unless --changed-lines)
  lint.py --tools conventions   fast, offline, no Maven (default: conventions,checkstyle)
  lint.py --sonar               add SonarLint (scripts/sonarlint/sonarlint.sh must be usable, see lint.md)
  lint.py --fix                 apply the safe mechanical fixes (tabs, trailing blanks, final newline, `if(`) to the
                                reported lines only
  lint.py --format json         machine-readable output
Exit status: 1 when an `error` is reported (--fail-on warning|never to change).

Severity: error = breaks the build, tests or the architecture; warning = violates a measured convention;
info = legacy idiom / opportunity. Every rule id is explained in references/lint.md.
"""
import argparse
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict, namedtuple

HERE = os.path.dirname(os.path.abspath(__file__))
SKILL = os.path.dirname(HERE)
REPO = os.environ.get("OPENSILEX_LINT_REPO") \
    or subprocess.run(["git", "-C", HERE, "rev-parse", "--show-toplevel"], capture_output=True, text=True).stdout.strip() \
    or os.path.abspath(os.path.join(SKILL, "..", "..", ".."))

Finding = namedtuple("Finding", "path line col severity tool rule message fix")
SEV_ORDER = {"error": 0, "warning": 1, "info": 2}
TOKEN = re.compile(r'//[^\n]*|/\*.*?\*/|"""(?:.|\n)*?"""|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])+\'', re.S)
HEADER_RE = re.compile(r"OpenSILEX - Licence AGPL")
UPPER_SNAKE = re.compile(r"^[A-Z][A-Z0-9_]*$")


# ----------------------------------------------------------------------------------------------- helpers
def git(*args):
    return subprocess.run(["git", "-C", REPO] + list(args), capture_output=True, text=True).stdout


def project_release():
    try:
        pom = open(os.path.join(REPO, "opensilex-parent", "pom.xml"), encoding="utf-8").read()
        return int(re.search(r"<java\.compiler\.version>(\d+)</java\.compiler\.version>", pom).group(1))
    except (OSError, AttributeError):
        return 17


def merge_base(base):
    for ref in ([base] if base else []) + ["origin/develop", "develop", "HEAD"]:
        r = subprocess.run(["git", "-C", REPO, "merge-base", "HEAD", ref], capture_output=True, text=True)
        if r.returncode == 0 and r.stdout.strip():
            return r.stdout.strip()
    return "HEAD"


def is_java_source(rel):
    return rel.endswith(".java") and ("/src/main/java/" in rel or "/src/test/java/" in rel) \
        and not any(s in "/" + rel for s in ("/target/", "/node_modules/", "/.kilo/", "/graft/", "/.claude/", "/front/"))


def changed_files(mb):
    names = set(git("diff", "--name-only", "--diff-filter=ACMR", mb).splitlines())
    names |= set(git("ls-files", "--others", "--exclude-standard").splitlines())
    return sorted(n for n in names if is_java_source(n) and os.path.isfile(os.path.join(REPO, n)))


def new_files(mb):
    names = set(git("diff", "--name-only", "--diff-filter=A", mb).splitlines())
    names |= set(git("ls-files", "--others", "--exclude-standard").splitlines())
    return names


def expand_paths(paths):
    out = []
    for p in paths:
        ap = p if os.path.isabs(p) else os.path.join(os.getcwd(), p)
        if os.path.isdir(ap):
            for dp, dns, fns in os.walk(ap):
                dns[:] = [d for d in dns if d not in ("target", "node_modules", ".git", ".kilo", "graft", "front")]
                for fn in fns:
                    rel = os.path.relpath(os.path.join(dp, fn), REPO)
                    if is_java_source(rel):
                        out.append(rel)
        elif os.path.isfile(ap):
            out.append(os.path.relpath(ap, REPO))
        else:
            print("lint.py: no such file or directory: %s" % p, file=sys.stderr)
    return sorted(set(out))


def changed_ranges(rel, mb):
    out = git("diff", "-U0", mb, "--", rel)
    ranges = []
    for m in re.finditer(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", out, re.M):
        start = int(m.group(1))
        count = int(m.group(2)) if m.group(2) is not None else 1
        if count > 0:
            ranges.append((start, start + count - 1))
    return ranges


def blank(m):
    return re.sub(r"[^\n]", " ", m.group(0))


class Ctx:
    def __init__(self, rel, release, is_new):
        self.rel = rel
        self.release = release
        self.is_new = is_new
        self.is_test = "/src/test/java/" in rel
        self.module = rel.split("/")[0]
        self.name = os.path.basename(rel)[:-5]
        raw = open(os.path.join(REPO, rel), encoding="utf-8", errors="replace").read()
        self.raw = raw
        self.ends_with_newline = raw.endswith("\n")
        self.lines = raw.split("\n")
        self.code = TOKEN.sub(blank, raw)
        self.code_lines = self.code.split("\n")
        m = re.search(r"^package\s+([\w.]+);", raw, re.M)
        self.package = m.group(1) if m else ""
        self.in_layer = {layer: ("." + layer + ".") in (self.package + ".") for layer in ("api", "dal", "bll")}

    def find(self, rx, severity, rule, msg, fix=None, code=True, skip=None):
        """Yield one Finding per line matching rx (default on the comment/string-blanked source)."""
        src = self.code_lines if code else self.lines
        for i, line in enumerate(src, 1):
            for m in rx.finditer(line):
                if skip and skip(line, m):
                    continue
                yield Finding(self.rel, i, m.start() + 1, severity, "conventions", rule,
                              msg(m) if callable(msg) else msg, fix)


# ----------------------------------------------------------------------------------------------- rules
def r_format(c):
    for i, line in enumerate(c.lines, 1):
        s = line.rstrip("\r")
        if re.match(r"^\t", s):
            yield Finding(c.rel, i, 1, "error", "conventions", "FMT-TAB",
                          "Tab in indentation: the project indents with 4 spaces (Checkstyle FileTabCharacter)", "tab")
        if s != s.rstrip():
            yield Finding(c.rel, i, len(s.rstrip()) + 1, "warning", "conventions", "FMT-TRAILING-WS",
                          "Trailing whitespace", "trailing")
        width = len(s.expandtabs(4))
        if width > 150 and "http" not in s and not s.lstrip().startswith("import "):
            yield Finding(c.rel, i, 151, "warning", "conventions", "FMT-LINE-LENGTH",
                          "Line is %d columns long (Checkstyle limit 150, aim at 120): wrap it" % width, None)
        elif width > 120 and "http" not in s and not s.lstrip().startswith("import "):
            yield Finding(c.rel, i, 121, "info", "conventions", "FMT-LINE-LENGTH",
                          "Line is %d columns long (aim at <= 120)" % width, None)
    if c.raw and not c.ends_with_newline:
        yield Finding(c.rel, len(c.lines), 1, "warning", "conventions", "FMT-EOF-NEWLINE",
                      "File does not end with a newline", "eof")
    yield from c.find(re.compile(r"\b(?:if|for|while|switch|catch|synchronized)\("), "warning", "FMT-KEYWORD-SPACE",
                      "Missing space between the keyword and '(' (`if (`)", "kwspace")
    yield from c.find(re.compile(r"^\s*\{\s*$"), "warning", "FMT-BRACE-OWN-LINE",
                      "Opening brace alone on its line: the project uses K&R braces (`{` at the end of the line)")


def r_imports(c):
    imports = []
    for i, line in enumerate(c.lines, 1):
        m = re.match(r"^import\s+(static\s+)?([\w.]+?)(\.\*)?;", line)
        if m:
            imports.append((i, bool(m.group(1)), m.group(2), bool(m.group(3))))
    for i, is_static, name, star in imports:
        if star and not is_static:
            yield Finding(c.rel, i, 1, "warning", "conventions", "IMP-STAR",
                          "Wildcard import %s.*: use explicit imports (Checkstyle AvoidStarImport, Sonar S2208)" % name, None)
        if re.match(r"jakarta\.(ws|validation|inject|annotation|servlet|persistence|enterprise)\b", name):
            yield Finding(c.rel, i, 1, "error", "conventions", "IMP-JAKARTA",
                          "The project is on javax.* (javax.ws.rs, javax.validation, javax.inject), not %s" % name, None)
        if c.is_test and name.startswith("org.junit.jupiter"):
            yield Finding(c.rel, i, 1, "error", "conventions", "TEST-JUNIT5",
                          "Tests use JUnit 4.13.2 (org.junit.*): do not introduce JUnit 5", None)
        if not star:
            simple = name.rsplit(".", 1)[-1]
            body = "\n".join(l for l in c.code_lines if not l.startswith("import "))
            in_javadoc = re.search(r"\b%s\b" % re.escape(simple), c.raw.replace(
                "\n".join(l for l in c.lines if l.startswith("import ")), ""))
            if not re.search(r"\b%s\b" % re.escape(simple), body) and not in_javadoc:
                yield Finding(c.rel, i, 1, "warning", "conventions", "IMP-UNUSED", "Unused import %s" % name, None)
    if c.is_new:
        seen_java = False
        for i, is_static, name, star in imports:
            if is_static:
                continue
            if name.startswith(("java.", "javax.")):
                seen_java = True
            elif seen_java:
                yield Finding(c.rel, i, 1, "info", "conventions", "IMP-ORDER",
                              "Import order: other packages first, then javax.*, java.*, then static imports "
                              "(IntelliJ default layout used by the team)", None)
                break


def r_header(c):
    head = "\n".join(c.lines[:14])
    if c.is_new and not HEADER_RE.search(head):
        yield Finding(c.rel, 1, 1, "warning", "conventions", "HDR-MISSING",
                      "New file without the OpenSILEX license header (CONTRIBUTING.md); "
                      "scaffold.py generates the current one", None)


def r_naming(c):
    if c.is_new and re.search(r"[A-Z_]", c.package):
        yield Finding(c.rel, 1, 1, "warning", "conventions", "NAME-PACKAGE",
                      "Package %s must be all lowercase (Checkstyle PackageName); camelCase packages are legacy" % c.package, None)
    if c.in_layer["dal"] and re.search(r"(DTO|API)$", c.name):
        yield Finding(c.rel, 1, 1, "error", "conventions", "NAME-LAYER",
                      "%s lives in a dal package: DTOs and API classes belong to the api package" % c.name, None)
    if c.in_layer["api"] and re.search(r"(DAO|Model)$", c.name) and not c.is_test:
        yield Finding(c.rel, 1, 1, "warning", "conventions", "NAME-LAYER",
                      "%s lives in an api package: models and DAOs belong to the dal package" % c.name, None)
    if c.is_test and "@Test" in c.code and not re.search(r"(Test|Tests|TestCase)$|^Test", c.name) \
            and not re.search(r"\babstract\s+class\b", c.code):
        yield Finding(c.rel, 1, 1, "error", "conventions", "NAME-TEST-SUFFIX",
                      "%s has @Test methods but its name does not match the surefire defaults (Test*, *Test, *Tests, "
                      "*TestCase): Maven will never run it" % c.name, None)
    const = re.compile(r"\bstatic\s+final\s+(?:String|int|long|double|float|boolean|char|short|byte|Integer|Long|Double|"
                       r"Boolean|URI|Pattern)\s+([A-Za-z_]\w*)\s*[=;]")
    for i, line in enumerate(c.code_lines, 1):
        for m in const.finditer(line):
            if not UPPER_SNAKE.match(m.group(1)):
                yield Finding(c.rel, i, m.start(1) + 1, "warning", "conventions", "NAME-CONSTANT",
                              "Constant %s must be UPPER_SNAKE_CASE" % m.group(1), None)
    for i, line in enumerate(c.code_lines, 1):
        m = re.search(r"\bstatic\s+final\s+Logger\s+(\w+)\s*=", line) or re.search(r"\bstatic\s+Logger\s+(\w+)\s*=", line)
        if m and m.group(1) != "LOGGER":
            yield Finding(c.rel, i, m.start(1) + 1, "info", "conventions", "NAME-LOGGER",
                          "Static logger is named LOGGER in the code base (found %s)" % m.group(1), None)


def r_architecture(c):
    if c.in_layer["dal"] and not c.is_test:
        for i, line in enumerate(c.lines, 1):
            m = re.match(r"^import\s+(?:static\s+)?(org\.opensilex\.[\w.]*?\.api\.[\w.*]+);", line)
            if m:
                yield Finding(c.rel, i, 1, "error", "conventions", "ARCH-DAL-API",
                              "dal must not import api classes or DTOs (%s): move the type to dal or convert in the API layer" % m.group(1), None)


def r_logging_exceptions(c):
    if not c.is_test and c.module not in ("opensilex-dev-tools",):
        yield from c.find(re.compile(r"\bSystem\.(?:out|err)\."), "warning", "LOG-SYSOUT",
                          "System.out/err: use the SLF4J LOGGER")
    yield from c.find(re.compile(r"\.printStackTrace\("), "warning", "LOG-STACKTRACE",
                      "printStackTrace(): log with LOGGER.error(\"message\", e) instead")
    for i, (raw_line, code_line) in enumerate(zip(c.lines, c.code_lines), 1):
        if re.search(r"\b(?:LOGGER|logger)\.(?:trace|debug|info|warn|error)\(", code_line) and re.search(r'"\s*\+', raw_line):
            yield Finding(c.rel, i, 1, "info", "conventions", "LOG-CONCAT",
                          "String concatenation in a log call: use SLF4J {} placeholders", None)
    for m in re.finditer(r"catch\s*\([^)]*\)\s*\{\s*\}", c.code):
        line = c.code.count("\n", 0, m.start()) + 1
        has_comment = bool(re.search(r"//|/\*", c.raw[m.start():m.end() + 200].split("}")[0] if False else
                                     "\n".join(c.lines[line - 1:line + 1])))
        yield Finding(c.rel, line, 1, "info" if has_comment else "warning", "conventions", "EXC-EMPTY-CATCH",
                      "Empty catch block: log it, rethrow it, or explain in a comment why ignoring it is safe", None)


def endpoints(c):
    """Yield (line_no, method_name, annotation_names, class_protected) for every JAX-RS method."""
    if "@Path(" not in c.code:
        return
    lines = [l.expandtabs(4) for l in c.lines]
    class_protected = bool(re.search(r"^@ApiProtected\b", c.raw, re.M))
    sig = re.compile(r" {4}public\s+(?:static\s+)?(?:<[^>]+>\s+)?[\w<>\[\],.? ]+?\s+(\w+)\s*\(")
    for i, line in enumerate(lines):
        m = sig.match(line)
        if not m:
            continue
        j = i - 1
        while j >= 0 and not re.match(r" {4}(?![@ /*])\S.*[;{}]\s*$", lines[j]):
            j -= 1
        names = re.findall(r"^ {4}@(\w+)", "\n".join(lines[j + 1:i]), re.M)
        if any(v in names for v in ("GET", "POST", "PUT", "DELETE", "PATCH")):
            yield i + 1, m.group(1), names, class_protected, "\n".join(lines[j + 1:i])


def r_api(c):
    if c.is_test:
        return
    for line, name, names, class_protected, chunk in endpoints(c):
        if "ApiProtected" not in names and not class_protected:
            yield Finding(c.rel, line, 1, "warning", "conventions", "API-PROTECTED",
                          "Endpoint %s() is public: add @ApiProtected unless it must be anonymous (login, public info)" % name, None)
        if "ApiOperation" not in names:
            yield Finding(c.rel, line, 1, "warning", "conventions", "API-OPERATION",
                          "Endpoint %s() has no @ApiOperation (Swagger feeds the generated TypeScript client)" % name, None)
        if "ApiResponses" not in names:
            yield Finding(c.rel, line, 1, "info", "conventions", "API-RESPONSES",
                          "Endpoint %s() has no @ApiResponses (98 %% of endpoints document their statuses)" % name, None)
        if ("PUT" in names or "DELETE" in names) and "ApiProtected" in names and "ApiCredential" not in names:
            yield Finding(c.rel, line, 1, "warning", "conventions", "API-CREDENTIAL",
                          "Write endpoint %s() has no @ApiCredential: access cannot be granted per profile" % name, None)
        if "GET" in names and re.search(r'@Path\("by_uris"\)', chunk) and "Deprecated" not in names:
            yield Finding(c.rel, line, 1, "warning", "conventions", "API-GET-BY-URIS",
                          "GET by_uris breaks on long URI lists: use POST by_uris with a JSON body", None)
    if c.module not in ("opensilex-brapi", "opensilex-faidare"):
        yield from c.find(re.compile(r'@QueryParam\("([a-z][a-z0-9]*[A-Z]\w*)"\)'), "warning", "API-QUERYPARAM-CASE",
                          lambda m: "Query parameter %s must be snake_case (BrAPI/FAIDARE are the only exceptions)" % m.group(1),
                          code=False)


def r_model(c):
    if c.is_test or "@SPARQLResource" not in c.code:
        return
    m = re.search(r"\b(final\s+)?(?:abstract\s+)?(class|record)\s+(\w+)", c.code)
    if m and (m.group(1) or m.group(2) == "record"):
        yield Finding(c.rel, c.code.count("\n", 0, m.start()) + 1, 1, "error", "conventions", "MODEL-FINAL",
                      "SPARQL models must be non-final classes (the mapper and the lazy proxies subclass them)", None)
    cls = m.group(3) if m else c.name
    ctors = re.findall(r"\b(?:public|protected|private)\s+%s\s*\(([^)]*)\)" % re.escape(cls), c.code)
    if ctors and not any(not p.strip() for p in ctors):
        yield Finding(c.rel, 1, 1, "error", "conventions", "MODEL-NOARG",
                      "SPARQL models need a public no-arg constructor", None)
    lines = c.code_lines
    field_rx = re.compile(r"\s*(?:(?:private|protected|public|final|transient|volatile)\s+)*(?!static\b)[\w<>\[\],.? ]+?\s+(\w+)\s*(?:=|;)")
    for i, line in enumerate(lines):
        if re.match(r"\s*@SPARQLProperty\b", line):
            # skip the (possibly multi-line) annotation, then read the declaration that follows
            depth, j = 0, i
            while j < len(lines):
                depth += lines[j].count("(") - lines[j].count(")")
                j += 1
                if depth <= 0:
                    break
            while j < len(lines) and (not lines[j].strip() or lines[j].lstrip().startswith("@")):
                j += 1
            fm = field_rx.match(lines[j]) if j < len(lines) else None
            if not fm:
                continue
            field = fm.group(1)
            cap = field[0].upper() + field[1:]
            if not re.search(r"\b(?:get|is)%s\s*\(" % cap, c.code) or not re.search(r"\bset%s\s*\(" % cap, c.code):
                yield Finding(c.rel, j + 1, 1, "warning", "conventions", "MODEL-ACCESSORS",
                              "Mapped field %s needs a getter and a setter in this class or a superclass "
                              "(the mapper goes through accessors)" % field, None)
            const = re.sub(r"(?<!^)(?=[A-Z])", "_", field).upper() + "_FIELD"
            if not re.search(r"\bstatic\s+final\s+String\s+%s\b" % const, c.code):
                yield Finding(c.rel, j + 1, 1, "info", "conventions", "MODEL-FIELD-CONSTANT",
                              "No %s constant for field %s (24 of 42 models define one; use it in DAO filters/schemas)" % (const, field), None)


RELEASE_FEATURES = [
    (21, r"\bThread\.(?:ofVirtual|ofPlatform)\b|\bExecutors\.newVirtualThreadPerTaskExecutor\b", "virtual threads"),
    (21, r"\bMath\.clamp\(", "Math.clamp"),
    (21, r"\bcase\s+[A-Za-z_][\w.<>]*\s+[a-z_]\w*\s+when\b|\bcase\s+null\b|\bcase\s+[A-Z]\w*\s*\(", "pattern matching for switch / record patterns"),
    (17, r"\b(?:sealed|non-sealed)\s+(?:abstract\s+)?(?:class|interface)\b|\bpermits\s+[A-Z]", "sealed classes"),
    (16, r"\brecord\s+[A-Z]\w*\s*(?:<[^>]*>)?\s*\(", "records"),
    (16, r"\binstanceof\s+(?:final\s+)?[A-Z][\w.]*(?:<[^>]*>)?\s+[a-z_]\w*", "instanceof pattern matching"),
    (16, r"\.toList\(\)", "Stream.toList()"),
    (14, r"\b(?:case\b[^:;{}\n]*|default)\s*->", "switch expressions"),
]


def r_java(c):
    for minimum, rx, what in RELEASE_FEATURES:
        if minimum > c.release:
            yield from c.find(re.compile(rx), "error", "JAVA-RELEASE",
                              "%s needs Java %d but the project release is %d (opensilex-parent/pom.xml)" % (what, minimum, c.release))
    if c.release < 15 and '"""' in c.raw:
        yield from c.find(re.compile(r'"""'), "error", "JAVA-RELEASE", "text blocks need Java 15; project release is %d" % c.release, code=False)
    yield from c.find(re.compile(r"\bnew\s+Date\(|\bCalendar\.getInstance\(|\bnew\s+SimpleDateFormat\("), "info", "JAVA-LEGACY-DATE",
                      "Legacy date API: use java.time (OffsetDateTime, Instant, LocalDate)")
    yield from c.find(re.compile(r"\bnew\s+(?:Vector|Hashtable|Stack)\s*<|\bStringBuffer\b"), "info", "JAVA-LEGACY-COLLECTION",
                      "Legacy synchronized class: use ArrayList/HashMap/ArrayDeque/StringBuilder")
    yield from c.find(re.compile(r"[(,]\s*(?:final\s+)?Optional<[^>]*>\s+\w+\s*[,)]|\b(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?Optional<"),
                      "warning", "JAVA-OPTIONAL-MISUSE", "Optional as parameter or field: use it as a return type only")
    yield from c.find(re.compile(r"[(,]\s*final\s+[\w<>\[\],.? ]+?\s+\w+\s*[,)]"), "info", "JAVA-FINAL-PARAM",
                      "`final` on a method parameter is legacy (not used in recent code)",
                      skip=lambda line, m: bool(re.search(r"\b(?:catch|for|try)\s*\(", line)))
    yield from c.find(re.compile(r'@SuppressWarnings\(\s*"all"\s*\)'), "warning", "JAVA-SUPPRESS-ALL",
                      'Do not use @SuppressWarnings("all"): name the warning ("unchecked") on the smallest scope', code=False)


def r_tests(c):
    if not c.is_test:
        return
    yield from c.find(re.compile(r"\bThread\.sleep\("), "warning", "TEST-SLEEP",
                      "Thread.sleep in a test: flaky; wait on a condition or inject the clock")
    yield from c.find(re.compile(r"@Ignore\b(?!\s*\()"), "info", "TEST-IGNORE", "@Ignore without a reason: write why and link the issue")
    yield from c.find(re.compile(r"\bSystem\.(?:out|err)\."), "info", "TEST-SYSOUT", "System.out in a test: assert instead")


RULES = [r_format, r_imports, r_header, r_naming, r_architecture, r_logging_exceptions, r_api, r_model, r_java, r_tests]


# ----------------------------------------------------------------------------------------------- fixes
def apply_fixes(findings):
    by_file = defaultdict(list)
    for f in findings:
        if f.fix:
            by_file[f.path].append(f)
    done = 0
    for rel, fs in by_file.items():
        path = os.path.join(REPO, rel)
        raw = open(path, encoding="utf-8", errors="replace").read()
        lines = raw.split("\n")
        ctx_code = TOKEN.sub(blank, raw).split("\n")
        per_line = defaultdict(set)
        for f in fs:
            per_line[f.line].add(f.fix)
        for lineno, kinds in sorted(per_line.items()):
            i = lineno - 1
            if i >= len(lines):
                continue
            # order matters: the keyword positions come from the original line, so they are applied first
            if "kwspace" in kinds:
                positions = [m.end() - 1 for m in re.finditer(r"\b(?:if|for|while|switch|catch|synchronized)\(", ctx_code[i])]
                for pos in reversed(positions):
                    lines[i] = lines[i][:pos] + " " + lines[i][pos:]
                    done += 1
            if "tab" in kinds:
                lead = re.match(r"^\t+", lines[i])
                if lead:
                    lines[i] = "    " * len(lead.group(0)) + lines[i][len(lead.group(0)):]
                    done += 1
            if "trailing" in kinds:
                new = lines[i].rstrip()
                if new != lines[i]:
                    lines[i] = new
                    done += 1
            if "eof" in kinds and lines and lines[-1] != "":
                lines.append("")
                done += 1
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write("\n".join(lines))
    return done, len(by_file)


# ----------------------------------------------------------------------------------------------- checkstyle
CS_KEEP = {
    "UnusedImportsCheck", "AvoidStarImportCheck", "RedundantImportCheck", "IllegalImportCheck", "WhitespaceAroundCheck",
    "WhitespaceAfterCheck", "NoWhitespaceAfterCheck", "NoWhitespaceBeforeCheck", "ParenPadCheck", "MethodParamPadCheck",
    "GenericWhitespaceCheck", "EmptyForIteratorPadCheck", "TypecastParenPadCheck", "OperatorWrapCheck", "NeedBracesCheck",
    "LeftCurlyCheck", "RightCurlyCheck", "EmptyBlockCheck", "EmptyStatementCheck", "MissingSwitchDefaultCheck",
    "MultipleVariableDeclarationsCheck", "SimplifyBooleanExpressionCheck", "SimplifyBooleanReturnCheck", "EqualsHashCodeCheck",
    "InnerAssignmentCheck", "IllegalInstantiationCheck", "ArrayTypeStyleCheck", "UpperEllCheck", "ConstantNameCheck",
    "LocalFinalVariableNameCheck", "LocalVariableNameCheck", "MemberNameCheck", "MethodNameCheck", "PackageNameCheck",
    "ParameterNameCheck", "StaticVariableNameCheck", "TypeNameCheck", "LineLengthCheck", "FileTabCharacterCheck",
    "NewlineAtEndOfFileCheck", "RegexpSinglelineCheck",
}
# checks already reported by the conventions tool: dropped from Checkstyle output (same line) to avoid duplicates
CS_DUP = {"FileTabCharacterCheck": "FMT-TAB", "RegexpSinglelineCheck": "FMT-TRAILING-WS", "NewlineAtEndOfFileCheck": "FMT-EOF-NEWLINE",
          "LineLengthCheck": "FMT-LINE-LENGTH", "AvoidStarImportCheck": "IMP-STAR", "UnusedImportsCheck": "IMP-UNUSED",
          "WhitespaceAfterCheck": "FMT-KEYWORD-SPACE"}


def run_checkstyle(files, verbose):
    by_module = defaultdict(list)
    for rel in files:
        by_module[rel.split("/src/")[0]].append(rel)
    findings = []
    jdk = os.path.join(HERE, "jdk.sh")
    for module, rels in sorted(by_module.items()):
        site = os.path.join(REPO, "site")
        before = {p: os.path.getmtime(p) for p in _xml_reports(site)}
        includes = ",".join("**/" + os.path.basename(r) for r in rels) if len(rels) <= 40 else None
        cmd = ["bash", jdk, "mvn", "-o", "-B", "-q", "-pl", module, "checkstyle:checkstyle", "-DskipFrontBuild",
               "-Dcheckstyle.includeTestSourceDirectory=true"]
        if includes:
            cmd.append("-Dcheckstyle.includes=" + includes)
        env = dict(os.environ, MAVEN_OPTS=(os.environ.get("MAVEN_OPTS", "") + " -Duser.language=en -Duser.country=US").strip())
        r = subprocess.run(cmd, cwd=REPO, capture_output=True, text=True, env=env)
        if verbose or r.returncode != 0:
            tail = (r.stdout + r.stderr).strip().splitlines()[-6:]
            if r.returncode != 0:
                print("lint.py: checkstyle failed for %s (offline Maven resolution?):\n  %s" % (module, "\n  ".join(tail)), file=sys.stderr)
                continue
        wanted = {os.path.join(REPO, r) for r in rels}
        for xml in _xml_reports(site):
            if os.path.getmtime(xml) <= before.get(xml, 0):
                continue
            for f in ET.parse(xml).getroot().iter("file"):
                if f.get("name") not in wanted:
                    continue
                rel = os.path.relpath(f.get("name"), REPO)
                for e in f.iter("error"):
                    check = e.get("source", "").rsplit(".", 1)[-1]
                    if check not in CS_KEEP:
                        continue
                    findings.append(Finding(rel, int(e.get("line", "1")), int(e.get("column", "1") or 1), "warning", "checkstyle",
                                            check[:-5], e.get("message", ""), None))
    return findings


def _xml_reports(site):
    out = []
    if os.path.isdir(site):
        for d in os.listdir(site):
            p = os.path.join(site, d, "checkstyle-result.xml")
            if os.path.isfile(p):
                out.append(p)
    return out


# ----------------------------------------------------------------------------------------------- sonarlint
def run_sonar(files, verbose, extra):
    script = os.path.join(HERE, "sonarlint", "sonarlint.sh")
    if not os.path.isfile(script):
        print("lint.py: SonarLint runner not installed (scripts/sonarlint/sonarlint.sh missing); skipped", file=sys.stderr)
        return []
    cmd = ["bash", script, "--all-lines"] + extra + files
    r = subprocess.run(cmd, cwd=REPO, capture_output=True, text=True)
    if r.returncode not in (0, 1):
        print("lint.py: SonarLint failed (exit %d):\n  %s" % (r.returncode, "\n  ".join((r.stdout + r.stderr).strip().splitlines()[-8:])),
              file=sys.stderr)
        return []
    sev = {"BLOCKER": "error", "CRITICAL": "error", "HIGH": "error", "MAJOR": "warning", "MEDIUM": "warning",
           "MINOR": "info", "LOW": "info", "INFO": "info"}
    findings = []
    for line in r.stdout.splitlines():
        m = re.match(r"^(\S+\.java):(\d+):(\d+) \[(\w+)\] (\S+) (.*)$", line)
        if m:
            findings.append(Finding(m.group(1), int(m.group(2)), int(m.group(3)), sev.get(m.group(4).upper(), "warning"),
                                    "sonar", m.group(5), m.group(6), None))
    return findings


# ----------------------------------------------------------------------------------------------- main
def lint_conventions(files, new, release):
    out = []
    for rel in files:
        try:
            ctx = Ctx(rel, release, rel in new)
        except OSError as e:
            print("lint.py: cannot read %s: %s" % (rel, e), file=sys.stderr)
            continue
        for rule in RULES:
            out.extend(rule(ctx))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paths", nargs="*", help="Java files or directories (default: files changed against develop)")
    ap.add_argument("--base", help="base ref for the changed files (default: merge-base with origin/develop or develop)")
    ap.add_argument("--changed-lines", action="store_true", help="report only added/modified lines (default when no path is given)")
    ap.add_argument("--all-lines", action="store_true", help="report every line even when no path is given")
    ap.add_argument("--tools", default="conventions,checkstyle", help="comma list of: conventions, checkstyle, sonar")
    ap.add_argument("--sonar", action="store_true", help="shortcut for adding sonar to --tools")
    ap.add_argument("--sonar-args", default="", help="extra arguments passed to sonarlint.sh")
    ap.add_argument("--release", type=int, help="Java release (default: java.compiler.version of opensilex-parent/pom.xml)")
    ap.add_argument("--fix", action="store_true", help="apply the safe mechanical fixes to the reported lines")
    ap.add_argument("--format", choices=("text", "json"), default="text")
    ap.add_argument("--fail-on", choices=("error", "warning", "never"), default="error")
    ap.add_argument("--min-severity", choices=("error", "warning", "info"), default="info")
    ap.add_argument("--verbose", action="store_true")
    a = ap.parse_args()

    release = a.release or project_release()
    mb = merge_base(a.base)
    from_git = not a.paths
    files = changed_files(mb) if from_git else expand_paths(a.paths)
    if not files:
        print("lint.py: no Java file to lint (nothing changed against %s)." % mb[:10])
        return 0
    new = new_files(mb)
    use_ranges = (from_git and not a.all_lines) or a.changed_lines
    ranges = {}
    if use_ranges:
        for rel in files:
            ranges[rel] = None if rel in new else changed_ranges(rel, mb)

    tools = set(t.strip() for t in a.tools.split(",") if t.strip())
    if a.sonar:
        tools.add("sonar")
    findings = []
    if "conventions" in tools:
        findings += lint_conventions(files, new, release)
    if "checkstyle" in tools:
        findings += run_checkstyle(files, a.verbose)
    if "sonar" in tools:
        findings += run_sonar(files, a.verbose, a.sonar_args.split())

    # drop Checkstyle duplicates of conventions rules (same file, same line)
    have = {(f.path, f.line, f.rule) for f in findings if f.tool == "conventions"}
    findings = [f for f in findings if not (f.tool == "checkstyle" and (f.path, f.line, CS_DUP.get(f.rule + "Check")) in have)]

    hidden = 0
    if use_ranges:
        kept = []
        for f in findings:
            r = ranges.get(f.path)
            if r is None or any(lo <= f.line <= hi for lo, hi in r):
                kept.append(f)
            else:
                hidden += 1
        findings = kept
    findings = [f for f in findings if SEV_ORDER[f.severity] <= SEV_ORDER[a.min_severity]]
    findings.sort(key=lambda f: (f.path, f.line, f.col, SEV_ORDER[f.severity]))

    if a.fix:
        done, nfiles = apply_fixes(findings)
        print("lint.py --fix: %d fixes applied in %d file(s); re-run lint.py to see what is left" % (done, nfiles), file=sys.stderr)

    if a.format == "json":
        print(json.dumps([f._asdict() for f in findings], indent=1))
    else:
        for f in findings:
            print("%s:%d:%d: %s: [%s:%s] %s%s" % (f.path, f.line, f.col, f.severity, f.tool, f.rule, f.message,
                                                  "  (fixable with --fix)" if f.fix else ""))
        counts = Counter(f.severity for f in findings)
        by_tool = Counter(f.tool for f in findings)
        scope = "changed lines only, %d hidden" % hidden if use_ranges else "all lines"
        print("\nlint.py: %d file(s), release %d, %s -> %d error(s), %d warning(s), %d info  [%s]" % (
            len(files), release, scope, counts["error"], counts["warning"], counts["info"],
            ", ".join("%s=%d" % kv for kv in sorted(by_tool.items())) or "clean"))
    bad = {"error": ("error",), "warning": ("error", "warning"), "never": ()}[a.fail_on]
    return 1 if any(f.severity in bad for f in findings) else 0


if __name__ == "__main__":
    sys.exit(main())
