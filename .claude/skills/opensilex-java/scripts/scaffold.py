#!/usr/bin/env python3
"""Instantiate the compile-checked templates of assets/templates for a new OpenSILEX concept.

Example:
  scaffold.py --concept SensorKit --module opensilex-core --prefix skit --parts model,filter,dao,dto,api,test
  scaffold.py --concept SensorKit --dry-run                    # print what would be written
  scaffold.py --concept SensorKit --out-root /tmp/scaffold     # write under another root (mirrors <module>/src/...)

Parts: model, filter, dao, logic, dto (3 DTOs), api, test, migration. Default: model,filter,dao,dto,api,test.
Every generated file starts with the current OpenSILEX header (git user.email as first contact).
The templates compile as they are (checked with JDK 17); the generated code still needs the ontology term, the
credential label keys and a review: see references/templates.md.
"""
import argparse
import datetime
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SKILL = os.path.dirname(HERE)
TEMPLATES = os.path.join(SKILL, "assets", "templates")
REPO = subprocess.run(["git", "-C", HERE, "rev-parse", "--show-toplevel"], capture_output=True, text=True).stdout.strip() \
    or os.path.abspath(os.path.join(SKILL, "..", "..", ".."))

# part -> list of (template path relative to assets/templates, sub-package or None, kind)
PARTS = {
    "model": [("dal/WidgetModel.java", "dal", "main")],
    "filter": [("dal/WidgetSearchFilter.java", "dal", "main")],
    "dao": [("dal/WidgetDAO.java", "dal", "main")],
    "logic": [("bll/WidgetLogic.java", "bll", "main")],
    "dto": [("api/WidgetDTO.java", "api", "main"), ("api/WidgetCreationDTO.java", "api", "main"),
            ("api/WidgetUpdateDTO.java", "api", "main")],
    "api": [("api/WidgetAPI.java", "api", "main")],
    "test": [("test/WidgetAPITest.java", "api", "test")],
    "migration": [("migration/WidgetMigration.java", None, "migration")],
}
DEFAULT_PARTS = "model,filter,dao,dto,api,test"
TOKEN = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"', re.S)
WORDS = re.compile(r"[A-Z][a-z0-9]*|[a-z0-9]+")


def words(pascal):
    return WORDS.findall(pascal)


class Names:
    def __init__(self, concept, plural):
        self.pascal = concept
        self.pascal_plural = plural
        w, wp = words(concept), words(plural)
        self.kebab = "-".join(x.lower() for x in w)
        self.kebab_plural = "-".join(x.lower() for x in wp)
        self.snake_plural = "_".join(x.lower() for x in wp)
        self.upper = "_".join(x.upper() for x in w)
        self.lower_camel = concept[0].lower() + concept[1:]
        self.lower_camel_plural = plural[0].lower() + plural[1:]
        self.human = " ".join(x.lower() for x in w)
        self.human_plural = " ".join(x.lower() for x in wp)
        self.human_cap = self.human[0].upper() + self.human[1:]
        self.human_plural_cap = self.human_plural[0].upper() + self.human_plural[1:]
        self.title_plural = " ".join(x.capitalize() for x in wp)


def instantiate(text, n, opts):
    # 1. exact snippets of the templates
    exact = [
        ("org.opensilex.core.widget", opts.package),
        ("org.opensilex.core.ontology.Oeso", opts.ontology),
        ("/core/widgets", opts.route),
        ("credential-groups.widgets", "credential-groups." + n.kebab_plural),
        ("http://opensilex.dev/widgets#my-widget", "http://opensilex.dev/%s#my-%s" % (n.kebab_plural, n.kebab)),
        ('"widget-modification"', '"%s-modification"' % n.kebab),
        ('"widget-delete"', '"%s-delete"' % n.kebab),
        ('GRAPH = "widget"', 'GRAPH = "%s"' % opts.graph),
        ('prefix = "wdg"', 'prefix = "%s"' % opts.prefix),
        ('resource = "Widget"', 'resource = "%s"' % n.pascal),
        ('CREDENTIAL_WIDGET_GROUP_ID = "Widgets"', 'CREDENTIAL_WIDGET_GROUP_ID = "%s"' % n.title_plural),
        ("WidgetMigration", opts.migration_name),
    ]
    for old, new in exact:
        text = text.replace(old, new)
    simple_ontology = opts.ontology.rsplit(".", 1)[-1]
    if simple_ontology != "Oeso":
        text = re.sub(r"\bOeso\b", simple_ontology, text)

    # 2. generic pass: identifiers in code, human words in strings/comments
    pat = re.compile(r"Widgets|Widget|WIDGET|widgets|widget")

    def sub(seg, human_ctx):
        def repl(m):
            s = m.group(0)
            before = seg[m.start() - 1] if m.start() > 0 else " "
            after = seg[m.end()] if m.end() < len(seg) else " "
            embedded = before.isalnum() or before == "_" or after.isalnum() or after == "_"
            as_word = human_ctx and not embedded
            if s == "WIDGET":
                return n.upper
            if s == "Widgets":
                return n.human_plural_cap if as_word else n.pascal_plural
            if s == "Widget":
                return n.human_cap if as_word else n.pascal
            if s == "widgets":
                return n.human_plural if as_word else n.lower_camel_plural
            return n.human if as_word else n.lower_camel
        return pat.sub(repl, seg)

    out, last = [], 0
    for m in TOKEN.finditer(text):
        out.append(sub(text[last:m.start()], False))
        out.append(sub(m.group(0), True))
        last = m.end()
    out.append(sub(text[last:], False))
    return "".join(out)


def header(file_name, email):
    now = datetime.datetime.now()
    return (
        "/*\n"
        " * *****************************************************************************\n"
        " *                         %s\n"
        " * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html\n"
        " * Copyright © INRAE %d.\n"
        " * Last Modification: %s\n"
        " * Contact: %s, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,\n"
        " * *****************************************************************************\n"
        " */\n\n"
    ) % (file_name, now.year, now.strftime("%d/%m/%Y %H:%M"), email)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--concept", required=True, help="PascalCase singular name, e.g. SensorKit")
    ap.add_argument("--module", default="opensilex-core")
    ap.add_argument("--package", help="base package (default org.opensilex.<module-without-opensilex->.<concept lowercase>)")
    ap.add_argument("--plural", help="PascalCase plural (default: concept + 's')")
    ap.add_argument("--prefix", help="URI prefix of generated instances, 2-5 lowercase letters (default: first 3 letters)")
    ap.add_argument("--graph", help="named graph (default: kebab-case concept)")
    ap.add_argument("--route", help="REST path (default: /<module-short>/<snake_case plural>)")
    ap.add_argument("--ontology", default="org.opensilex.core.ontology.Oeso", help="vocabulary class used by @SPARQLResource")
    ap.add_argument("--migration-name", help="class name of the migration (default <Concept>Migration)")
    ap.add_argument("--parts", default=DEFAULT_PARTS)
    ap.add_argument("--email", help="first contact of the header (default: git config user.email)")
    ap.add_argument("--no-header", action="store_true")
    ap.add_argument("--out-root", help="write under this directory instead of the repository")
    ap.add_argument("--force", action="store_true", help="overwrite existing files")
    ap.add_argument("--dry-run", action="store_true")
    opts = ap.parse_args()

    if not re.fullmatch(r"[A-Z][A-Za-z0-9]*", opts.concept):
        sys.exit("--concept must be PascalCase (letters and digits), got %r" % opts.concept)
    short = opts.module[len("opensilex-"):] if opts.module.startswith("opensilex-") else opts.module
    plural = opts.plural or opts.concept + "s"
    n = Names(opts.concept, plural)
    opts.package = opts.package or "org.opensilex.%s.%s" % (short.replace("-", ""), opts.concept.lower())
    opts.prefix = opts.prefix or opts.concept[:3].lower()
    if not re.fullmatch(r"[a-z]{2,5}", opts.prefix):
        sys.exit("--prefix must be 2-5 lowercase letters, got %r" % opts.prefix)
    opts.graph = opts.graph or n.kebab
    opts.route = opts.route or "/%s/%s" % (short, n.snake_plural)
    opts.migration_name = opts.migration_name or opts.concept + "Migration"
    email = opts.email or subprocess.run(["git", "-C", REPO, "config", "user.email"], capture_output=True, text=True).stdout.strip() \
        or "first.last@inrae.fr"
    root = opts.out_root or REPO

    parts = [p.strip() for p in opts.parts.split(",") if p.strip()]
    unknown = [p for p in parts if p not in PARTS]
    if unknown:
        sys.exit("unknown part(s): %s (choose among %s)" % (", ".join(unknown), ", ".join(PARTS)))

    plan = []
    for part in parts:
        for rel, sub, kind in PARTS[part]:
            src = os.path.join(TEMPLATES, rel)
            base = os.path.basename(rel).replace("WidgetMigration", opts.migration_name).replace("Widget", n.pascal)
            pkg_path = opts.package.replace(".", "/")
            if kind == "main":
                dst = os.path.join(root, opts.module, "src/main/java", pkg_path, sub, base)
            elif kind == "test":
                dst = os.path.join(root, opts.module, "src/test/java", pkg_path, sub, base)
            else:
                dst = os.path.join(root, "opensilex-migration/src/main/java/org/opensilex/migration", base)
            plan.append((src, dst, base))

    existing = [d for _, d, _ in plan if os.path.exists(d)]
    if existing and not opts.force:
        sys.exit("refusing to overwrite (use --force): \n  " + "\n  ".join(existing))

    for src, dst, base in plan:
        content = instantiate(open(src, encoding="utf-8").read(), n, opts)
        if not opts.no_header:
            content = header(base, email) + content
        print(("would write " if opts.dry_run else "writing     ") + os.path.relpath(dst, root))
        if not opts.dry_run:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "w", encoding="utf-8", newline="\n") as f:
                f.write(content)

    print("\nNext steps:")
    print("  1. Ontology: add the class %s and its properties to the OWL file of the module, and a constant in %s." % (
        n.pascal, opts.ontology.rsplit(".", 1)[-1]))
    print("  2. Credentials: add the i18n keys credential-groups.%s (front language files) if they do not exist." % n.kebab_plural)
    print("  3. Replace the sample fields (description, startDate) by the real ones, in the model, the DTOs (toModel/fromModel) and the test.")
    print("  4. Compile with JDK 17 (scripts/jdk.sh mvn -o -q -pl %s compile -DskipFrontBuild), run the generated test, then scripts/lint.py." % opts.module)


if __name__ == "__main__":
    main()
