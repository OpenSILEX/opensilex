#!/usr/bin/env python3
"""Summarise an OpenSILEX profiling run recorded by profile.sh, or compare it with a reference run.

  analyze.py RUN                     per phase: process, heap, metaspace, classes, GC, CPU, containers; JFR top lists
  analyze.py RUN --compare BEFORE    key figures side by side (BEFORE = the reference run)
  analyze.py RUN --no-jfr            CSV figures only (fast)
  analyze.py RUN --top 15 --package org.opensilex.

JFR files are streamed through `jfr print` (the JDK recorded in run.txt, else PATH): no text dump on disk.
Allocation figures are JFR estimates (jdk.ObjectAllocationSample weights); CPU shares count jdk.ExecutionSample events.
Runs recorded before profile.sh (no run.txt, no phases.csv) are read as a single phase.
"""
import argparse
import csv
import glob
import os
import re
import shutil
import subprocess
from collections import Counter
from datetime import datetime

ALLOC_UNITS = {"byte": 1, "bytes": 1, "kB": 1024, "MB": 1024 ** 2, "GB": 1024 ** 3}
MEM_UNITS = {"B": 1 / 1024 ** 2, "KiB": 1 / 1024, "kB": 1 / 1024, "MiB": 1, "MB": 1, "GiB": 1024, "GB": 1024}
FRAME_RE = re.compile(r"^\s+([\w$.<>]+)\(")
ENDPOINT_RE = re.compile(r"\.api\.[\w.]*API\.\w+$|API\.\w+$")
FIGURES = [  # (key, label) in display order
    ("duration_s", "duration (s)"), ("rss_start", "RSS start (MB)"), ("rss_peak", "RSS peak (MB)"),
    ("rss_end", "RSS end (MB)"), ("heap_used_peak", "heap used peak (MB)"),
    ("heap_commit_peak", "heap reserved peak (MB)"), ("meta_start", "metaspace start (MB)"),
    ("meta_peak", "metaspace peak (MB)"), ("classes_loaded", "classes loaded"),
    ("classes_unloaded", "classes unloaded"), ("young_gc", "young GCs"), ("full_gc", "full GCs"),
    ("gc_time_s", "GC time (s)"), ("threads_peak", "threads peak"), ("cpu_mean", "CPU mean (%)"),
    ("cpu_peak", "CPU peak (%)"),
]


def parse_time(text):
    for fmt in ("%Y-%m-%dT%H:%M:%S", "%H:%M:%S"):
        try:
            return datetime.strptime(text.strip(), fmt)
        except ValueError:
            continue
    return None


def read_csv(path):
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    for row in rows:
        row["_t"] = parse_time(row.get("time", ""))
    return [r for r in rows if r["_t"]]


def mebibytes(text):
    """'1.829GiB / 30.5GiB' (docker stats) -> 1872.9"""
    match = re.match(r"\s*([\d.]+)\s*([KMG]i?B|B)", text or "")
    return float(match.group(1)) * MEM_UNITS.get(match.group(2), 1) if match else None


def load_run(path):
    info = {}
    if os.path.exists(os.path.join(path, "run.txt")):
        with open(os.path.join(path, "run.txt"), encoding="utf-8") as handle:
            info = dict(line.rstrip("\n").split("=", 1) for line in handle if "=" in line)
    samples = read_csv(os.path.join(path, "sample.csv"))
    marks = [(r["label"], r["_t"]) for r in read_csv(os.path.join(path, "phases.csv"))]
    if not marks and samples:
        marks = [("run", samples[0]["_t"])]
    windows = [(label, start, marks[i + 1][1] if i + 1 < len(marks) else None)
               for i, (label, start) in enumerate(marks)]
    return {"path": path.rstrip("/"), "info": info, "samples": samples, "windows": windows,
            "containers": read_csv(os.path.join(path, "containers.csv"))}


def in_window(rows, start, end):
    return [r for r in rows if r["_t"] >= start and (end is None or r["_t"] < end)]


def figures(samples):
    """Process and JVM figures of a list of sample.csv rows."""
    if not samples:
        return {}

    def column(name):
        return [float(r[name]) for r in samples if r.get(name) not in (None, "")]

    def delta(name):
        return float(samples[-1][name]) - float(samples[0][name])

    rss, meta, cpu = column("rss_mb"), column("meta_used_mb"), column("cpu_pct")
    return {"duration_s": (samples[-1]["_t"] - samples[0]["_t"]).total_seconds(), "rss_start": rss[0],
            "rss_peak": max(rss), "rss_end": rss[-1], "heap_used_peak": max(column("heap_used_mb")),
            "heap_commit_peak": max(column("heap_commit_mb")), "meta_start": meta[0], "meta_peak": max(meta),
            "classes_loaded": delta("classes_loaded"), "classes_unloaded": delta("classes_unloaded"),
            "young_gc": delta("ygc"), "full_gc": delta("fgc"), "gc_time_s": delta("gct_s"),
            "threads_peak": max(column("threads")), "cpu_mean": sum(cpu) / len(cpu), "cpu_peak": max(cpu)}


def container_gains(rows):
    """{container: (start MiB, peak MiB, gain MiB)}: compare gains between runs, not absolute sizes."""
    by_name = {}
    for row in rows:
        value = mebibytes(row.get("mem", ""))
        if value is not None:
            by_name.setdefault(row["name"], []).append(value)
    return {name: (values[0], max(values), values[-1] - values[0]) for name, values in sorted(by_name.items())}


def jfr_events(lines, weighted):
    """(weight, allocated class, frames) for each event of `jfr print` output."""
    current, in_stack = None, False
    for line in lines:
        if re.match(r"^jdk\.\w+ \{", line):
            if current and current[0]:
                yield tuple(current)
            current, in_stack = [None if weighted else 1.0, None, []], False
            continue
        stripped = line.strip()
        if current is None:
            continue
        if weighted and stripped.startswith("weight ="):
            amount, unit = stripped.split("=", 1)[1].split()[:2]
            current[0] = float(amount.replace(",", "")) * ALLOC_UNITS.get(unit, 1)
        elif stripped.startswith("objectClass ="):
            current[1] = stripped.split("=", 1)[1].strip().split(" (")[0]
        elif stripped.startswith("stackTrace = ["):
            in_stack = True
        elif in_stack and stripped.startswith("]"):
            in_stack = False
        elif in_stack and FRAME_RE.match(line):
            current[2].append(FRAME_RE.match(line).group(1))
    if current and current[0]:
        yield tuple(current)


def jfr_profile(jfr, recording, event, package):
    """(total weight, {view: Counter}) for one JFR event type; views: endpoint, app frame, leaf frame, class."""
    tops = {view: Counter() for view in ("endpoint", "app", "leaf", "class")}
    total = 0.0
    command = [jfr, "-J-Duser.language=en", "-J-Duser.country=US", "print", "--events", event,
               "--stack-depth", "64", recording]
    with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True,
                          errors="replace") as process:
        for weight, allocated, frames in jfr_events(process.stdout, event.endswith("AllocationSample")):
            total += weight
            app = [f for f in frames if f.startswith(package)]
            tops["leaf"][frames[0] if frames else "?"] += weight
            tops["app"][app[0] if app else f"(outside {package}*)"] += weight
            tops["endpoint"][next((f for f in app if ENDPOINT_RE.search(f)), "(no endpoint in the stack)")] += weight
            if allocated:
                tops["class"][allocated] += weight
    return total, tops


def jfr_tool(run):
    candidate = os.path.join(run["info"].get("jdk", ""), "jfr")
    return candidate if os.access(candidate, os.X_OK) else shutil.which("jfr")


def print_top(title, counter, total, count, gigabytes=False):
    print(f"\n{title}:\n")
    if not total:
        print("- none")
        return
    for key, weight in counter.most_common(count):
        size = f" {weight / 1024 ** 3:7.2f} GB" if gigabytes else ""
        print(f"- {weight / total * 100:5.1f} %{size}  `{key}`")


def print_jfr(run, args):
    jfr = jfr_tool(run)
    if not jfr:
        print("\n`jfr` not found (JDK bin): JFR section skipped")
        return
    phases = sorted(glob.glob(os.path.join(run["path"], "jfr", "*.jfr")))
    whole = os.path.join(run["path"], "run.jfr")
    for recording in phases or ([whole] if os.path.exists(whole) else []):
        print(f"\n## JFR `{os.path.relpath(recording, run['path'])}`")
        total, tops = jfr_profile(jfr, recording, "jdk.ObjectAllocationSample", args.package)
        print(f"\nAllocated (JFR estimate): {total / 1024 ** 3:.2f} GB")
        for view, title in (("endpoint", "by REST endpoint"), ("app", f"by first {args.package}* frame"),
                            ("class", "by allocated class")):
            print_top(f"Allocation {title}", tops[view], total, args.top, gigabytes=True)
        total, tops = jfr_profile(jfr, recording, "jdk.ExecutionSample", args.package)
        for view, title in (("app", f"by first {args.package}* frame"), ("leaf", "by leaf frame")):
            print_top(f"CPU samples {title} ({int(total)} samples)", tops[view], total, args.top)


def number(value):
    if value is None:
        return "-"
    return f"{value:.1f}" if abs(value) < 100 and value != int(value) else f"{value:.0f}"


def print_run(run, args):
    info = run["info"]
    print(f"# Profile `{os.path.basename(run['path'])}`\n")
    for key in ("main", "checkout", "process_started", "started", "stopped"):
        if info.get(key):
            print(f"- {key}: {info[key]}")
    if "org.opensilex.dev." in info.get("main", ""):
        print("- **dev launcher**: DEBUG logging forced (DevModule), logging costs included in every figure")
    columns = [(label, figures(in_window(run["samples"], start, end)),
                container_gains(in_window(run["containers"], start, end))) for label, start, end in run["windows"]]
    if len(columns) > 1:
        columns.append(("whole run", figures(run["samples"]), container_gains(run["containers"])))
    print("\n| figure | " + " | ".join(c[0] for c in columns) + " |")
    print("|---|" + "---|" * len(columns))
    for key, label in FIGURES:
        print(f"| {label} | " + " | ".join(number(c[1].get(key)) for c in columns) + " |")
    for name in sorted({name for c in columns for name in c[2]}):
        gains = [number(c[2][name][2]) if name in c[2] else "-" for c in columns]
        print(f"| {name} gain (MiB) | " + " | ".join(gains) + " |")
    if not args.no_jfr:
        print_jfr(run, args)


def print_compare(after, before, args):
    print(f"# Compare `{os.path.basename(after['path'])}` with `{os.path.basename(before['path'])}` (reference)\n")
    new_figures, old_figures = figures(after["samples"]), figures(before["samples"])
    print("| figure | reference | this run | change |\n|---|---|---|---|")
    for key, label in FIGURES:
        old, new = old_figures.get(key), new_figures.get(key)
        change = f"{(new - old) / old * 100:+.0f} %" if old and new is not None else "-"
        print(f"| {label} | {number(old)} | {number(new)} | {change} |")
    new_gains, old_gains = container_gains(after["containers"]), container_gains(before["containers"])
    for name in sorted(set(new_gains) | set(old_gains)):
        old = old_gains[name][2] if name in old_gains else None
        new = new_gains[name][2] if name in new_gains else None
        print(f"| {name} gain (MiB) | {number(old)} | {number(new)} | |")
    if args.no_jfr:
        return
    for run, title in ((before, "reference"), (after, "this run")):
        recording, jfr = os.path.join(run["path"], "run.jfr"), jfr_tool(run)
        if jfr and os.path.exists(recording):
            total, tops = jfr_profile(jfr, recording, "jdk.ObjectAllocationSample", args.package)
            print_top(f"Allocation, {title}: {total / 1024 ** 3:.2f} GB, by first {args.package}* frame",
                      tops["app"], total, args.top, gigabytes=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("run", help="run directory written by profile.sh")
    parser.add_argument("--compare", metavar="BEFORE", help="reference run to compare with")
    parser.add_argument("--no-jfr", action="store_true", help="skip the JFR analysis")
    parser.add_argument("--top", type=int, default=10, help="lines per JFR list (default 10)")
    parser.add_argument("--package", default="org.opensilex.", help="prefix of the application frames")
    args = parser.parse_args()
    run = load_run(args.run)
    if not run["samples"]:
        parser.error(f"no sample.csv rows in {args.run}")
    if args.compare:
        print_compare(run, load_run(args.compare), args)
    else:
        print_run(run, args)


if __name__ == "__main__":
    main()
