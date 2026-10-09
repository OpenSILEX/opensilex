#!/usr/bin/env python3
"""What a review of an OpenSILEX change needs before anyone reads the code.

Prints the scope split into FUNCTIONAL files (read them) and MECHANICAL files (only arguments, imports or whitespace
change: skim them), the existing files to compare with, a test map (changed endpoints and new public methods, and the
tests that reach them), and LEADS: hypotheses to confirm by reading the code, never findings as such.

Run from the repository root (Python 3.8+, standard library; PyYAML used when present):
  review_scope.py                                 # branch + working tree vs merge-base with origin/develop
  review_scope.py --base origin/develop --head HEAD    # committed changes only
  review_scope.py --base <sha>~1 --head <sha>     # a past commit: every file is read at <sha>
  review_scope.py --paths opensilex-core/src/main/java/org/opensilex/core/site
  review_scope.py --leads-only | --json
"""
import argparse
import json
import os
import re
from collections import Counter

from scope_code import METHOD_SIG_RE, endpoints, has_override, java_methods
from scope_git import (TEST_PATHSPEC, Snapshot, change_profile, git, line_ranges, load_changes, read_old,
                       resolve_base, resolve_commit)
from scope_leads import (cascade_lead, duplicate_leads, front_leads, i18n_leads, java_leads, lang_file_leads, lead,
                         whitespace_churn_leads)

JAVA_SUFFIXES = ("CreationDTO", "UpdateDTO", "GetDTO", "SearchFilter", "Model", "DAO", "Logic", "API", "DTO",
                 "Config", "Module", "Exception", "Utils", "Service", "Test")
CODE_KINDS = ("java-main", "java-test", "vue", "ts")
MAX_PER_LEAD = 8


def classify(path, lines):
    """(kind, layer) of a changed file."""
    if path.endswith(".java"):
        if "/src/test/" in path:
            return "java-test", "test"
        layer = next((l for l in ("api", "bll", "dal") if f"/{l}/" in path), "other")
        if "/migration/" in path.lower() or any("implements OpenSilexModuleUpdate" in l for l in lines):
            layer = "migration"
        return "java-main", layer
    if path.endswith(".vue"):
        return "vue", "front"
    if path.endswith((".ts", ".js")) and "/front/" in path:
        return "ts", "front"
    if re.search(r"/lang/message-\w+\.json$", path):
        return "i18n", "front"
    if path.endswith(".md") or path.startswith("opensilex-doc/"):
        return "doc", "doc"
    if os.path.basename(path) in ("pom.xml", "package.json", "yarn.lock"):
        return "build", "build"
    if path.startswith(".gitlab") or path.endswith(".gitlab-ci.yml"):
        return "ci", "ci"
    if path.endswith((".yml", ".yaml", ".properties")):
        return "config", "config"
    return "other", "other"


def module_of(path):
    first = path.split("/", 1)[0]
    return first if first.startswith(("opensilex-", "inrae-")) else "(root)"


def suffix_of(name):
    base = name[:-5] if name.endswith(".java") else name
    return next(((base[:-len(s)], s) for s in JAVA_SUFFIXES if base.endswith(s) and len(base) > len(s)), (base, ""))


def package_of(path):
    match = re.search(r"/src/(?:main|test)/java/(.+)/[^/]+$", path)
    return match.group(1).replace("/", ".") if match else ""


def siblings_of(path, entry, snapshot, changed):
    """Existing files that show how the team writes this kind of file."""
    directory, name = os.path.dirname(path), os.path.basename(path)
    files = snapshot.files()
    item = {"file": path}
    if path.endswith(".java"):
        stem, suffix = suffix_of(name)
        concept_dir = re.sub(r"/(api|bll|dal)(/.*)?$", "", directory)
        dto_ok = suffix.endswith("DTO")
        item["same_concept"] = sorted(p for p in files if p.startswith(concept_dir + "/") and p != path
                                      and p.endswith(".java") and os.path.basename(p).startswith(stem)
                                      and (dto_ok or not p.endswith("DTO.java")))[:5]
        if suffix:
            in_test = "/src/test/" in path
            peers = [p for p in files if p.startswith(entry["module"] + "/") and p.endswith(suffix + ".java")
                     and p not in changed and ("/src/test/" in p) == in_test and not p.startswith(concept_dir + "/")]
            peers.sort(key=lambda p: -len(os.path.commonprefix([p, path])))
            item["same_role"] = peers[:3]
    else:
        item["same_directory"] = sorted(os.path.basename(p) for p in files if os.path.dirname(p) == directory
                                        and p != path and p.endswith(".vue"))[:5]
    return item


def tests_referencing(snapshot, class_name, package, cache):
    """Test files that use the class (same package, import, or fully qualified name)."""
    key = (class_name, package)
    if key not in cache:
        kept = []
        for test in snapshot.grep(class_name, [TEST_PATHSPEC], word=True)[:80]:
            content = "\n".join(snapshot.read(test))
            if package_of(test) == package or re.search(rf"\b{re.escape(package)}\.({class_name}|\*)\b", content):
                kept.append(test)
        cache[key] = kept
    return cache[key]


def changed_public_methods(path, entry, functional, snapshot, users, leads):
    """Public methods whose signature is new, that have >= 3 functional changed lines, or that call a private method
    with >= 5; and the tests that call them directly (`.name(`) or through an endpoint (`getMethod("name"`)."""
    lines, class_name, found = entry["lines"], os.path.basename(path)[:-5], []
    methods = []
    for name, start, end in java_methods(lines):
        signature = next(n for n in range(start, end + 1) if METHOD_SIG_RE.match(lines[n]))
        methods.append((name, start, end, signature, sum(1 for n in functional if start < n <= end + 1)))
    changed_private = [m for m in methods if not lines[m[3]].strip().startswith("public") and m[4] >= 5]
    for name, start, end, signature, touched in methods:
        is_accessor = re.match(r"(get|set|is)[A-Z]", name) and end - signature <= 5
        body = "\n".join(lines[signature + 1:end + 1])
        via = [p[0] for p in changed_private if re.search(rf"\b{p[0]}\(", body)]
        if (not lines[signature].strip().startswith("public") or is_accessor or has_override(lines, start, end)
                or (signature + 1 not in functional and touched < 3 and not via)):
            continue
        tested = [t for t in snapshot.grep(rf"\.{name}\(", [TEST_PATHSPEC]) if t in users]
        tested += snapshot.grep(f'getMethod("{name}"', [TEST_PATHSPEC], fixed=True)
        status = "new signature" if signature + 1 in functional else f"{touched} lines changed"
        if via:
            status += f", calls changed {', '.join(v + '()' for v in via)}"
        found.append({"file": path, "method": name, "line": signature + 1, "change": status,
                      "tests": sorted(set(tested))})
        if not tested:
            lead(leads, "TEST-METHOD-UNTESTED", path, signature + 1,
                 f"public {class_name}.{name}() ({status}): no test calls it, directly or through an endpoint")
    return found


def test_map(files, snapshot, base, leads):
    cache, result = {}, {"changed_test_files": [], "endpoints": [], "classes": [], "methods": []}
    for path, entry in files.items():
        if entry["kind"] == "java-test":
            added = [t.strip() for _, t in entry["added"]]
            result["changed_test_files"].append({
                "file": path, "tests_added": sum(t.startswith("@Test") for t in added),
                "tests_removed": sum(t.strip().startswith("@Test") for _, t in entry["removed"]),
                "ignore_added": sum(t.startswith("@Ignore") for t in added)})
    for path, entry in files.items():
        if entry["kind"] != "java-main" or entry["status"] == "D" or entry["mechanical"]:
            continue
        lines, class_name, package = entry["lines"], os.path.basename(path)[:-5], package_of(path)
        users = tests_referencing(snapshot, class_name, package, cache)
        result["classes"].append({"file": path, "class": class_name, "tests": users,
                                  "test_changed": any(t in files for t in users)})
        functional = {n for n, t in entry["added"] if n not in entry["profile"]["trivial"] and t.strip()}
        if class_name.endswith("API"):
            current = endpoints(lines)
            if not current and any("@Path(" in l for l in lines):
                lead(leads, "SCRIPT-NO-ENDPOINT", path, 0, "no endpoint detected in this API: read it by hand")
            for method, (start, end) in current.items():
                touched = sum(1 for n in functional if start < n <= end + 1)
                if touched:
                    callers = [t for t in snapshot.grep(f'getMethod("{method}"', [TEST_PATHSPEC], fixed=True)
                               if t in users]
                    result["endpoints"].append({"api": class_name, "file": path, "method": method,
                                                "line": start + 1, "changed_lines": touched, "tests": callers})
                    if not callers and touched >= 3:
                        lead(leads, "TEST-ENDPOINT-UNTESTED", path, start + 1,
                             f"{method}() ({touched} lines changed): no test calls getMethod(\"{method}\")")
            if entry["status"] != "A":
                for method in sorted(set(endpoints(read_old(entry["old_path"], base))) - set(current)):
                    result["endpoints"].append({"api": class_name, "file": path, "method": method, "removed": True})
        elif not re.search(r"(DTO|Model|SearchFilter|Exception|Config)$", class_name):
            result["methods"].extend(changed_public_methods(path, entry, functional, snapshot, users, leads))
        if not users and (re.search(r"(API|Logic|DAO|Service|Utils|Helper|Exporter|Importer|Parser)$", class_name)
                          or entry["layer"] == "migration"):
            lead(leads, "TEST-CLASS-UNREFERENCED", path, 0, f"no test uses {package}.{class_name}")
    return result


def collect(args):
    base, base_label = resolve_base(args.base)
    head = resolve_commit(args.head) if args.head else None
    snapshot = Snapshot(head)
    files, not_analysed, generated, skipped = load_changes(base, head, snapshot, args.paths)
    for path, entry in files.items():
        entry["lines"] = snapshot.read(path) if entry["status"] != "D" else []
        entry["kind"], entry["layer"] = classify(path, entry["lines"])
        entry["module"] = module_of(path)
        entry["profile"] = change_profile(entry)
        entry["mechanical"] = entry["kind"] in CODE_KINDS and entry["profile"]["functional"] <= 2
    leads = []
    for path, entry in files.items():
        if entry["mechanical"] or entry["status"] == "D":
            continue
        if entry["kind"] in ("java-main", "java-test"):
            java_leads(path, entry, entry["lines"], leads)
        elif entry["kind"] in ("vue", "ts"):
            front_leads(path, entry, entry["lines"], leads)
            if path.endswith(".vue"):
                i18n_leads(path, entry["lines"], base, leads)
    lang_file_leads([p for p in files if files[p]["kind"] == "i18n"], snapshot, base, leads)
    duplicate_leads({p: e for p, e in files.items() if e["kind"] in CODE_KINDS and not e["mechanical"]}, leads)
    whitespace_churn_leads(base, head, [p for p, e in files.items() if e["status"] == "M" and not e.get("untracked")],
                           leads)
    mechanical = [p for p, e in files.items() if e["mechanical"]]
    cascade_lead(mechanical, files, leads)
    for path in generated:
        lead(leads, "MAINT-GENERATED-EDIT", path, 0, "generated file edited: regenerate it instead")
    tests = test_map(files, snapshot, base, leads)

    functional = {p: e for p, e in files.items() if not e["mechanical"]}
    functional_lines = sum(e["profile"]["functional"] for e in functional.values())
    functional_modules = sorted({e["module"] for e in functional.values() if e["kind"] in CODE_KINDS})
    large = functional_lines > 1200 or (len(functional_modules) >= 3 and functional_lines > 400)
    rows = [{"file": p, "status": e["status"] + ("?" if e.get("untracked") else ""), "added": len(e["added"]),
             "removed": len(e["removed"]), "functional": e["profile"]["functional"], "kind": e["kind"],
             "layer": e["layer"], "module": e["module"], "mechanical": e["mechanical"]} for p, e in files.items()]
    return {
        "summary": {"base": base[:12], "base_label": base_label, "head": head[:12] if head else "working tree",
                    "files": len(rows), "lines_added": sum(r["added"] for r in rows),
                    "lines_removed": sum(r["removed"] for r in rows), "functional_lines": functional_lines,
                    "functional_files": len(functional), "mechanical_files": len(mechanical),
                    "functional_modules": functional_modules,
                    "size": "large: split by axis or module" if large else
                            "medium" if functional_lines > 300 else "small"},
        "files": rows,
        "changed_ranges": {p: line_ranges(n for n, _ in e["added"]) for p, e in functional.items()},
        "siblings": [siblings_of(p, e, snapshot, files) for p, e in functional.items()
                     if e["kind"] in ("java-main", "java-test", "vue") and e["status"] != "D"],
        "tests": tests, "leads": leads,
        "not_analysed": not_analysed, "generated": generated, "skipped_untracked_dirs": skipped,
    }


def print_markdown(r, leads_only):
    s = r["summary"]
    print("# Review scope\n")
    print(f"- Base `{s['base']}` ({s['base_label']}), head: {s['head']}")
    print(f"- {s['files']} files, +{s['lines_added']} -{s['lines_removed']}; functional: {s['functional_lines']} "
          f"lines in {s['functional_files']} files; mechanical (arguments/imports/whitespace only): "
          f"{s['mechanical_files']} files")
    print(f"- Size: {s['size']}; modules with functional code changes: {', '.join(s['functional_modules']) or '-'}")
    if not leads_only:
        print("\n## Functional files (read them)\n\n| file | st | + | - | functional | kind | layer |\n|---|---|---|---|---|---|---|")
        for row in (x for x in r["files"] if not x["mechanical"]):
            print(f"| {row['file']} | {row['status']} | {row['added']} | {row['removed']} | {row['functional']} "
                  f"| {row['kind']} | {row['layer']} |")
        mechanical = [os.path.basename(x["file"]) for x in r["files"] if x["mechanical"]]
        if mechanical:
            more = f" ... +{len(mechanical) - 40}" if len(mechanical) > 40 else ""
            print(f"\n## Mechanical files (skim)\n\n{', '.join(mechanical[:40])}{more}")
        print("\n## Compare with (homogeneity)\n")
        for item in r["siblings"]:
            parts = [f"{label}: {', '.join(os.path.basename(p) for p in item[key])}"
                     for key, label in (("same_concept", "concept"), ("same_role", "same role"),
                                        ("same_directory", "directory")) if item.get(key)]
            print(f"- `{os.path.basename(item['file'])}`: {'; '.join(parts) or 'none found: search by role'}")
        print_tests(r["tests"])
    print("\n## Leads (hypotheses: confirm in the code, drop false positives)\n")
    by_id = Counter(item["id"] for item in r["leads"])
    shown = Counter()
    for item in sorted(r["leads"], key=lambda x: (x["id"].split("-")[0], x["id"], x["file"], x["line"])):
        shown[item["id"]] += 1
        if shown[item["id"]] > MAX_PER_LEAD:
            if shown[item["id"]] == MAX_PER_LEAD + 1:
                print(f"- {item['id']}: ... {by_id[item['id']] - MAX_PER_LEAD} more (--json)")
            continue
        where = f"{item['file']}:{item['line']}" if item["line"] else item["file"]
        print(f"- {item['id']} `{where}` {item['detail']}")
    if not r["leads"]:
        print("- none")
    skipped = ([f"{p} (binary or lock)" for p in r["not_analysed"]] + [f"{p} (generated)" for p in r["generated"]]
               + [f"{p} (nested repository or large untracked tree: --paths to include)"
                  for p in r["skipped_untracked_dirs"]])
    if skipped:
        print("\n## Not analysed\n\n" + "\n".join(f"- {line}" for line in skipped))


def print_tests(t):
    print("\n## Tests\n")
    if not t["changed_test_files"]:
        print("- no test file changed")
    for item in t["changed_test_files"]:
        ignored = f", +{item['ignore_added']} @Ignore" if item["ignore_added"] else ""
        print(f"- changed `{os.path.basename(item['file'])}`: +{item['tests_added']} / -{item['tests_removed']} "
              f"@Test{ignored}")
    for item in t["endpoints"]:
        if item.get("removed"):
            print(f"- endpoint removed {item['api']}#{item['method']}: front client and callers?")
        else:
            tests = ", ".join(os.path.basename(p) for p in item["tests"]) or "NOT called by any test"
            print(f"- endpoint {item['api']}#{item['method']} ({item['changed_lines']} lines): {tests}")
    for item in t["methods"]:
        tests = ", ".join(os.path.basename(p) for p in item["tests"]) or "no test calls it"
        print(f"- public method {os.path.basename(item['file'])[:-5]}#{item['method']} ({item['change']}): {tests}")
    for item in t["classes"]:
        users = ", ".join(os.path.basename(p) for p in item["tests"][:4]) or "none"
        note = " (none changed)" if item["tests"] and not item["test_changed"] else ""
        print(f"- class {item['class']}: used by {users}{note}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", help="base ref (default: merge-base of HEAD with origin/develop)")
    parser.add_argument("--head", help="head commit (default: working tree, untracked files included)")
    parser.add_argument("--paths", nargs="*", default=[], help="restrict to these paths")
    parser.add_argument("--json", action="store_true", help="full JSON output")
    parser.add_argument("--leads-only", action="store_true", help="summary and leads only")
    args = parser.parse_args()
    os.chdir(git("rev-parse", "--show-toplevel").strip())
    result = collect(args)
    if args.json:
        print(json.dumps(result, indent=1))
    else:
        print_markdown(result, args.leads_only)


if __name__ == "__main__":
    main()
