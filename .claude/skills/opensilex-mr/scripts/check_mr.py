#!/usr/bin/env python3
"""Check an OpenSILEX merge request draft before it is published.

ERROR = GitLab CI (`merge-request:check`) would fail, or the draft still contains template placeholders or
unresolved "A completer" markers. WARN = the team conventions or readability suffer. INFO = worth a second look.
Exit status 1 when an ERROR is found.

  check_mr.py DRAFT.md --title "fix(germplasm): Fixed ..."      # title given inline
  check_mr.py DRAFT.md                                            # title read from DRAFT.title when it exists
  check_mr.py DRAFT.md --template .gitlab/merge_request_templates/bug_fix1.md --source feature/x/fix/y --target develop
"""
import argparse
import os
import re
import subprocess
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    yaml = None

# Same pattern as .gitlab-ci.yml (merge-request:check): [[:alnum:]] is Unicode-aware in the job's UTF-8 shell and the
# pattern is not anchored at the end. ASCII-only is a team convention (English titles), checked separately as a WARN.
CC_TITLE_RE = re.compile(
    r"^(Draft: )?(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\([\w.-]+\))?(!)?: [^\W_]")
FRENCH_RE = re.compile(r"[àâçéèêëîïôûùüÿœ]|\b(le|la|les|des|du|une|est|sont|pour|avec|dans|sur|ne|pas|lors|"
                       r"ajout|correction|corrige|donnees|maintenant|cette|ces)\b", re.IGNORECASE)
# "method", "class", "unit" and "variable" are OpenSILEX domain words: not listed here.
DEV_CENTRIC_RE = re.compile(r"\b(refactor\w*|DAO|DTO|this MR|cette MR|merge request|unit tests?|NPE|"
                            r"null ?pointer\w*|stack ?trace|\w+\.(java|vue|ts))\b", re.IGNORECASE)
TODO_MARKER_RE = re.compile(r"<!--\s*(À|A) compl[ée]ter|\bTODO\b|\?\?\?|XXX", re.IGNORECASE)
FILE_REF_RE = re.compile(r"`([\w./-]+\.(?:java|vue|ts|js|json|md|ya?ml|xml|properties|owl|ttl))`")
GENERIC_LINKS = ("https://trello.com/)", "https://trello.com/>")


def git(*args):
    result = subprocess.run(["git", *args], capture_output=True, text=True, errors="replace")
    return result.stdout if result.returncode == 0 else None


def split_front_matter(text):
    """(front_matter_text or None, body). GitLab CI extracts it only when the description STARTS with ---."""
    lines = text.split("\n")
    if not lines or lines[0].strip() != "---":
        return None, text
    for i in range(1, len(lines)):
        if lines[i].strip() == "---":
            return "\n".join(lines[1:i]), "\n".join(lines[i + 1:])
    return None, text


def checklist_items(body):
    """Texts of `- [ ]` / `- [x]` items, indented continuation lines included."""
    items, current = [], None
    for line in body.split("\n"):
        match = re.match(r"^\s*- \[[ xX]\]\s+(.*)$", line)
        if match:
            if current is not None:
                items.append(current)
            current = match.group(1)
        elif current is not None and line.startswith("  ") and line.strip() and not line.strip().startswith("- "):
            current += " " + line.strip()
        else:
            if current is not None:
                items.append(current)
            current = None
    if current is not None:
        items.append(current)
    return items


def normalise_item(text):
    text = re.sub(r"\s*[—–-]\s*(non concern[ée]e?|n/?a|sans objet)\.?\s*$", "", text.strip(), flags=re.IGNORECASE)
    text = re.sub(r"\s*\((non concern[ée]e?|n/?a|sans objet)\)\s*$", "", text, flags=re.IGNORECASE)
    return re.sub(r"\s+", " ", text).lower()


def ticked(body):
    return [re.sub(r"\s+", " ", m.group(1)).strip()
            for m in re.finditer(r"^\s*- \[[xX]\]\s+(.*)$", body, flags=re.MULTILINE)]


def placeholder_lines(template_body):
    """Lines of the template body that are example content (to replace, never to publish as is)."""
    result, in_item = [], False
    for line in template_body.split("\n"):
        stripped = line.strip()
        if re.match(r"^- \[[ xX]\]", stripped):
            in_item = True
            continue
        if in_item and line.startswith("  ") and stripped:
            continue  # continuation of a checklist item
        in_item = False
        if not stripped:
            continue
        if stripped.startswith("#") and not re.search(r"sous[- ]partie", stripped, re.IGNORECASE):
            continue  # real section headings are kept
        result.append(stripped)
    return result


def sections(body):
    """[(level, title, content_lines)] by Markdown heading (code blocks ignored)."""
    result, current, in_code = [], None, False
    for line in body.split("\n"):
        if line.strip().startswith("```"):
            in_code = not in_code
        match = None if in_code else re.match(r"^(#{1,6})\s+(.*)$", line)
        if match:
            current = (len(match.group(1)), match.group(2).strip(), [])
            result.append(current)
        elif current is not None:
            current[2].append(line)
    return result


class Report:
    """ERROR = the CI would reject the MR or placeholders remain; WARN = conventions or readability; INFO = look."""

    def __init__(self):
        self.errors, self.warnings, self.infos = [], [], []

    def print_and_exit(self):
        for label, items in (("ERROR", self.errors), ("WARN", self.warnings), ("INFO", self.infos)):
            for item in items:
                print(f"{label:5} {item}")
        if not self.errors and not self.warnings:
            print("OK    no problem found")
        print(f"\n{len(self.errors)} error(s), {len(self.warnings)} warning(s)")
        sys.exit(1 if self.errors else 0)


def check_title(title, report):
    if not title:
        report.errors.append("no title given (--title, or a .title file next to the draft)")
        return
    if "\n" in title:
        report.errors.append("title spans several lines")
    if not CC_TITLE_RE.match(title.split("\n")[0]):
        report.errors.append(f"title does not match the CI Conventional Commit pattern: {title!r} (type(scope): "
                             "Description; known type; scope without spaces; description starting with a letter "
                             "or digit)")
    if re.search(r"[^\x20-\x7e]", title):
        report.warnings.append("title has non-ASCII characters: accepted by the CI, but titles are English (team "
                               "convention) and become the merge commit subject")
    description = re.sub(r"^(Draft: )?\w+(\([^)]*\))?!?: ", "", title)
    if len(description.split()) < 4:
        report.warnings.append(f"title description too vague ({description!r}): say what changed for whom (team "
                               "example: 'Fixed target column being able to be put at end during data import')")
    if len(title) > 100:
        report.warnings.append(f"title is {len(title)} characters: it becomes the merge commit subject, aim for < 100")
    if FRENCH_RE.search(description):
        report.errors.append("title must be written in English (team rule; the CI regex alone would accept it)")


def read_front_matter(front, report):
    """(changelog text, ignore-changelog value) as the CI reads them."""
    if yaml is None:
        report.warnings.append("PyYAML missing: front matter checked with a simple parser")
        changelog = re.search(r"^changelog:[ \t]*(.*)$", front, re.MULTILINE)
        ignore = re.search(r"^ignore-changelog:[ \t]*(\S+)", front, re.MULTILINE)
        return (changelog.group(1) if changelog else "").strip(), ignore.group(1) if ignore else None
    try:
        data = yaml.safe_load(front) or {}
    except yaml.YAMLError as error:
        report.errors.append(f"front matter is not valid YAML: {error}")
        return "", None
    if not isinstance(data, dict):
        return "", None
    changelog = data.get("changelog") or ""
    if not isinstance(changelog, str):
        report.errors.append("changelog must be text (use `changelog: |-` followed by indented lines)")
        changelog = str(changelog)
    unknown = set(data) - {"changelog", "ignore-changelog"}
    if unknown:
        report.infos.append(f"extra front matter keys ignored by the CI: {', '.join(sorted(unknown))}")
    return changelog, data.get("ignore-changelog")


def check_front_matter(front, title, report):
    if front is None:
        report.errors.append("the description must START with the YAML front matter (--- changelog / "
                             "ignore-changelog ---): the CI extracts it from the first line")
        return
    changelog, ignore = read_front_matter(front, report)
    ignore_true = str(ignore).lower() == "true"
    if changelog.strip() and ignore_true:
        report.errors.append("changelog is filled AND ignore-changelog is true: the CI rejects this, keep one")
    elif not changelog.strip() and not ignore_true:
        report.errors.append("changelog is empty and ignore-changelog is not true: the CI rejects this")
    if re.search(r"#\s*(Remplir cette section|Passer cette valeur)", front):
        report.warnings.append("template comments left in the front matter: remove them")
    if changelog.strip():
        if re.search(r"^changelog:[ \t]*[^|>\s]", front, re.MULTILINE) and len(changelog) > 100:
            report.warnings.append("long changelog on one line: use `changelog: |-` and wrap lines")
        if FRENCH_RE.search(changelog):
            report.warnings.append("changelog should be written in English (it is copied into CHANGELOG.md)")
        if DEV_CENTRIC_RE.search(changelog):
            report.infos.append("changelog uses code-level words: describe the change from the user's point of view "
                                "('You can now ...', '... now works correctly') unless API users are the audience")
        if re.match(r"^\s*(-\s*)?\(!\d+\)", changelog):
            report.infos.append("the `(!MR) [Scope]` prefix is added when CHANGELOG.md is compiled: not needed here")
    elif ignore_true:
        report.warnings.append("team rule: always write the changelog entry with `ignore-changelog: false`; only a "
                               "port of an already released change may stay ignored, if the author chose it")
    if changelog.strip() and str(ignore).lower() != "false":
        report.warnings.append("team rule: keep both lines in the front matter, add `ignore-changelog: false`")


def load_template(top, template_path, title_type, report):
    """Body of the template the draft follows (given, or chosen from the title type)."""
    template_dir = os.path.join(top, ".gitlab", "merge_request_templates")
    if template_path is None and os.path.isdir(template_dir):
        prefix = "bug_fix" if title_type == "fix" else "feature"
        candidates = sorted(p for p in os.listdir(template_dir) if p.startswith(prefix) and p.endswith(".md"))
        template_path = os.path.join(template_dir, candidates[0]) if candidates else None
    if not template_path or not os.path.exists(template_path):
        report.warnings.append("template not found: checklist and example content not compared")
        return ""
    report.infos.append(f"compared with template {os.path.relpath(template_path, top)}")
    with open(template_path, encoding="utf-8") as handle:
        return split_front_matter(handle.read())[1]


def check_template(body, template_body, report):
    if template_body:
        present = [normalise_item(i) for i in checklist_items(body)]
        for item in checklist_items(template_body):
            expected = normalise_item(item)
            # a note may follow the template text: "Tests écrits et OK (pas de tests front automatisés)"
            if not any(p == expected or p.startswith(expected + " ") for p in present):
                report.warnings.append(f"checklist item of the template missing or reworded: '{item[:70]}'")
        draft_lines = {l.strip() for l in body.split("\n")}
        for line in placeholder_lines(template_body):
            if line in draft_lines:
                report.errors.append(f"template example content left: '{line[:80]}'")
    for item in ticked(body):
        if item.lower().startswith("relecture"):
            report.warnings.append("'Relecture MR' is ticked: the reviewer ticks it, not the author")
    if any(link in body for link in GENERIC_LINKS):
        report.errors.append("generic Trello link left ([Carte trello](https://trello.com/)): put the card URL or "
                             "remove it")


def check_structure(text, body, title_type, report):
    found = sections(body)
    titles = [t.lower() for _, t, _ in found]
    for required in ("contexte", "changements"):
        if required not in titles:
            report.errors.append(f"section '# {required.capitalize()}' missing (template structure)")
    if title_type == "fix":
        for required in ("avant", "après"):
            if required not in titles:
                report.warnings.append(f"bug fix without '## {required.capitalize()}': show the behaviour before "
                                       "and after")
    for index, (level, heading, content) in enumerate(found):
        has_children = index + 1 < len(found) and found[index + 1][0] > level
        if not any(l.strip() for l in content) and not has_children:
            report.warnings.append(f"empty section '{heading}': fill it or remove it")
        if re.search(r"sous[- ]partie\s*\d", heading, re.IGNORECASE):
            report.errors.append(f"template heading left: '{heading}': name the sub-part after what it describes")
    for number, line in enumerate(text.split("\n"), 1):
        if TODO_MARKER_RE.search(line):
            report.errors.append(f"line {number}: unresolved marker: {line.strip()[:80]}")
    non_empty = [l for l in body.split("\n") if l.strip()]
    if len(non_empty) > 120:
        report.warnings.append(f"description is {len(non_empty)} lines: a reviewer reads ~60; group changes by intent")
    in_changes, bullets = False, 0
    for level, heading, content in found:
        if level == 1:
            in_changes = heading.lower() == "changements"
        if in_changes:
            bullets += sum(1 for l in content if re.match(r"^\s*- `", l))
    if bullets > 15:
        report.warnings.append(f"{bullets} file bullets under 'Changements': group them by intent, keep the files "
                               "that need explaining")


def check_cited_files(body, args, target, report):
    """Backticked file names should be part of the change, or be deliberate context."""
    head = (git("rev-parse", "--verify", "--quiet", args.head + "^{commit}") or "").strip()
    if args.base:
        base = (git("rev-parse", "--verify", "--quiet", args.base + "^{commit}") or "").strip()
    else:
        base = (git("merge-base", args.head, f"origin/{target}") or git("merge-base", args.head, target) or "").strip()
    if not base or not head:
        return
    changed = (git("diff", "--name-only", base, head) or "").split()
    names = {os.path.basename(p) for p in changed} | set(changed)
    for ref in sorted(set(FILE_REF_RE.findall(body))):
        if ref not in names and not any(p.endswith("/" + ref) for p in changed):
            report.infos.append(f"`{ref}` is cited but not changed in {base[:9]}..{head[:9]}: intended context, or "
                                "a typo?")


def check_branch(source, target, report):
    if target == "master" and not source.startswith("hotfix/"):
        report.errors.append("CI: only hotfix/* branches can target master")
    if target in ("develop", "release") and not source.startswith("feature/"):
        report.errors.append(f"CI: only feature/* branches can target {target}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("draft", help="MR description file (Markdown with YAML front matter)")
    parser.add_argument("--title", help="MR title (default: content of <draft without .md>.title)")
    parser.add_argument("--template", help="template the draft follows (default: from the title type)")
    parser.add_argument("--source", help="source branch (default: current branch)")
    parser.add_argument("--target", help="target branch (default: develop, master for hotfix/*)")
    parser.add_argument("--base", help="base ref for file references (default: merge-base with origin/<target>)")
    parser.add_argument("--head", default="HEAD", help="head ref for file references (default: HEAD)")
    args = parser.parse_args()

    top = (git("rev-parse", "--show-toplevel") or os.getcwd()).strip()
    with open(args.draft, encoding="utf-8") as handle:
        text = handle.read()
    title = args.title
    title_file = re.sub(r"\.md$", "", args.draft) + ".title"
    if title is None and os.path.exists(title_file):
        with open(title_file, encoding="utf-8") as handle:
            title = handle.read().strip()
    type_match = re.match(r"^(Draft: )?(\w+)", title or "")
    title_type = type_match.group(2) if type_match else None
    source = args.source or (git("branch", "--show-current") or "").strip()
    target = args.target or ("master" if source.startswith("hotfix/") else "develop")

    report = Report()
    front, body = split_front_matter(text)
    check_title(title, report)
    check_front_matter(front, title, report)
    check_template(body, load_template(top, args.template, title_type, report), report)
    check_structure(text, body, title_type, report)
    check_cited_files(body, args, target, report)
    if source:
        check_branch(source, target, report)
    report.print_and_exit()


if __name__ == "__main__":
    main()
