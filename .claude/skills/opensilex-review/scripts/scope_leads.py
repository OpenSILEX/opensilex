"""Lead detectors: places a reviewer should read. A lead is a hypothesis, never a finding.

Every detector looks at FUNCTIONAL added lines only (see scope_git.change_profile): a line whose only change is an
argument, an import or whitespace does not raise a lead.
"""
import hashlib
import json
import re
from collections import Counter, defaultdict

from scope_code import TS_METHOD_RE, brace_depths, inside_loop, java_methods, parent_chain
from scope_git import git, read_old

try:
    import yaml  # optional: <i18n> blocks are YAML
except ImportError:  # pragma: no cover
    yaml = None

# Calls on an instance whose name says it reaches a store (sparql, dao, mongo...). Static helpers
# (SPARQLDeserializers.getShortURI) start with an upper case and are ignored.
STORE_CALL_RE = re.compile(r"\b([a-z]\w*)\s*\.\s*(\w+)\s*\((\s*\))?")
STORE_RECEIVER_RE = re.compile(r"dao|sparql|mongo|nosql|collection|service|repository|client", re.IGNORECASE)
STORE_METHOD_RE = re.compile(r"^(get\w*|load\w*|search\w*|count\w*|exist\w*|find\w*|execute\w*|aggregate|"
                             r"distinct|create\w*|update\w*|delete\w*|insert\w*|query\w*)$")
FRONT_CALL_RE = re.compile(r"\bawait\b|\.then\(|\w+Service\.\w+\(|\$opensilex\.\w+\(")


def lead(leads, lead_id, path, line, detail):
    leads.append({"id": lead_id, "file": path, "line": line, "detail": detail.strip()[:160]})


def functional_lines(entry):
    return [(n, t) for n, t in entry["added"] if n not in entry["profile"]["trivial"] and t.strip()]


def store_call_in_loop(lines, index, text):
    for match in STORE_CALL_RE.finditer(text):
        receiver, method, empty_args = match.groups()
        if not STORE_RECEIVER_RE.search(receiver) or not STORE_METHOD_RE.match(method):
            continue
        if empty_args and method.startswith("get"):
            continue  # a getter, not a query
        loop = inside_loop(lines, index)
        if loop:
            return f"{receiver}.{method}(...) inside `{loop.strip()[:60]}`"
    return None


def java_leads(path, entry, lines, leads):
    is_test = "/src/test/" in path
    depths, methods = brace_depths(lines), java_methods(lines)
    nesting_reported = set()
    for number, text in functional_lines(entry):
        stripped, index = text.strip(), number - 1
        if not 0 <= index < len(lines):
            continue
        if not is_test:
            call = store_call_in_loop(lines, index, text)
            if call:
                lead(leads, "PERF-QUERY-IN-LOOP", path, number, call)
            if re.search(r'"https?://www\.opensilex\.org/vocabulary/oeso#', text):
                lead(leads, "HOMO-URI-LITERAL", path, number, "ontology URI literal: use the Oeso constants")
            if re.search(r"\bnew Date\(|\bSimpleDateFormat\b", text):
                lead(leads, "HOMO-LEGACY-DATE", path, number, stripped)
        if re.search(r"catch\s*\([^)]*\)\s*\{\s*\}", text):
            lead(leads, "MAINT-EMPTY-CATCH", path, number, stripped)
        if "printStackTrace()" in text or re.search(r"System\.(out|err)\.print", text):
            lead(leads, "MAINT-STDOUT", path, number, stripped)
        if "Thread.sleep(" in text:
            lead(leads, "TEST-SLEEP" if is_test else "PERF-SLEEP", path, number, stripped)
        if re.search(r"//.*\b(TODO|FIXME|XXX)\b", text):
            lead(leads, "CLAR-TODO", path, number, stripped)
        if re.match(r"^\s*//\s*[\w.$\[\]()<>]+.*[;{]\s*$", text) and not re.match(r"^\s*//\s*[A-Z][a-z]+ ", text):
            lead(leads, "MAINT-COMMENTED-CODE", path, number, stripped)
        if "@SuppressWarnings" in text:
            lead(leads, "MAINT-SUPPRESS", path, number, stripped)
        # class = 1 and method body = 2: depth 6 means four nested blocks or lambdas inside a method
        if depths[index] >= 6 and not stripped.startswith(("}", "*", "//")):
            method = next((m for m in methods if m[1] <= index <= m[2]), None)
            key = method[0] if method else index // 40
            if key not in nesting_reported:
                nesting_reported.add(key)
                lead(leads, "CLAR-DEEP-NESTING", path, number,
                     f"{depths[index] - 2} nested blocks in {method[0] + '()' if method else 'this method'}")
    functional = {n for n, _ in functional_lines(entry)}
    for name, start, end in methods:
        length, touched = end - start + 1, sum(1 for n in functional if start < n <= end + 1)
        if length > 80 and touched >= 3:
            lead(leads, "MAINT-LONG-METHOD", path, start + 1, f"{name}() is {length} lines, {touched} changed here")


def prop_declaration(lines, index):
    """The field declared by the @Prop(...) decorator on lines[index], even when the decorator spans lines."""
    depth, text, j = 0, lines[index][lines[index].index("@Prop(") + len("@Prop"):], index
    while j < len(lines):
        for position, char in enumerate(text):
            depth += {"(": 1, ")": -1}.get(char, 0)
            if depth == 0:
                rest = text[position + 1:].strip()
                return rest or next((l for l in lines[j + 1:j + 4] if l.strip()), None)
        j += 1
        text = lines[j] if j < len(lines) else ""
    return None


def front_leads(path, entry, lines, leads):
    template_end = next((i for i, l in enumerate(lines) if l.startswith("</template>")), -1)
    for number, text in functional_lines(entry):
        stripped, index = text.strip(), number - 1
        if not 0 <= index < len(lines):
            continue
        in_template = path.endswith(".vue") and index <= template_end
        if not in_template and re.search(r":\s*any\b|\bas any\b|<any>", text):
            lead(leads, "HOMO-TS-ANY", path, number, stripped)
        if "console.log(" in text:
            lead(leads, "CLAR-CONSOLE-LOG", path, number, stripped)
        if not in_template and "$emit(" in text:
            enclosing = next((t for _, t in parent_chain(lines, index) if TS_METHOD_RE.match(t)), "")
            if not re.search(r"\bemit[A-Z]\w*\s*\(", enclosing):
                lead(leads, "HOMO-RAW-EMIT", path, number, "$emit outside an emitXxx() method (Vue guidelines)")
        if "@Prop(" in text:
            declaration = prop_declaration(lines, index)
            if declaration and "readonly" not in declaration:
                lead(leads, "HOMO-PROP-NOT-READONLY", path, number, declaration)
        if in_template and "v-for=" in text and ":key" not in " ".join(lines[index:index + 3]):
            lead(leads, "PERF-VFOR-NO-KEY", path, number, stripped)
        if re.search(r"deep\s*:\s*true", text):
            lead(leads, "PERF-DEEP-WATCH", path, number, stripped)
        if "setTimeout(" in text:
            lead(leads, "MAINT-SETTIMEOUT", path, number, stripped)
        if re.search(r"//.*\b(TODO|FIXME|XXX)\b", text):
            lead(leads, "CLAR-TODO", path, number, stripped)
        loop = None if in_template or not FRONT_CALL_RE.search(text) else inside_loop(lines, index)
        if loop:
            lead(leads, "PERF-CALL-IN-LOOP", path, number, f"{stripped[:70]} inside `{loop.strip()[:50]}`")


def flatten_keys(node, prefix=""):
    keys = set()
    for key, value in (node.items() if isinstance(node, dict) else []):
        full = f"{prefix}.{key}" if prefix else str(key)
        keys |= flatten_keys(value, full) if isinstance(value, dict) else {full}
    return keys


def i18n_block(lines):
    """The <i18n> block of a component as a dict, {"__invalid__": True} when it does not parse, else None."""
    start = next((i for i, l in enumerate(lines) if l.strip().startswith("<i18n")), None)
    if start is None:
        return None
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip().startswith("</i18n>")), None)
    if end is None:
        return None
    text = "\n".join(lines[start + 1:end])
    try:
        return json.loads(text)
    except ValueError:
        if yaml is None:
            return None
    try:
        data = yaml.safe_load(text)
    except yaml.YAMLError:
        return {"__invalid__": True}
    return data if isinstance(data, dict) else None


def i18n_leads(path, lines, base, leads):
    block = i18n_block(lines)
    if not block:
        return
    if block.get("__invalid__"):
        lead(leads, "HOMO-I18N-INVALID", path, 0, "<i18n> block does not parse")
        return
    en, fr = flatten_keys(block.get("en", {})), flatten_keys(block.get("fr", {}))
    old = i18n_block(read_old(path, base)) or {}
    old_keys = flatten_keys(old.get("en", {})) | flatten_keys(old.get("fr", {}))
    for key in sorted((en ^ fr) - old_keys):
        lead(leads, "HOMO-I18N-PARITY", path, 0, f"key '{key}' has no '{'fr' if key in en else 'en'}' translation")


def lang_file_leads(paths, snapshot, base, leads):
    """Keys added to lang/message-en.json without message-fr.json, and the reverse."""
    for path in paths:
        match = re.search(r"/lang/message-(en|fr)\.json$", path)
        if not match:
            continue
        lang = match.group(1)
        other = "fr" if lang == "en" else "en"
        other_path = path.replace(f"message-{lang}.json", f"message-{other}.json")
        try:
            new = flatten_keys(json.loads("\n".join(snapshot.read(path)) or "{}"))
            old = flatten_keys(json.loads("\n".join(read_old(path, base)) or "{}"))
            others = flatten_keys(json.loads("\n".join(snapshot.read(other_path)) or "{}"))
        except ValueError:
            lead(leads, "HOMO-I18N-INVALID", path, 0, "message file does not parse as JSON")
            continue
        for key in sorted((new - old) - others):
            lead(leads, "HOMO-I18N-PARITY", other_path, 0, f"key '{key}' added in {lang}, missing in {other}")


def duplicate_leads(entries, leads, window=6):
    """Blocks of `window` identical functional lines added in two places of the change."""
    seen = defaultdict(list)
    for path, entry in entries.items():
        block_lines = [(n, re.sub(r"\s+", " ", t.strip())) for n, t in functional_lines(entry)]
        for i in range(len(block_lines) - window + 1):
            block = block_lines[i:i + window]
            texts = [t for _, t in block]
            if block[-1][0] - block[0][0] != window - 1 or sum(len(t) > 3 for t in texts) < window - 1:
                continue
            seen[hashlib.sha1("\n".join(texts).encode()).hexdigest()].append((path, block[0][0]))
    pairs = defaultdict(list)
    for places in seen.values():
        first, *others = sorted(set(places))
        for other in others:
            if other[0] != first[0] or abs(other[1] - first[1]) >= window:
                pairs[(first[0], other[0])].append((first[1], other[1]))
    for (p1, p2), positions in pairs.items():
        positions.sort()
        start = previous = positions[0]
        for position in positions[1:] + [None]:
            if position and position == (previous[0] + 1, previous[1] + 1):
                previous = position
                continue
            size = previous[0] - start[0] + window
            lead(leads, "MAINT-DUPLICATED-BLOCK", p1, start[0], f"~{size} lines also added at {p2}:{start[1]}")
            start = previous = position


def whitespace_churn_leads(base, head, paths, leads):
    """Files where many changed lines differ only by whitespace: reformatting mixed with the change."""
    if not paths:
        return
    rng = [base, head] if head else [base]

    def changed_lines(*extra):
        counts = {}
        for row in git("diff", "--numstat", *extra, *rng, "--", *paths).splitlines():
            added, removed, path = row.split("\t", 2)
            if added != "-":
                counts[path] = int(added) + int(removed)
        return counts

    full, ignoring_ws = changed_lines(), changed_lines("-w")
    for path, total in full.items():
        churn = total - ignoring_ws.get(path, 0)
        if churn >= 20 and churn >= 0.4 * total:
            lead(leads, "CLAR-REFORMAT-MIXED", path, 0,
                 f"{churn} of {total} changed lines differ only by whitespace (CONTRIBUTING: do not mix)")


def cascade_lead(mechanical, entries, leads):
    """Many files that only change call arguments: a signature change rippling through callers."""
    if len(mechanical) < 4:
        return
    calls = Counter()
    for path in mechanical:
        for number, text in entries[path]["added"]:
            if number in entries[path]["profile"]["trivial"]:
                calls.update(re.findall(r"\bnew (\w+)\(", text))
    top = ", ".join(f"new {name}( x{count}" for name, count in calls.most_common(3)) or "calls"
    lead(leads, "MAINT-SIGNATURE-CASCADE", f"{len(mechanical)} files", 0,
         f"only arguments/imports change ({top}): find the signature that caused it; is the dependency needed "
         "by every caller?")
