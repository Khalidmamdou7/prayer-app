#!/usr/bin/env python3
"""Builds app/src/main/assets/cities.tsv from GeoNames (CC BY 4.0, https://www.geonames.org).

Usage:
  curl -O https://download.geonames.org/export/dump/cities15000.zip && unzip cities15000.zip
  curl -O https://download.geonames.org/export/dump/admin1CodesASCII.txt
  python3 tools/build_cities.py cities15000.txt admin1CodesASCII.txt app/src/main/assets/cities.tsv

Output columns (tab separated, sorted by population, largest first):
  name, ascii name, arabic name, country code, region, latitude, longitude
"""
import re
import sys

ARABIC = re.compile(r"^[؀-ۿ\s\-'ʼ]+$")
# Letters used by Persian/Urdu but not Arabic: skip names containing them.
NON_ARABIC = re.compile(r"[پچژگکیڈٹںےھۀ]")


def arabic_name(alternates: str) -> str:
    for name in alternates.split(","):
        name = name.strip()
        if name and ARABIC.match(name) and not NON_ARABIC.search(name):
            return name
    return ""


def main(cities_path, admin1_path, out_path):
    regions = {}
    with open(admin1_path, encoding="utf-8") as f:
        for line in f:
            code, name, *_ = line.rstrip("\n").split("\t")
            regions[code] = name

    rows = []
    with open(cities_path, encoding="utf-8") as f:
        for line in f:
            c = line.rstrip("\n").split("\t")
            name, ascii_name, alternates = c[1], c[2], c[3]
            lat, lng, cc, admin1, population = float(c[4]), float(c[5]), c[8], c[10], int(c[14] or 0)
            region = regions.get(f"{cc}.{admin1}", "")
            rows.append((population, [
                name, ascii_name if ascii_name != name else "", arabic_name(alternates), cc,
                region if region != name else "", f"{lat:.4f}", f"{lng:.4f}",
            ]))

    rows.sort(key=lambda r: -r[0])
    with open(out_path, "w", encoding="utf-8") as out:
        for _, cols in rows:
            out.write("\t".join(col.replace("\t", " ") for col in cols) + "\n")


if __name__ == "__main__":
    main(*sys.argv[1:4])
