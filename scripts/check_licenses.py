#!/usr/bin/env python3
"""Dependency license gate for the Zapl Lab Android client.

Reads the JSON reports of gradle-license-plugin (tasks license<Variant>ReportReport), the policy
in third_party/license-policy.json and THIRD_PARTY_NOTICES.md, and fails when:

- a packaged Maven dependency has no license, an unrecognised license or a license outside the
  allowlist;
- a dependency (group:artifact and version) is missing from THIRD_PARTY_NOTICES.md;
- a native submodule or desugar_jdk_libs changed since its licenses were last reviewed;
- with --release: any reviewed component is still marked unresolved.

Usage:
  scripts/check_licenses.py [--release] [--markdown] REPORT.json [REPORT.json ...]

--markdown prints the Maven inventory table used in THIRD_PARTY_NOTICES.md and exits.
Uses only the Python standard library.
"""
import json
import os
import re
import subprocess
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
POLICY = ROOT / "third_party" / "license-policy.json"
NOTICES = ROOT / "THIRD_PARTY_NOTICES.md"
VERSIONS = ROOT / "V2rayNG" / "gradle" / "libs.versions.toml"


def load_dependencies(paths):
    deps = {}
    for path in paths:
        for entry in json.loads(Path(path).read_text(encoding="utf-8")):
            deps[entry["dependency"]] = entry
    return [deps[key] for key in sorted(deps)]


def to_spdx(license_entry, policy):
    name = re.sub(r"\s+", " ", (license_entry.get("license") or "").strip().lower())
    url = re.sub(r"^https?://", "", (license_entry.get("license_url") or "").strip().lower()).rstrip("/")
    return policy["license_names"].get(name) or policy["license_urls"].get(url)


def gitlink(path):
    out = subprocess.run(
        ["git", "-C", str(ROOT), "ls-tree", "HEAD", path],
        capture_output=True, text=True, check=True,
    ).stdout.split()
    return out[2] if len(out) >= 3 else None


def markdown_table(deps, policy):
    print("| Artifact | Version | License |")
    print("|---|---|---|")
    for dep in deps:
        group, artifact, version = dep["dependency"].split(":")
        ids = sorted({to_spdx(lic, policy) or lic.get("license") for lic in dep.get("licenses") or []})
        print(f"| `{group}:{artifact}` | {version} | {' AND '.join(ids)} |")


def main(argv):
    release = "--release" in argv
    markdown = "--markdown" in argv
    reports = [arg for arg in argv if not arg.startswith("--")]
    if not reports:
        print(__doc__)
        return 2

    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    deps = load_dependencies(reports)
    if markdown:
        markdown_table(deps, policy)
        return 0

    notices = NOTICES.read_text(encoding="utf-8")
    allowed = set(policy["allowed_licenses"])
    reviewed = policy["reviewed_artifacts"]
    errors, warnings = [], []

    for dep in deps:
        coordinate = dep["dependency"]
        group, artifact, version = coordinate.split(":")
        key = f"{group}:{artifact}"
        licenses = dep.get("licenses") or []
        if not licenses:
            errors.append(f"{coordinate}: no license declared")
        for lic in licenses:
            spdx = to_spdx(lic, policy)
            if spdx is None:
                errors.append(f"{coordinate}: unrecognised license {lic.get('license')!r} ({lic.get('license_url')})")
            elif spdx not in allowed:
                errors.append(f"{coordinate}: license {spdx} is not in the allowlist")
        if not re.search(rf"\|\s*`{re.escape(key)}`\s*\|\s*{re.escape(version)}\s*\|", notices):
            errors.append(f"{coordinate}: not listed with this version in THIRD_PARTY_NOTICES.md")
        if key in reviewed and not reviewed[key]["resolved"]:
            warnings.append(f"{coordinate}: {reviewed[key]['note']}")

    manual = policy["manual_components"]
    for name in ("AndroidLibXrayLite", "hev-socks5-tunnel"):
        actual = gitlink(name)
        if actual != manual[name]["gitlink"]:
            errors.append(
                f"{name}: submodule is at {actual}, licenses were reviewed at {manual[name]['gitlink']}; "
                "re-audit it and update THIRD_PARTY_NOTICES.md and third_party/license-policy.json"
            )
    desugar = tomllib.loads(VERSIONS.read_text(encoding="utf-8"))["versions"]["desugarJdkLibs"]
    if desugar != manual["desugar_jdk_libs"]["version"]:
        errors.append(f"desugar_jdk_libs: version {desugar} was not reviewed")
    for name, component in manual.items():
        if not component.get("resolved", True):
            warnings.append(f"{name}: {component['note']}")

    unresolved = [f"{k}: {v['note']}" for k, v in reviewed.items() if not v["resolved"]]
    unresolved += [f"{k}: {v['note']}" for k, v in manual.items() if not v.get("resolved", True)]
    if release and unresolved:
        errors.extend(f"unresolved before release - {item}" for item in unresolved)

    in_ci = os.environ.get("GITHUB_ACTIONS") == "true"
    for message in warnings:
        print(f"::warning::{message}" if in_ci else f"WARNING {message}")
    for message in errors:
        print(f"::error::{message}" if in_ci else f"ERROR {message}")
    summary = f"checked {len(deps)} dependencies: {len(errors)} errors, {len(warnings)} warnings"
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as out:
            out.write(f"## License audit\n\n{summary}\n\n")
            out.writelines(f"- error: {m}\n" for m in errors)
            out.writelines(f"- warning: {m}\n" for m in warnings)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
