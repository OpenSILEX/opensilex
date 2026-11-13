"""What a merge request description must mention, read from git: file kinds, signals and ports of released changes.

Used by mr_facts.py. Signals are the facts a reviewer must hear about: endpoints and HTTP verbs, DTO fields, SPARQL
mapping, migrations, credentials, configuration, ontology, dependencies, tests, generated files, front changes.
"""
import os
import re
import subprocess
from collections import Counter, defaultdict

FIELD_RE = re.compile(r"^\s*(private|protected|public)\s+(?!static)[\w<>\[\], ?]+\s+(\w+)\s*(=.*)?;")
VERB_RE = re.compile(r"^\s*@(GET|POST|PUT|DELETE|PATCH)\b")
KIND_SIGNALS = {"doc": "docs_changed", "changelog": "changelog_md_edited", "config": "config_files_changed",
                "ontology": "ontology_changed", "vue": "front_touched", "i18n": "front_touched",
                "front-ts": "front_touched"}


def git(*args):
    """stdout of a git command, or None when it fails."""
    result = subprocess.run(["git", *args], capture_output=True, text=True, errors="replace")
    return result.stdout if result.returncode == 0 else None


def classify(path):
    name = os.path.basename(path)
    if path.startswith(".claude/"):
        return "tooling"
    if path.endswith(".java"):
        return "java-test" if "/src/test/" in path else "java-main"
    if path.endswith(".vue"):
        return "vue"
    if path.endswith((".ts", ".js")) and "/front/" in path:
        return "front-ts"
    if re.search(r"/lang/message-\w+\.json$", path):
        return "i18n"
    if path == "CHANGELOG.md":
        return "changelog"
    if path.endswith(".md") or path.startswith("opensilex-doc/"):
        return "doc"
    if name in ("pom.xml", "package.json", "yarn.lock"):
        return "build"
    if path.startswith(".gitlab") or name == ".gitlab-ci.yml":
        return "ci"
    if path.endswith((".owl", ".ttl", ".rdf", ".nt")):
        return "ontology"
    if path.endswith((".yml", ".yaml", ".properties")):
        return "config"
    return "other"


def module_of(path):
    if path.startswith(".claude/skills/"):
        return "skills/" + path.split("/")[2]
    first = path.split("/", 1)[0]
    return first if first.startswith(("opensilex-", "inrae-")) else "(root)"


def concept_of(path):
    match = re.search(r"/org/opensilex/\w+/(\w+)/", path) or re.search(r"/front/src/components/(\w+)/", path)
    return match.group(1) if match else None


def diff_lines(base, head, path):
    """(added, removed) line texts of one file."""
    out = git("diff", "-U0", "--no-color", base, head, "--", path) or ""
    added = [l[1:] for l in out.splitlines() if l.startswith("+") and not l.startswith("+++ ")]
    removed = [l[1:] for l in out.splitlines() if l.startswith("-") and not l.startswith("--- ")]
    return added, removed


def api_signals(name, added, removed, signals):
    for lines, bucket in ((added, "endpoints_paths_added"), (removed, "endpoints_paths_removed")):
        for line in lines:
            match = re.search(r'@Path\("([^"]*)"\)', line)
            if match:
                signals[bucket].append(f"{name} {match.group(1)}")
    verbs_added = Counter(m.group(1) for m in map(VERB_RE.match, added) if m)
    verbs_removed = Counter(m.group(1) for m in map(VERB_RE.match, removed) if m)
    if verbs_added or verbs_removed:
        change = [f"+{v} x{n}" for v, n in sorted(verbs_added.items())] + \
                 [f"-{v} x{n}" for v, n in sorted(verbs_removed.items())]
        signals["endpoints_http_verbs"].append(f"{name}: {', '.join(change)}")
    for line in added:
        match = re.search(r"@ApiCredential\(\s*credentialId\s*=\s*([\w.]+)", line)
        if match:
            signals["credentials_added"].append(f"{name} {match.group(1)}")


def dto_signals(name, added, removed, signals):
    for lines, bucket in ((added, "dto_fields_added"), (removed, "dto_fields_removed")):
        for line in lines:
            field = FIELD_RE.match(line)
            if field:
                signals[bucket].append(f"{name}.{field.group(2)}")
            json_name = re.search(r'@JsonProperty\("([^"]+)"\)', line)
            if json_name:
                signals[bucket.replace("fields", "json_names")].append(f"{name} {json_name.group(1)}")


def java_signals(row, added, removed, signals):
    name, status = os.path.basename(row["file"])[:-5], row["status"]
    if row["kind"] == "java-test":
        for annotation, bucket in (("@Test", "tests_added"), ("@Ignore", "tests_ignored")):
            count = sum(1 for l in added if l.strip().startswith(annotation))
            if count:
                signals[bucket].append(f"{name} (+{count} {annotation})")
        return
    if name.endswith("API"):
        api_signals(name, added, removed, signals)
    if name.endswith("DTO"):
        dto_signals(name, added, removed, signals)
    if name.endswith("Model") and any("@SPARQLProperty" in l or "@SPARQLResource" in l for l in added + removed):
        signals["sparql_mapping_changed"].append(name)
    if name.endswith("Config") and any("@ConfigDescription" in l for l in added):
        signals["config_options_added"].append(name)
    if status.startswith("A") and any("implements OpenSilexModuleUpdate" in l for l in added):
        signals["migrations_added"].append(row["file"])
    if status.startswith("D"):
        signals["java_classes_deleted"].append(row["file"])


def signals_for(base, head, files):
    """{signal: sorted values} for the changed files."""
    signals = defaultdict(list)
    for row in files:
        path, kind = row["file"], row["kind"]
        if "/front/src/lib/" in "/" + path or "/front/types/" in "/" + path:
            signals["generated_files_touched"].append(path)
        if kind in KIND_SIGNALS:
            signals[KIND_SIGNALS[kind]].append(path)
        if kind in ("java-main", "java-test"):
            java_signals(row, *diff_lines(base, head, path), signals)
        elif kind == "build" and not path.endswith("yarn.lock"):
            for line in diff_lines(base, head, path)[0]:
                match = re.search(r"<([\w.-]*version)>([^<]+)</", line) or \
                    re.search(r'^\s*"([@\w/.-]+)"\s*:\s*"([~^]?\d[^"]*)"', line)
                if match:
                    signals["dependencies_changed"].append(f"{path}: {match.group(1)} -> {match.group(2)}")
    return {key: sorted(set(values)) for key, values in signals.items()}


def ports_already_released(commits, remote_target):
    """Commits that cherry-pick or re-apply an MR whose `(!NNNN)` entry is already in a CHANGELOG.md.

    A port of a released hotfix must not get a second changelog entry (the 1.5.x hotfixes were ported this way)."""
    changelogs = {}
    for ref in dict.fromkeys([remote_target, "origin/master", "origin/develop", "HEAD"]):
        content = git("show", f"{ref}:CHANGELOG.md")
        if content:
            changelogs[ref] = content
    found = []
    for commit in commits:
        text = f"{commit['subject']}\n{commit['body']}"
        picked = re.findall(r"cherry picked from commit ([0-9a-f]{7,40})", text)
        in_target = git("merge-base", "--is-ancestor", commit["sha"], remote_target) is not None
        if not picked and in_target:
            continue  # the original commit of a merged MR mentions its own number: not a port
        for number in sorted(set(re.findall(r"!(\d{2,6})\b", text))):
            ref = next((r for r, content in changelogs.items() if f"(!{number})" in content), None)
            if ref:
                before = changelogs[ref][:changelogs[ref].find(f"(!{number})")]
                versions = re.findall(r"^## \[([^\]]+)\]", before, re.MULTILINE)
                found.append({"commit": commit["sha"], "mr": f"!{number}", "changelog_ref": ref,
                              "version": versions[-1] if versions else "?", "cherry_pick_of": picked})
                break
        if picked and not any(f["commit"] == commit["sha"] for f in found):
            found.append({"commit": commit["sha"], "mr": None, "changelog_ref": None, "version": None,
                          "cherry_pick_of": picked})
    return found
