#!/usr/bin/env python3
"""Gather the facts an OpenSILEX merge request description is written from.

Prints (Markdown, or JSON with --json): branch and target, the GitLab CI rules the MR will be checked against,
push/sync state, commits, changed files grouped by module and kind, signals a reviewer must hear about (endpoints,
DTO fields, credentials, configuration, migrations, ontology, i18n, generated files, dependencies), a suggested
title type/scope, the template to use, where to save the draft and the URL that opens a new MR.

It only reads git. Run from the repository root:
  mr_facts.py                      # current branch, target inferred (feature/* -> develop, hotfix/* -> master)
  mr_facts.py --target release     # explicit target branch
  mr_facts.py --json
"""
import argparse
import json
import os
import re
import subprocess
import sys
from collections import Counter, defaultdict

CC_TYPES = ("build", "chore", "ci", "docs", "feat", "fix", "perf", "refactor", "revert", "style", "test")
NO_CHANGELOG_TYPES = ("build", "chore", "ci", "docs", "refactor", "style", "test")
EXCLUDED = ("target/", "node_modules/", ".kilo/", "graft/", "site/", ".idea/", ".node/", ".claude/")


def git(*args):
    result = subprocess.run(["git", *args], capture_output=True, text=True, errors="replace")
    return result.stdout if result.returncode == 0 else None


def must(*args):
    out = git(*args)
    if out is None:
        sys.exit(f"git {' '.join(args)} failed")
    return out


def infer_target(branch):
    if branch.startswith("hotfix/"):
        return "master"
    if branch.startswith("vue3/") and branch != "vue3/main":
        return "vue3/main"
    return "develop"


def branch_checks(branch, target):
    """The rules of the `merge-request:check` CI job, plus the naming convention of the dev-tools docs."""
    problems, notes = [], []
    if target == "master" and not branch.startswith("hotfix/"):
        problems.append("CI refuses it: only `hotfix/*` branches can target master")
    if target in ("develop", "release") and not branch.startswith("feature/"):
        problems.append(f"CI refuses it: only `feature/*` branches can target {target}")
    if branch == "release":
        notes.append("no MR pipeline runs for the release branch")
    if branch.startswith("feature/") and not re.match(r"^feature/[^/]+/(fix/)?[^/].*", branch):
        notes.append("naming convention is `feature/<dev>/<name>` or `feature/<dev>/fix/<name>`")
    if branch.startswith("hotfix/") and not re.match(r"^hotfix/[^/]+/[^/].*", branch):
        notes.append("naming convention is `hotfix/<dev>/<name>`")
    return problems, notes


def classify(path):
    name = os.path.basename(path)
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


FIELD_RE = re.compile(r"^\s*(private|protected|public)\s+(?!static)[\w<>\[\], ?]+\s+(\w+)\s*(=.*)?;")


def signals_for(base, head, files):
    signals = defaultdict(list)
    for row in files:
        path, kind, status = row["file"], row["kind"], row["status"]
        name = os.path.basename(path)
        if "/front/src/lib/" in "/" + path or "/front/types/" in "/" + path:
            signals["generated_files_touched"].append(path)
        if kind in ("vue", "i18n", "front-ts"):
            signals["front_touched"].append(path)
        if kind == "doc":
            signals["docs_changed"].append(path)
        if kind == "changelog":
            signals["changelog_md_edited"].append(path)
        if kind == "config":
            signals["config_files_changed"].append(path)
        if kind == "ontology":
            signals["ontology_changed"].append(path)
        if status.startswith("D") and kind == "java-main":
            signals["java_classes_deleted"].append(path)
        if kind not in ("java-main", "java-test", "build"):
            continue
        added, removed = diff_lines(base, head, path)
        if kind == "java-main":
            if name.endswith("API.java"):
                for lines, bucket in ((added, "endpoints_paths_added"), (removed, "endpoints_paths_removed")):
                    for line in lines:
                        match = re.search(r'@Path\("([^"]*)"\)', line)
                        if match:
                            signals[bucket].append(f"{name[:-5]} {match.group(1)}")
                verbs_added = Counter(m.group(1) for l in added for m in [re.match(r"^\s*@(GET|POST|PUT|DELETE|PATCH)\b", l)] if m)
                verbs_removed = Counter(m.group(1) for l in removed for m in [re.match(r"^\s*@(GET|POST|PUT|DELETE|PATCH)\b", l)] if m)
                if verbs_added or verbs_removed:
                    change = ", ".join([f"+{v} x{n}" for v, n in sorted(verbs_added.items())] +
                                       [f"-{v} x{n}" for v, n in sorted(verbs_removed.items())])
                    signals["endpoints_http_verbs"].append(f"{name[:-5]}: {change}")
                for line in added:
                    match = re.search(r"@ApiCredential\(\s*credentialId\s*=\s*([\w.]+)", line)
                    if match:
                        signals["credentials_added"].append(f"{name[:-5]} {match.group(1)}")
            if name.endswith("DTO.java"):
                for lines, bucket in ((added, "dto_fields_added"), (removed, "dto_fields_removed")):
                    for line in lines:
                        match = FIELD_RE.match(line)
                        if match:
                            signals[bucket].append(f"{name[:-5]}.{match.group(2)}")
                        match = re.search(r'@JsonProperty\("([^"]+)"\)', line)
                        if match:
                            signals[bucket.replace("fields", "json_names")].append(f"{name[:-5]} {match.group(1)}")
            if name.endswith("Model.java") and any("@SPARQLProperty" in l or "@SPARQLResource" in l
                                                   for l in added + removed):
                signals["sparql_mapping_changed"].append(name[:-5])
            if name.endswith("Config.java") and any("@ConfigDescription" in l for l in added):
                signals["config_options_added"].append(name[:-5])
            if status.startswith("A") and any("implements OpenSilexModuleUpdate" in l for l in added):
                signals["migrations_added"].append(path)
        elif kind == "java-test":
            count = sum(1 for l in added if l.strip().startswith("@Test"))
            if count:
                signals["tests_added"].append(f"{name[:-5]} (+{count} @Test)")
            ignored = sum(1 for l in added if l.strip().startswith("@Ignore"))
            if ignored:
                signals["tests_ignored"].append(f"{name[:-5]} (+{ignored} @Ignore)")
        elif kind == "build" and name != "yarn.lock":
            for line in added:
                match = re.search(r"<([\w.-]*version)>([^<]+)</", line) or \
                    re.search(r'^\s*"([@\w/.-]+)"\s*:\s*"([~^]?\d[^"]*)"', line)
                if match:
                    signals["dependencies_changed"].append(f"{path}: {match.group(1)} -> {match.group(2)}")
    return {k: sorted(set(v)) for k, v in signals.items()}


def suggest_changelog(suggested_type, commits, ported):
    released = {p["commit"] for p in ported if p["changelog_ref"]}
    if commits and all(c["sha"] in released for c in commits):
        return ("ignore-changelog: true - every commit ports a change already listed in a CHANGELOG.md "
                "(say so in Points d'attention)")
    if released:
        return ("changelog entry for the new work only - some commits port changes already in a CHANGELOG.md: "
                "do not list them again")
    if suggested_type in NO_CHANGELOG_TYPES:
        return "ignore-changelog: true"
    return "changelog entry (user point of view, English)"


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
        already_in_target = subprocess.run(["git", "merge-base", "--is-ancestor", commit["sha"], remote_target],
                                           capture_output=True).returncode == 0
        if not picked and already_in_target:
            continue  # the original commit of a merged MR mentions its own number: not a port
        for number in sorted(set(re.findall(r"!(\d{2,6})\b", text))):
            for ref, content in changelogs.items():
                index = content.find(f"(!{number})")
                if index < 0:
                    continue
                versions = re.findall(r"^## \[([^\]]+)\]", content[:index], re.MULTILINE)
                found.append({"commit": commit["sha"], "mr": f"!{number}", "changelog_ref": ref,
                              "version": versions[-1] if versions else "?", "cherry_pick_of": picked})
                break
        if picked and not any(f["commit"] == commit["sha"] for f in found):
            found.append({"commit": commit["sha"], "mr": None, "changelog_ref": None, "version": None,
                          "cherry_pick_of": picked})
    return found


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--target", help="target branch (default: inferred from the branch name)")
    parser.add_argument("--head", default="HEAD", help="head ref (default: HEAD, committed work only)")
    parser.add_argument("--base", help="base ref (default: merge-base of head with origin/<target>)")
    parser.add_argument("--source", help="source branch name, when --head is a commit (default: the branch of --head)")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    os.chdir(must("rev-parse", "--show-toplevel").strip())
    branch = (git("branch", "--show-current") or "").strip() or "(detached HEAD)"
    target = args.target or infer_target(branch)
    remote_target = f"origin/{target}" if git("rev-parse", "--verify", "--quiet", f"origin/{target}") else target
    base = (git("rev-parse", "--verify", "--quiet", args.base + "^{commit}") if args.base
            else git("merge-base", args.head, remote_target) or "").strip()
    if not base:
        sys.exit(f"No merge-base between {args.head} and {remote_target}: fetch it (git fetch origin {target})")
    head = must("rev-parse", args.head).strip()
    current = head == must("rev-parse", "HEAD").strip()
    is_branch = current and branch != "(detached HEAD)"
    if not current:  # another ref: describe it, not the checked-out branch
        branch = re.sub(r"^(refs/heads/|refs/remotes/origin/|origin/)", "", args.head)
        is_branch = bool(git("show-ref", "--verify", "--quiet", f"refs/heads/{branch}") is not None or
                         git("show-ref", "--verify", "--quiet", f"refs/remotes/origin/{branch}") is not None)
        if not is_branch:
            branch = args.head
    if args.source:
        branch, is_branch = args.source, True
    if not args.target:
        target = infer_target(branch)
        remote_target = f"origin/{target}" if git("rev-parse", "--verify", "--quiet", f"origin/{target}") else target

    problems, notes = branch_checks(branch, target) if is_branch else ([], ["head is not a branch: naming rules skipped"])
    sync = {"upstream": None, "checked_out": current}
    if current:
        upstream = (git("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}") or "").strip()
        sync["upstream"] = upstream or None
        if upstream:
            behind, ahead = (git("rev-list", "--left-right", "--count", f"{upstream}...HEAD") or "0 0").split()
            sync.update({"behind_upstream": int(behind), "ahead_of_upstream": int(ahead)})
        sync["uncommitted_tracked_changes"] = len(
            [l for l in (git("status", "--porcelain", "--untracked-files=no") or "").splitlines() if l.strip()])
    sync["behind_target"] = int((git("rev-list", "--count", f"{head}..{remote_target}") or "0").strip() or 0)
    sync["merges_in_branch"] = int((git("rev-list", "--count", "--merges", f"{base}..{head}") or "0").strip() or 0)

    commits = []
    log = git("log", "--no-merges", "--format=%h%x09%cs%x09%an%x09%s%x09%b%x1e", f"{base}..{head}") or ""
    for record in log.split("\x1e"):
        record = record.strip("\n")
        if not record:
            continue
        sha, date, author, subject, body = (record.split("\t", 4) + [""])[:5]
        commits.append({"sha": sha, "date": date, "author": author, "subject": subject,
                        "body": "\n".join(l for l in body.strip().splitlines() if l.strip())})
    ported = ports_already_released(commits, remote_target)

    # --no-renames everywhere: a moved file is reported as D (old path) + A (new path), simple and unambiguous
    status_by_path = {}
    for line in (git("diff", "--name-status", "--no-renames", base, head) or "").splitlines():
        status, path = line.split("\t", 1)
        status_by_path[path] = status
    files = []
    for line in (git("diff", "--numstat", "--no-renames", base, head) or "").splitlines():
        added, removed, path = line.split("\t", 2)
        if path.startswith(EXCLUDED):
            continue
        files.append({"file": path, "status": status_by_path.get(path, "M"),
                      "added": int(added) if added != "-" else 0, "removed": int(removed) if removed != "-" else 0,
                      "kind": classify(path), "module": module_of(path), "concept": concept_of(path)})

    signals = signals_for(base, head, files)

    commit_types, commit_scopes = Counter(), Counter()
    for commit in commits:
        match = re.match(r"^(\w+)(\(([^)]+)\))?!?:", commit["subject"])
        if match and match.group(1) in CC_TYPES:
            commit_types[match.group(1)] += 1
            if match.group(3):
                commit_scopes[match.group(3)] += 1
    if "/fix/" in branch or branch.startswith("hotfix/"):
        suggested_type = "fix"
    elif commit_types:
        suggested_type = commit_types.most_common(1)[0][0]
    elif files and all(f["kind"] == "doc" for f in files):
        suggested_type = "docs"
    else:
        suggested_type = "feat"
    concepts = Counter(f["concept"] for f in files if f["concept"])
    scope_candidates = [s for s, _ in commit_scopes.most_common(3)] + \
                       [c for c, _ in concepts.most_common(3) if c not in commit_scopes]
    template_dir = ".gitlab/merge_request_templates"
    templates = sorted(p for p in os.listdir(template_dir) if p.endswith(".md")) if os.path.isdir(template_dir) else []
    prefix = "bug_fix" if suggested_type == "fix" else "feature"
    template = next((t for t in templates if t.startswith(prefix)), None)

    remote_url = (git("remote", "get-url", "origin") or "").strip()
    web = re.sub(r"^ssh://git@([^/:]+)(:\d+)?/", r"https://\1/", remote_url)
    web = re.sub(r"^git@([^:]+):", r"https://\1/", web)
    web = re.sub(r"\.git$", "", web)
    slug = re.sub(r"[^A-Za-z0-9._-]+", "_", branch).strip("_") or "detached"
    draft_dir = must("rev-parse", "--git-path", "mr-drafts").strip()
    result = {
        "branch": branch, "target": target, "base": base[:12], "head": head[:12],
        "branch_checks": {"ci_blocking": problems, "conventions": notes},
        "sync": sync, "commits": commits, "files": files,
        "totals": {"files": len(files), "added": sum(f["added"] for f in files),
                   "removed": sum(f["removed"] for f in files),
                   "by_kind": dict(Counter(f["kind"] for f in files)),
                   "by_module": dict(Counter(f["module"] for f in files))},
        "signals": signals,
        "ported": ported,
        "suggested": {"type": suggested_type, "scope_candidates": scope_candidates,
                      "template": f"{template_dir}/{template}" if template else None,
                      "changelog": suggest_changelog(suggested_type, commits, ported)},
        "templates": [f"{template_dir}/{t}" for t in templates],
        "draft_paths": {"description": os.path.join(draft_dir, f"{slug}.md"),
                        "title": os.path.join(draft_dir, f"{slug}.title")},
        "new_mr_url": (f"{web}/-/merge_requests/new?merge_request%5Bsource_branch%5D={branch}"
                       f"&merge_request%5Btarget_branch%5D={target}") if web.startswith("https://") else None,
    }
    if args.json:
        print(json.dumps(result, indent=1, ensure_ascii=False))
    else:
        print_markdown(result)


def print_markdown(r):
    t, s = r["totals"], r["sync"]
    print(f"# MR facts: `{r['branch']}` -> `{r['target']}`\n")
    print(f"- Range: `{r['base']}..{r['head']}` - {len(r['commits'])} commits ({s['merges_in_branch']} merges) - "
          f"{t['files']} files, +{t['added']} -{t['removed']}")
    print(f"- By kind: {', '.join(f'{k} {v}' for k, v in sorted(t['by_kind'].items())) or '-'}")
    print(f"- By module: {', '.join(f'{k} {v}' for k, v in sorted(t['by_module'].items())) or '-'}")
    for problem in r["branch_checks"]["ci_blocking"]:
        print(f"- **CI BLOCKING**: {problem}")
    for note in r["branch_checks"]["conventions"]:
        print(f"- convention: {note}")
    if not s["checked_out"]:
        print("- not the checked-out commit: push/sync state not computed")
    elif not s["upstream"]:
        print("- branch not pushed (no upstream)")
    elif s.get("ahead_of_upstream") or s.get("behind_upstream"):
        print(f"- vs {s['upstream']}: {s.get('ahead_of_upstream', 0)} to push, {s.get('behind_upstream', 0)} to pull")
    if s["behind_target"] and s["checked_out"]:
        print(f"- {s['behind_target']} commits of `{r['target']}` are not in the branch: merge it before the MR "
              f"(team rule: merge, not rebase)")
    if s.get("uncommitted_tracked_changes"):
        print(f"- {s['uncommitted_tracked_changes']} uncommitted tracked changes: NOT part of the MR")
    print("\n## Commits\n")
    for c in r["commits"] or [{"sha": "-", "date": "", "subject": "no commit in range", "author": "-", "body": ""}]:
        print(f"- `{c['sha']}` {c['date']} {c['subject']} ({c['author']})")
        body = c["body"].splitlines()
        for line in body[:4]:
            print(f"  > {line[:110]}")
        if len(body) > 4:
            print(f"  > ... ({len(body) - 4} more lines: git log -1 {c['sha']})")
    for port in r["ported"]:
        if port["changelog_ref"]:
            print(f"- **port of a released change**: `{port['commit']}` re-applies {port['mr']}, already in "
                  f"CHANGELOG.md of {port['changelog_ref']} (version {port['version']})")
        else:
            print(f"- cherry-pick: `{port['commit']}` <- {', '.join(port['cherry_pick_of'])} (not found in a CHANGELOG)")
    print("\n## Files\n")
    by_module = defaultdict(list)
    for f in r["files"]:
        by_module[f["module"]].append(f)
    for module, rows in sorted(by_module.items()):
        print(f"**{module}**")
        for f in rows:
            print(f"- {f['status'][0]} `{f['file']}` +{f['added']} -{f['removed']} ({f['kind']})")
    print("\n## Signals for the description\n")
    if not r["signals"]:
        print("- none")
    for key, values in sorted(r["signals"].items()):
        shown = values if len(values) <= 8 else values[:8]
        more = f" ... and {len(values) - 8} more" if len(values) > 8 else ""
        print(f"- **{key}**: " + "; ".join(f"`{v}`" for v in shown) + more)
    sg = r["suggested"]
    print("\n## Suggested\n")
    print(f"- title type: `{sg['type']}`; scope candidates: {', '.join(sg['scope_candidates']) or '-'}")
    print(f"- template: `{sg['template']}`")
    print(f"- changelog: {sg['changelog']}")
    print(f"- draft: `{r['draft_paths']['description']}` (title in `{r['draft_paths']['title']}`)")
    if r["new_mr_url"]:
        print(f"- new MR page: {r['new_mr_url']}")


if __name__ == "__main__":
    main()
