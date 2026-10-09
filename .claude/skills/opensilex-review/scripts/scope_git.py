"""Git access for the review scripts: refs, the reviewed snapshot, diff parsing and change profiles.

A Snapshot is the code as it is at `head` (a commit), or the working tree when `head` is None. Every read, file
listing and search goes through it, so a review of a past commit never mixes in newer files.
"""
import os
import re
import subprocess
import sys
from collections import Counter

# Copies, build output and generated code: never reviewed.
EXCLUDED_PREFIXES = ("target/", "node_modules/", ".kilo/", "graft/", "site/", ".idea/", ".node/", ".claude/")
EXCLUDED_PARTS = ("/target/", "/node_modules/")
GENERATED_PARTS = ("/front/src/lib/", "/front/types/")
NOT_ANALYSED_SUFFIXES = (".lock", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".pdf", ".zip", ".jar")
TEST_PATHSPEC = "*/src/test/java/*"


def git(*args):
    """stdout of a git command, or exit with its error."""
    result = subprocess.run(["git", *args], capture_output=True, text=True, errors="replace")
    if result.returncode != 0:
        sys.exit(f"git {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def try_git(*args):
    """stdout of a git command, or None when it fails (no match, unknown ref...)."""
    result = subprocess.run(["git", *args], capture_output=True, text=True, errors="replace")
    return result.stdout if result.returncode == 0 else None


def resolve_commit(ref):
    sha = try_git("rev-parse", "--verify", "--quiet", ref + "^{commit}")
    if not sha:
        sys.exit(f"Unknown ref: {ref}")
    return sha.strip()


def resolve_base(base):
    """(sha, label): the given ref, else the merge-base of HEAD with origin/develop (or develop)."""
    if base:
        return resolve_commit(base), base
    for candidate in ("origin/develop", "develop"):
        merge_base = try_git("merge-base", "HEAD", candidate)
        if merge_base:
            return merge_base.strip(), f"merge-base(HEAD, {candidate})"
    sys.exit("Cannot find origin/develop or develop: pass --base")


def excluded(path, explicit_paths=()):
    if explicit_paths and any(path.startswith(p.rstrip("/")) for p in explicit_paths):
        return False
    return path.startswith(EXCLUDED_PREFIXES) or any(part in "/" + path for part in EXCLUDED_PARTS)


class Snapshot:
    """The reviewed code: commit `head`, or the working tree (untracked files included) when head is None."""

    def __init__(self, head=None):
        self.head = head
        self._cache = {}
        self._files = None

    def read(self, path):
        if path not in self._cache:
            if self.head:
                content = try_git("show", f"{self.head}:{path}")
            else:
                try:
                    with open(path, encoding="utf-8", errors="replace") as handle:
                        content = handle.read()
                except OSError:
                    content = None
            self._cache[path] = content.splitlines() if content is not None else []
        return self._cache[path]

    def files(self):
        if self._files is None:
            if self.head:
                listing = git("ls-tree", "-r", "--name-only", self.head).splitlines()
            else:
                listing = git("ls-files").splitlines() + git("ls-files", "--others", "--exclude-standard").splitlines()
            self._files = {p for p in listing if not excluded(p)}
        return self._files

    def grep(self, pattern, pathspecs, fixed=False, word=False):
        """Paths whose content matches `pattern` (extended regex unless fixed)."""
        args = ["grep", "-l", "-I", "-F" if fixed else "-E"] + (["-w"] if word else []) + ["-e", pattern]
        args += [self.head] if self.head else ["--untracked"]
        out = try_git(*args, "--", *pathspecs) or ""
        prefix = f"{self.head}:" if self.head else ""
        return sorted(line[len(prefix):] for line in out.splitlines() if line)


def read_old(path, base):
    content = try_git("show", f"{base}:{path}")
    return content.splitlines() if content is not None else []


def parse_diff(diff_text):
    """`git diff -U0` -> {path: {status, old_path, path, binary, added: [(line, text)], removed: [(line, text)]}}."""
    files, current = {}, None
    old_line = new_line = 0
    for line in diff_text.splitlines():
        if line.startswith("diff --git "):
            match = re.match(r"diff --git a/(.*) b/(.*)$", line)
            current = {"status": "M", "old_path": match.group(1), "path": match.group(2),
                       "added": [], "removed": [], "binary": False}
            files[current["path"]] = current
        elif current is None or line.startswith(("+++ ", "--- ")):
            continue
        elif line.startswith("new file mode"):
            current["status"] = "A"
        elif line.startswith("deleted file mode"):
            current["status"] = "D"
        elif line.startswith("rename from "):
            current["status"] = "R"
        elif line.startswith("Binary files"):
            current["binary"] = True
        elif line.startswith("@@"):
            match = re.match(r"@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@", line)
            old_line, new_line = int(match.group(1)), int(match.group(2))
        elif line.startswith("+"):
            current["added"].append((new_line, line[1:]))
            new_line += 1
        elif line.startswith("-"):
            current["removed"].append((old_line, line[1:]))
            old_line += 1
    return files


def collect_untracked(explicit_paths=()):
    """(new files, skipped directories): new package directories are expanded, nested repositories are not."""
    out = git("ls-files", "--others", "--exclude-standard", "--directory", "--no-empty-directory",
              *(["--", *explicit_paths] if explicit_paths else []))
    kept, skipped = [], []
    for path in out.splitlines():
        if excluded(path, explicit_paths):
            continue
        if not path.endswith("/"):
            kept.append(path)
            continue
        if os.path.isdir(os.path.join(path, ".git")):
            skipped.append(path)
            continue
        sub = [p for p in git("ls-files", "--others", "--exclude-standard", "--", path).splitlines()
               if not excluded(p, explicit_paths)]
        if len(sub) <= 200:
            kept.extend(sub)
        else:
            skipped.append(path)
    return kept, skipped


def load_changes(base, head, snapshot, paths=()):
    """Changed files between base and head (or the working tree): (reviewable, not analysed, generated, skipped)."""
    rng = [base, head] if head else [base]
    parsed = parse_diff(git("diff", "-U0", "--no-color", "--find-renames", "--no-ext-diff", *rng, "--", *paths))
    skipped_dirs = []
    if not head:
        untracked, skipped_dirs = collect_untracked(paths)
        for path in untracked:
            if path not in parsed:
                parsed[path] = {"status": "A", "old_path": path, "path": path, "binary": False, "untracked": True,
                                "added": list(enumerate(snapshot.read(path), 1)), "removed": []}
    files, not_analysed, generated = {}, [], []
    for path, entry in sorted(parsed.items()):
        if excluded(path, paths):
            continue
        if any(part in "/" + path for part in GENERATED_PARTS):
            generated.append(path)
        elif entry["binary"] or path.endswith(NOT_ANALYSED_SUFFIXES):
            not_analysed.append(path)
        else:
            files[path] = entry
    return files, not_analysed, generated, skipped_dirs


CALL_ARGS_RE = re.compile(r"\b(?!(?:if|while|for|switch|catch|synchronized|return)\b)(\w+)\s*\([^()]*\)")


def strip_arguments(text):
    """The line with string literals and CALL argument lists emptied (conditions of if/while/for... are kept):
    equal results mean that only the arguments of calls changed."""
    text = re.sub(r'"(\\.|[^"\\])*"', '""', text.strip())
    previous = None
    while previous != text:  # innermost calls first; the placeholder has no parenthesis
        previous, text = text, CALL_ARGS_RE.sub(lambda m: m.group(1) + "…", text)
    return re.sub(r"\s+", " ", text)


def change_profile(entry):
    """`trivial`: added line numbers that only touch imports, blank lines, arguments or whitespace;
    `functional`: count of the other changed lines (added + removed)."""
    def is_import(text):
        return text.strip().startswith(("import ", "package ")) or not text.strip()

    removed = Counter(strip_arguments(t) for _, t in entry["removed"] if not is_import(t))
    trivial, functional_added = set(), 0
    for number, text in entry["added"]:
        key = strip_arguments(text)
        if is_import(text):
            trivial.add(number)
        elif removed[key] > 0:
            removed[key] -= 1
            trivial.add(number)
        else:
            functional_added += 1
    return {"trivial": trivial, "functional": functional_added + sum(removed.values())}


def line_ranges(numbers):
    ranges = []
    for n in sorted(numbers):
        if ranges and n == ranges[-1][1] + 1:
            ranges[-1][1] = n
        else:
            ranges.append([n, n])
    return ranges
