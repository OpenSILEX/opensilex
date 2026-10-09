#!/usr/bin/env python3
"""Gather the facts an OpenSILEX merge request description is written from.

Prints (Markdown, or JSON with --json): branch and target, the GitLab CI rules the MR will be checked against,
push/sync state, commits with their bodies, ports of already released changes, changed files by module and kind,
signals a reviewer must hear about (see mr_signals.py), a suggested title type/scope and changelog decision, the
template to use, where to save the draft and the URL that opens a new MR.

It only reads git. Run from the repository root:
  mr_facts.py                      # current branch, target inferred (feature/* -> develop, hotfix/* -> master)
  mr_facts.py --target release     # explicit target branch
  mr_facts.py --base <sha>~1 --head <sha> --source feature/dev/name    # a past commit, without checking it out
  mr_facts.py --json
"""
import argparse
import json
import os
import re
import sys
from collections import Counter, defaultdict

from mr_signals import classify, concept_of, git, module_of, ports_already_released, signals_for

CC_TYPES = ("build", "chore", "ci", "docs", "feat", "fix", "perf", "refactor", "revert", "style", "test")
NO_CHANGELOG_TYPES = ("build", "chore", "ci", "docs", "refactor", "style", "test")
EXCLUDED = ("target/", "node_modules/", ".kilo/", "graft/", "site/", ".idea/", ".node/", ".claude/")
TEMPLATE_DIR = ".gitlab/merge_request_templates"


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


def remote_ref(target):
    return f"origin/{target}" if git("rev-parse", "--verify", "--quiet", f"origin/{target}") else target


def resolve_refs(args):
    """{branch, is_branch, checked_out, target, remote_target, base, head} of the MR described."""
    head = must("rev-parse", args.head).strip()
    checked_out = head == must("rev-parse", "HEAD").strip()
    branch = (git("branch", "--show-current") or "").strip() if checked_out else ""
    is_branch = bool(branch)
    if not checked_out:  # another ref: describe it, not the checked-out branch
        branch = re.sub(r"^(refs/heads/|refs/remotes/origin/|origin/)", "", args.head)
        is_branch = any(git("show-ref", "--verify", "--quiet", ref) is not None
                        for ref in (f"refs/heads/{branch}", f"refs/remotes/origin/{branch}"))
    if args.source:
        branch, is_branch = args.source, True
    if not is_branch:
        branch = args.head if not checked_out else "(detached HEAD)"
    target = args.target or infer_target(branch)
    remote_target = remote_ref(target)
    base = (git("rev-parse", "--verify", "--quiet", args.base + "^{commit}") if args.base
            else git("merge-base", head, remote_target)) or ""
    if not base.strip():
        sys.exit(f"No merge-base between {args.head} and {remote_target}: fetch it (git fetch origin {target})")
    return {"branch": branch, "is_branch": is_branch, "checked_out": checked_out, "target": target,
            "remote_target": remote_target, "base": base.strip(), "head": head}


def sync_state(refs):
    """Push state, commits of the target missing from the branch, uncommitted changes."""
    base, head = refs["base"], refs["head"]
    sync = {"upstream": None, "checked_out": refs["checked_out"]}
    if refs["checked_out"]:
        upstream = (git("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}") or "").strip()
        sync["upstream"] = upstream or None
        if upstream:
            behind, ahead = (git("rev-list", "--left-right", "--count", f"{upstream}...HEAD") or "0 0").split()
            sync.update({"behind_upstream": int(behind), "ahead_of_upstream": int(ahead)})
        status = git("status", "--porcelain", "--untracked-files=no") or ""
        sync["uncommitted_tracked_changes"] = len([l for l in status.splitlines() if l.strip()])
    sync["behind_target"] = int((git("rev-list", "--count", f"{head}..{refs['remote_target']}") or "0").strip() or 0)
    sync["merges_in_branch"] = int((git("rev-list", "--count", "--merges", f"{base}..{head}") or "0").strip() or 0)
    return sync


def read_commits(base, head):
    commits = []
    log = git("log", "--no-merges", "--format=%h%x09%cs%x09%an%x09%s%x09%b%x1e", f"{base}..{head}") or ""
    for record in log.split("\x1e"):
        record = record.strip("\n")
        if record:
            sha, date, author, subject, body = (record.split("\t", 4) + [""])[:5]
            commits.append({"sha": sha, "date": date, "author": author, "subject": subject,
                            "body": "\n".join(l for l in body.strip().splitlines() if l.strip())})
    return commits


def changed_files(base, head):
    """--no-renames: a moved file is reported as D (old path) + A (new path), simple and unambiguous."""
    status_by_path = {}
    for line in (git("diff", "--name-status", "--no-renames", base, head) or "").splitlines():
        status, path = line.split("\t", 1)
        status_by_path[path] = status
    files = []
    for line in (git("diff", "--numstat", "--no-renames", base, head) or "").splitlines():
        added, removed, path = line.split("\t", 2)
        if not path.startswith(EXCLUDED) or path.startswith(".claude/skills/"):  # versioned skills are content
            files.append({"file": path, "status": status_by_path.get(path, "M"),
                          "added": int(added) if added != "-" else 0, "removed": int(removed) if removed != "-" else 0,
                          "kind": classify(path), "module": module_of(path), "concept": concept_of(path)})
    return files


def suggest_changelog(kind, commits, ported):
    released = {p["commit"] for p in ported if p["changelog_ref"]}
    if commits and all(c["sha"] in released for c in commits):
        return ("ignore-changelog: true - every commit ports a change already listed in a CHANGELOG.md "
                "(say so in Points d'attention)")
    if released:
        return "changelog entry for the new work only - some commits port changes already in a CHANGELOG.md"
    return "ignore-changelog: true" if kind in NO_CHANGELOG_TYPES else "changelog entry (user point of view, English)"


def list_templates():
    return sorted(p for p in os.listdir(TEMPLATE_DIR) if p.endswith(".md")) if os.path.isdir(TEMPLATE_DIR) else []


def suggest(branch, commits, files, ported):
    """Title type and scope candidates, template, changelog decision."""
    types, scopes = Counter(), Counter()
    for commit in commits:
        match = re.match(r"^(\w+)(\(([^)]+)\))?!?:", commit["subject"])
        if match and match.group(1) in CC_TYPES:
            types[match.group(1)] += 1
            scopes.update([match.group(3)] if match.group(3) else [])
    if "/fix/" in branch or branch.startswith("hotfix/"):
        kind = "fix"
    elif types:
        kind = types.most_common(1)[0][0]
    else:
        kind = "docs" if files and all(f["kind"] == "doc" for f in files) else "feat"
    concepts = Counter(f["concept"] for f in files if f["concept"])
    template = next((t for t in list_templates() if t.startswith("bug_fix" if kind == "fix" else "feature")), None)
    return {"type": kind, "changelog": suggest_changelog(kind, commits, ported),
            "scope_candidates": [s for s, _ in scopes.most_common(3)]
                                + [c for c, _ in concepts.most_common(3) if c not in scopes],
            "template": f"{TEMPLATE_DIR}/{template}" if template else None}


def new_mr_url(branch, target):
    remote = (git("remote", "get-url", "origin") or "").strip()
    web = re.sub(r"^ssh://git@([^/:]+)(:\d+)?/", r"https://\1/", remote)
    web = re.sub(r"\.git$", "", re.sub(r"^git@([^:]+):", r"https://\1/", web))
    if not web.startswith("https://"):
        return None
    return (f"{web}/-/merge_requests/new?merge_request%5Bsource_branch%5D={branch}"
            f"&merge_request%5Btarget_branch%5D={target}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--target", help="target branch (default: inferred from the branch name)")
    parser.add_argument("--head", default="HEAD", help="head ref (default: HEAD, committed work only)")
    parser.add_argument("--base", help="base ref (default: merge-base of head with origin/<target>)")
    parser.add_argument("--source", help="source branch name when --head is a commit (default: the branch of --head)")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    os.chdir(must("rev-parse", "--show-toplevel").strip())

    refs = resolve_refs(args)
    branch, target, base, head = refs["branch"], refs["target"], refs["base"], refs["head"]
    if refs["is_branch"]:
        problems, notes = branch_checks(branch, target)
    else:
        problems, notes = [], ["head is not a branch: naming rules skipped"]
    commits, files = read_commits(base, head), changed_files(base, head)
    ported = ports_already_released(commits, refs["remote_target"])
    slug = re.sub(r"[^A-Za-z0-9._-]+", "_", branch).strip("_") or "detached"
    draft_dir = must("rev-parse", "--git-path", "mr-drafts").strip()
    result = {
        "branch": branch, "target": target, "base": base[:12], "head": head[:12],
        "branch_checks": {"ci_blocking": problems, "conventions": notes}, "sync": sync_state(refs),
        "commits": commits, "files": files, "ported": ported, "signals": signals_for(base, head, files),
        "totals": {"files": len(files), "added": sum(f["added"] for f in files),
                   "removed": sum(f["removed"] for f in files), "by_kind": dict(Counter(f["kind"] for f in files)),
                   "by_module": dict(Counter(f["module"] for f in files))},
        "suggested": suggest(branch, commits, files, ported),
        "templates": [f"{TEMPLATE_DIR}/{t}" for t in list_templates()],
        "draft_paths": {"description": os.path.join(draft_dir, f"{slug}.md"),
                        "title": os.path.join(draft_dir, f"{slug}.title")},
        "new_mr_url": new_mr_url(branch, target),
    }
    if args.json:
        print(json.dumps(result, indent=1, ensure_ascii=False))
    else:
        print_summary(result)
        print_details(result)


def print_summary(r):
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
              "(team rule: merge, not rebase)")
    if s.get("uncommitted_tracked_changes"):
        print(f"- {s['uncommitted_tracked_changes']} uncommitted tracked changes: NOT part of the MR")


def print_details(r):
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
            print(f"- cherry-pick: `{port['commit']}` <- {', '.join(port['cherry_pick_of'])} (not in a CHANGELOG)")
    print("\n## Files\n")
    by_module = defaultdict(list)
    for f in r["files"]:
        by_module[f["module"]].append(f)
    for module, rows in sorted(by_module.items()):
        print(f"**{module}**")
        for f in rows:
            print(f"- {f['status'][0]} `{f['file']}` +{f['added']} -{f['removed']} ({f['kind']})")
    print("\n## Signals for the description\n")
    for key, values in sorted(r["signals"].items()):
        more = f" ... and {len(values) - 8} more" if len(values) > 8 else ""
        print(f"- **{key}**: " + "; ".join(f"`{v}`" for v in values[:8]) + more)
    if not r["signals"]:
        print("- none")
    sg = r["suggested"]
    print("\n## Suggested\n")
    print(f"- title type: `{sg['type']}`; scope candidates: {', '.join(sg['scope_candidates']) or '-'}")
    print(f"- template: `{sg['template']}`\n- changelog: {sg['changelog']}")
    print(f"- draft: `{r['draft_paths']['description']}` (title in `{r['draft_paths']['title']}`)")
    if r["new_mr_url"]:
        print(f"- new MR page: {r['new_mr_url']}")


if __name__ == "__main__":
    main()
