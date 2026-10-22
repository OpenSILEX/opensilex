#!/usr/bin/env python3
"""Mechanical check of the OpenSILEX Vue conventions on .vue files.

Usage:
  check_vue_conventions.py [file.vue ...]
  without arguments: the .vue files changed against HEAD, plus the untracked ones (git).

Output: one line per finding, `file:line: LEVEL [rule] message`, sorted by line.
  ERROR = breaks an explicit convention; WARN = needs a look (often a real problem).
Exit code: 1 when there is at least one ERROR, 0 otherwise.

This script does not replace a review: it sees neither dead code, nor the reuse of the
common/ building blocks, nor a wrapped FormSelector, nor hard-coded text.
"""

import argparse
import re
import subprocess
import sys

# Below this many script lines the whole script fits on one screen, so missing regions are tolerated.
MAX_SCRIPT_LINES_WITHOUT_REGIONS = 25

ACCEPTED_BLOCK_ORDERS = {
    "template script",
    "template script style",
    "template script i18n",
    "template script style i18n",
}

# Bootstrap layout and component classes; the d-flex, m*-, p*-, text-* utilities are fine.
BOOTSTRAP_CLASSES = (
    r"row|col(-[a-z0-9]+)*|btn(-[a-z-]+)?|card(-[a-z]+)?|form-group|form-control|form-select|form-check"
    r"|input-group|dropdown(-[a-z]+)?|table|list-group(-item)?|nav|nav-tabs|nav-item|nav-link"
    r"|alert(-[a-z]+)?|badge|modal(-[a-z]+)?|collapse|container(-fluid)?"
)

# One rule per line pattern: (level, rule, message, pattern, pattern that excuses a matching line).
LINE_RULE_PATTERNS = [
    ("ERROR", "style-scoped", 'expected <style scoped lang="scss">',
     r"^<style", r'^<style scoped lang="scss">'),
    # defineEmits: named-tuple syntax, not call overloads, not an array of strings
    ("ERROR", "emits-syntax", "defineEmits with call-overload syntax: write  name: [payload: Type]",
     r"""\(e: *['"]""", None),
    ("ERROR", "emits-untyped", "untyped defineEmits: write defineEmits<{ name: [payload: Type] }>()",
     r"defineEmits\(\[", None),
    # Template refs: useTemplateRef<InstanceType<typeof X>>('name')
    ("ERROR", "template-ref", "template ref as ref(null): use useTemplateRef<InstanceType<typeof X>>('name')",
     r"\bref(<[^>]*>)?\(null\)", None),
    ("WARN", "template-ref", "ref<InstanceType<…>>: a template ref is written with useTemplateRef",
     r"\bref<InstanceType", None),
    ("WARN", "template-ref-untyped", "useTemplateRef without a type parameter",
     r"""useTemplateRef\(['"]""", None),
    ("ERROR", "no-any", "type any: use the generated DTO/service, a naive-ui type or a local interface",
     r"(:\s*any\b|<any>|<any\[\]>|\bas any\b|\bany\[\]|Array<any>|Record<[^>]*,\s*any>|\(any\))",
     r"^\s*(//|\*)"),
    ("ERROR", "vue2", "Vue 2 / bootstrap-vue leftover",
     r"(<b-[a-z]|vue-property-decorator|@Component|@Prop\(|export default \{|this\.\$)", None),
    ("WARN", "vue2-emit", "$emit in the script: use the function returned by defineEmits",
     r"\$emit\(", r'(^\s*@|="|#)'),
    ("ERROR", "bootstrap",
     "Bootstrap component: replace it with the Naive UI equivalent (see references/bootstrap-to-naive.md)",
     r'class="([^"]* )?(' + BOOTSTRAP_CLASSES + r')( [^"]*)?"', None),
    ("ERROR", "bootstrap", "Bootstrap JS attribute (data-bs-*): use the Naive UI component",
     r"data-bs-", None),
    ("WARN", "console", "console.* left in the component",
     r"console\.(log|debug)\(", None),
    ("WARN", "function-type", "Function type too broad: type the signature",
     r":\s*Function\b", None),
]
LINE_RULES = [
    (level, rule, message, re.compile(pattern), re.compile(exclude) if exclude else None)
    for level, rule, message, pattern, exclude in LINE_RULE_PATTERNS
]

BLOCK_TAG = re.compile(r"^<(template|script|style|i18n)([ >]|$)")
SCRIPT_SETUP_TS = re.compile(r'^<script setup lang="ts">')
SCRIPT_OPEN = re.compile(r"^<script")
SCRIPT_CLOSE = re.compile(r"^</script>")
REGION_PUBLIC = re.compile(r"//\s*#region\s+Public")
REGION_PRIVATE = re.compile(r"//\s*#region\s+Private")
REGION_START = re.compile(r"//[ \t]*#region")
REGION_END = re.compile(r"//\s*#endregion")
REGION_TITLE_PREFIX = re.compile(r".*#region[ \t]*")
PUBLIC_MACRO = re.compile(r"\b(defineProps|defineEmits|defineModel)\b")
DEFINE_EXPOSE = re.compile(r"^\s*defineExpose\(")

# Expected order of the Private sub-regions, recognised by keywords in their title.
SUBREGION_RANKS = [
    (re.compile(r"plugin|service|inject|composable"), 1),
    (re.compile(r"template"), 2),
    (re.compile(r"data|computed|state|constant"), 3),
    (re.compile(r"hook|watch|lifecycle"), 4),
    (re.compile(r"event|handler"), 5),
    (re.compile(r"method|function"), 6),
]
SUBREGION_ORDER = "Plugins & services → Template refs → Data & Computed → Hooks & watchers → Event handlers → Methods"


def changed_vue_files():
    """The .vue files changed against HEAD and the untracked ones, relative to the current directory."""
    commands = [
        ["git", "diff", "--name-only", "--relative", "HEAD", "--", "*.vue"],
        ["git", "ls-files", "--others", "--exclude-standard", "--", "*.vue"],
    ]
    files = set()
    for command in commands:
        try:
            result = subprocess.run(command, capture_output=True, text=True, check=False)
        except OSError:
            continue
        files.update(line for line in result.stdout.splitlines() if line)
    return sorted(files)


def read_lines(path):
    """File lines without their newline; a final newline does not open an extra empty line."""
    with open(path, encoding="utf-8", errors="replace", newline="") as handle:
        text = handle.read()
    lines = text.split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    return lines


def first_line(pattern, lines):
    """1-based number of the first line matching pattern, or None."""
    for number, line in enumerate(lines, 1):
        if pattern.search(line):
            return number
    return None


def subregion_rank(title):
    title = title.lower()
    for pattern, rank in SUBREGION_RANKS:
        if pattern.search(title):
            return rank
    return 0


def check_block_order(lines):
    tags = [match.group(1) for match in map(BLOCK_TAG.match, lines) if match]
    order = " ".join(tags)
    if order not in ACCEPTED_BLOCK_ORDERS:
        yield 1, "ERROR", "block-order", f'block order "{order}"; expected: template script style i18n'
    if first_line(SCRIPT_SETUP_TS, lines) is None:
        yield first_line(SCRIPT_OPEN, lines) or 1, "ERROR", "script-setup", 'expected <script setup lang="ts">'


def check_lines(lines):
    for level, rule, message, pattern, exclude in LINE_RULES:
        for number, line in enumerate(lines, 1):
            if pattern.search(line) and not (exclude and exclude.search(line)):
                yield number, level, rule, message


def check_regions(lines):
    public = first_line(REGION_PUBLIC, lines)
    private = first_line(REGION_PRIVATE, lines)
    macro_lines = [number for number, line in enumerate(lines, 1) if PUBLIC_MACRO.search(line)]

    if private is None or (public is None and macro_lines):
        # The Public region is required only when the component has a public API.
        script_length = 0
        in_script = False
        for line in lines:
            if SCRIPT_OPEN.match(line):
                in_script = True
                continue
            if SCRIPT_CLOSE.match(line):
                in_script = False
            if in_script:
                script_length += 1
        if script_length > MAX_SCRIPT_LINES_WITHOUT_REGIONS:
            yield (1, "ERROR", "regions",
                   f"//#region Public and //#region Private missing (script of {script_length} lines)")
        return

    if public is not None and public >= private:
        yield private, "ERROR", "regions", "the Private region comes before the Public region"
    for number in macro_lines:
        if number >= private:
            yield number, "ERROR", "regions", "defineProps/defineEmits/defineModel outside the Public region"

    expose = first_line(DEFINE_EXPOSE, lines)
    region_ends = [number for number, line in enumerate(lines, 1) if REGION_END.search(line)]
    if expose is not None and region_ends and expose < region_ends[-1]:
        yield expose, "ERROR", "regions", "defineExpose() must come after the Private region is closed"

    highest_rank = 0
    for number, line in enumerate(lines, 1):
        if number <= private or not REGION_START.search(line):
            continue
        title = REGION_TITLE_PREFIX.sub("", line, count=1)
        rank = subregion_rank(title)
        if 0 < rank < highest_rank:
            yield (number, "WARN", "region-order",
                   f'sub-region "{title}" after a higher-ranked sub-region (order: {SUBREGION_ORDER})')
        if rank > 0:
            highest_rank = rank


def check_file(path):
    lines = read_lines(path)
    findings = [*check_block_order(lines), *check_lines(lines), *check_regions(lines)]
    # sorted() is stable: findings on the same line keep the order of the checks.
    return sorted(findings, key=lambda finding: finding[0])


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("files", nargs="*", help=".vue files to check (default: changed and untracked ones)")
    files = parser.parse_args().files or changed_vue_files()
    if not files:
        print("No .vue file to check.", file=sys.stderr)
        return 0

    errors = 0
    for path in files:
        try:
            findings = check_file(path)
        except OSError:
            print(f"{path}: not found", file=sys.stderr)
            continue
        for number, level, rule, message in findings:
            print(f"{path}:{number}: {level} [{rule}] {message}")
            if level == "ERROR":
                errors += 1

    if errors:
        print(f"— {errors} error(s).", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
