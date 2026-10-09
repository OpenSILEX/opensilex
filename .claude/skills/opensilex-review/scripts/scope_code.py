"""Light structure of Java and TypeScript sources: nesting, enclosing lines, methods, JAX-RS endpoints.

Line-based heuristics tuned on the OpenSILEX code (4-space indentation, one declaration per line). They locate
code for a human reviewer; they are not a parser.
"""
import re

METHOD_SIG_RE = re.compile(
    r"^\s*(public|protected|private)\s+(static\s+)?(final\s+)?(synchronized\s+)?"
    r"(<[^>]+>\s+)?[\w<>\[\],.? ]+\s+(\w+)\s*\(")
TS_METHOD_RE = re.compile(r"^\s*(public|private|protected)?\s*(async\s+)?(\w+)\s*\([^)]*\)\s*(:\s*[^={]+)?\{\s*$")
JAX_RS_VERB_RE = re.compile(r"^\s*@(GET|POST|PUT|DELETE|PATCH)\b")
LOOP_RE = re.compile(r"^\s*(for|while)\s*\(|^\s*do\s*\{|\.forEach\(|\.map\(|\.flatMap\(|\.filter\(|"
                     r"\.peek\(|\.reduce\(|\bv-for=")
STRING_RE = re.compile(r'"(\\.|[^"\\])*"' + r"|'(\\.|[^'\\])'")


def indent_of(line):
    expanded = line.replace("\t", "    ")
    return len(expanded) - len(expanded.lstrip(" "))


def parent_chain(lines, index):
    """Lines enclosing lines[index] by indentation, nearest first, up to the method or class header."""
    chain, current = [], indent_of(lines[index])
    for i in range(index - 1, max(-1, index - 400), -1):
        text = lines[i]
        if not text.strip() or text.strip().startswith(("//", "*", "/*", "@")):
            continue
        if indent_of(text) < current:
            chain.append((i, text))
            current = indent_of(text)
            if METHOD_SIG_RE.match(text) or re.match(r"^\s*(public |private |export )?(abstract )?class ", text):
                break
            if current == 0:
                break
    return chain


def inside_loop(lines, index):
    """The loop header enclosing lines[index] (or on the same line), else None."""
    if LOOP_RE.search(lines[index]):
        return lines[index]
    return next((t for _, t in parent_chain(lines, index) if LOOP_RE.search(t)), None)


def brace_depths(lines):
    """Brace depth at the start of each line (strings, chars and comments ignored)."""
    depths, depth, in_comment = [], 0, False
    for line in lines:
        depths.append(depth)
        code = STRING_RE.sub('""', line)
        if in_comment:
            if "*/" not in code:
                continue
            code, in_comment = code.split("*/", 1)[1], False
        code = re.sub(r"/\*.*?\*/", "", code.split("//", 1)[0])
        if "/*" in code:
            code, in_comment = code.split("/*", 1)[0], True
        depth += code.count("{") - code.count("}")
    return depths


def annotation_start(lines, index):
    """First line of the annotations above the declaration at lines[index], multi-line annotations included."""
    start, depth = index, 0
    for j in range(index - 1, -1, -1):
        code = STRING_RE.sub('""', lines[j].strip())
        closes_more = code.count(")") > code.count("(")
        if depth == 0 and not code.startswith("@") and not closes_more:
            break
        depth = max(0, depth + code.count(")") - code.count("("))
        start = j
    return start


def java_methods(lines):
    """[(name, first annotation line, last line)] of methods with a body (0-based indexes)."""
    methods, i = [], 0
    while i < len(lines):
        match = METHOD_SIG_RE.match(lines[i])
        if not match or lines[i].rstrip().endswith(";"):
            i += 1
            continue
        depth, started, j = 0, False, i
        while j < len(lines):
            code = STRING_RE.sub('""', lines[j]).split("//", 1)[0]
            depth += code.count("{") - code.count("}")
            started = started or "{" in code
            if (started and depth <= 0) or (not started and code.rstrip().endswith(";")):
                break
            j += 1
        if started:
            methods.append((match.group(6), annotation_start(lines, i), min(j, len(lines) - 1)))
            i = j + 1
        else:
            i += 1
    return methods


def endpoints(lines):
    """{method name: (first annotation line, last line)} for JAX-RS resource methods."""
    result = {}
    for name, start, end in java_methods(lines):
        header = lines[start:end + 1]
        signature = next((k for k, l in enumerate(header) if METHOD_SIG_RE.match(l)), len(header))
        if any(JAX_RS_VERB_RE.match(l) for l in header[:signature]):
            result[name] = (start, end)
    return result


def has_override(lines, start, end):
    return any(l.strip().startswith("@Override") for l in lines[start:end + 1] if not METHOD_SIG_RE.match(l))
