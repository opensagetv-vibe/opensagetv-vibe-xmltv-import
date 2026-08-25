#!/usr/bin/env python3
"""Report XMLTV identity sources and field coverage without retaining guide data."""
import collections
import pathlib
import sys
import xml.etree.ElementTree as ET


def local(tag):
    return tag.rsplit("}", 1)[-1]


def audit(path):
    tags = collections.Counter()
    episode_systems = collections.Counter()
    series_systems = collections.Counter()
    identity_conflicts = collections.defaultdict(set)
    programmes = 0
    for _, elem in ET.iterparse(path, events=("end",)):
        name = local(elem.tag)
        tags[name] += 1
        if name != "programme":
            continue
        programmes += 1
        values = {}
        selected = None
        series = None
        fingerprint = []
        for child in elem:
            child_name = local(child.tag)
            value = (child.text or "").strip()
            if child_name in ("title", "sub-title", "date"):
                fingerprint.append(child_name + "=" + value.casefold())
            elif child_name == "episode-num":
                system = child.get("system", "").casefold()
                episode_systems[system or "(none)"] += 1
                values[system] = value
                if system in ("tms", "dd_progid", "plutod", "pluto", "xmltv:", "placeholder"):
                    selected = system + ":" + value
            elif child_name == "series-id":
                system = child.get("system", "").casefold()
                series_systems[system or "(none)"] += 1
                series = system + ":" + value
        identity = selected or series
        if identity:
            identity_conflicts[identity].add("|".join(fingerprint))
        elem.clear()
    conflicts = sum(1 for variants in identity_conflicts.values() if len(variants) > 1)
    print(f"FILE\t{path.name}\tprogrammes={programmes}\tidentity_keys={len(identity_conflicts)}\tconflicting_keys={conflicts}")
    print("EPISODE_SYSTEMS\t" + ", ".join(f"{k}={v}" for k, v in episode_systems.most_common()))
    print("SERIES_SYSTEMS\t" + ", ".join(f"{k}={v}" for k, v in series_systems.most_common()))
    useful = ("sub-title", "desc", "category", "credits", "date", "icon", "rating", "star-rating",
              "video", "audio", "subtitles", "new", "live", "premiere", "previously-shown", "length",
              "url", "country", "language")
    print("FIELDS\t" + ", ".join(f"{k}={tags[k]}" for k in useful if tags[k]))


def main():
    root = pathlib.Path(sys.argv[1])
    files = sorted(p for p in root.iterdir() if p.is_file() and
                   (p.suffix in (".xml", ".xmltv") or p.name.endswith(".xml.old")))
    if not files:
        raise SystemExit("no XMLTV files found")
    for path in files:
        audit(path)


if __name__ == "__main__":
    main()
