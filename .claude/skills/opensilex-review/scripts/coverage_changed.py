#!/usr/bin/env python3
"""Line coverage of the CHANGED Java lines, read from JaCoCo XML reports.

Produce the reports first (references/coverage.md), for example:
  rm -f opensilex-core/target/jacoco*.exec      # otherwise older runs are merged into the report
  .claude/skills/opensilex-java/scripts/jdk.sh mvn -o -pl opensilex-core -Pwith-test-report verify \
      -Dtest=WidgetAPITest -Dsurefire.failIfNoSpecifiedTests=false -DskipFrontBuild \
      -Dcheckstyle.skip -Dspotbugs.skip -Dpmd.skip -Dcpd.skip
then, from the repository root:
  coverage_changed.py                         # changed lines vs merge-base with origin/develop (+ working tree)
  coverage_changed.py --base A --head B       # same ranges as review_scope.py
  coverage_changed.py --reports site/opensilex-core/jacoco/jacoco.xml

A changed line counts only when JaCoCo considers it executable (declarations, blank lines and
comments are ignored). "Uncovered" means no test of THAT run executed it.
"""
import argparse
import glob
import os
import sys
import xml.etree.ElementTree as ElementTree

from scope_git import Snapshot, git, line_ranges, load_changes, resolve_base, resolve_commit


def load_reports(paths):
    """{"org/opensilex/x/Y.java": {line: (missed_instructions, covered_instructions, missed_branches)}}"""
    coverage = {}
    for path in paths:
        try:
            root = ElementTree.parse(path).getroot()
        except (ElementTree.ParseError, OSError) as error:
            print(f"skip {path}: {error}", file=sys.stderr)
            continue
        for package in root.iter("package"):
            for sourcefile in package.findall("sourcefile"):
                lines = coverage.setdefault(f"{package.get('name')}/{sourcefile.get('name')}", {})
                for line in sourcefile.findall("line"):
                    number = int(line.get("nr"))
                    current = (int(line.get("mi")), int(line.get("ci")), int(line.get("mb")))
                    if number not in lines or current[1] > lines[number][1]:  # several reports: keep the best
                        lines[number] = current
    return coverage


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", help="base ref (default: merge-base of HEAD with origin/develop)")
    parser.add_argument("--head", help="head ref (default: working tree, untracked files included)")
    parser.add_argument("--reports", nargs="*", help="jacoco.xml files (default: site/**/jacoco.xml)")
    args = parser.parse_args()

    os.chdir(git("rev-parse", "--show-toplevel").strip())
    reports = args.reports or sorted(set(glob.glob("site/**/jacoco.xml", recursive=True)
                                         + glob.glob("*/target/site/jacoco/jacoco.xml")))
    reports = [p for p in reports if os.path.getsize(p) > 1024]  # site/jacoco.xml is an empty aggregate
    if not reports:
        sys.exit("No jacoco.xml with data: run the tests with -Pwith-test-report first (references/coverage.md)")
    for path in reports:
        print(f"report: {path}", file=sys.stderr)
    coverage = load_reports(reports)

    base, _ = resolve_base(args.base)
    head = resolve_commit(args.head) if args.head else None
    if head and head != git("rev-parse", "HEAD").strip():
        sys.exit(f"--head {args.head} is not the checked-out commit: JaCoCo line numbers describe the checked-out "
                 "sources. Check the branch out (or use a worktree), rebuild the report, retry.")
    parsed, _, _, _ = load_changes(base, head, Snapshot(head), ["*.java"])

    total_exec = total_covered = 0
    print("| file | changed executable lines | covered | uncovered lines |")
    print("|---|---|---|---|")
    for path, entry in sorted(parsed.items()):
        if "/src/main/java/" not in path or entry.get("status") == "D" or not entry["added"]:
            continue
        lines = coverage.get(path.split("/src/main/java/", 1)[1])
        if lines is None:
            print(f"| {path} | ? | not in any report (module not run, or class never loaded) | |")
            continue
        executable = [n for n, _ in entry["added"] if n in lines]
        covered = [n for n in executable if lines[n][1] > 0]
        partial = [n for n in covered if lines[n][2] > 0]
        missed = line_ranges(n for n in executable if lines[n][1] == 0)
        total_exec += len(executable)
        total_covered += len(covered)
        pct = f"{100 * len(covered) / len(executable):.0f}%" if executable else "n/a"
        partial_text = f" (branches partly missed: {', '.join(map(str, partial[:8]))})" if partial else ""
        missed_text = ", ".join(f"{a}-{b}" if a != b else str(a) for a, b in missed) or "-"
        print(f"| {path} | {len(executable)} | {len(covered)} ({pct}){partial_text} | {missed_text} |")
    if total_exec:
        print(f"\nChanged executable lines covered: {total_covered}/{total_exec} "
              f"({100 * total_covered / total_exec:.0f}%), by the tests of the run(s) above only.")
    else:
        print("\nNo changed executable line found in the reports.")


if __name__ == "__main__":
    main()
