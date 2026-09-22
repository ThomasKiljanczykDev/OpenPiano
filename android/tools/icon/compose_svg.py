#!/usr/bin/env python3
"""Builds a flattened (background+foreground) SVG from icon.svg.

Used by render_icon.sh to produce the legacy square/round launcher bitmaps
and the Play Store master icon, none of which use the adaptive-icon layer
split. Stdlib-only (xml.etree) so the render pipeline needs nothing beyond
rsvg-convert and cwebp.
"""
from __future__ import annotations

import argparse
import xml.etree.ElementTree as ET

SVG_NS = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG_NS)


def find_by_id(root: ET.Element, target_id: str) -> ET.Element:
    for el in root.iter():
        if el.get("id") == target_id:
            return el
    raise SystemExit(f"error: no element with id={target_id!r} found")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source_svg")
    parser.add_argument("output_svg")
    parser.add_argument("--round", action="store_true", help="clip to a centered circle")
    args = parser.parse_args()

    tree = ET.parse(args.source_svg)
    root = tree.getroot()
    background = find_by_id(root, "background")
    foreground = find_by_id(root, "foreground")

    out = ET.Element(f"{{{SVG_NS}}}svg", {
        "viewBox": "0 0 108 108",
        "width": "108",
        "height": "108",
    })

    if args.round:
        defs = ET.SubElement(out, f"{{{SVG_NS}}}defs")
        clip = ET.SubElement(defs, f"{{{SVG_NS}}}clipPath", {"id": "roundMask"})
        ET.SubElement(clip, f"{{{SVG_NS}}}circle", {"cx": "54", "cy": "54", "r": "54"})
        content = ET.SubElement(out, f"{{{SVG_NS}}}g", {"clip-path": "url(#roundMask)"})
    else:
        content = out

    content.append(background)
    content.append(foreground)

    ET.ElementTree(out).write(args.output_svg, encoding="unicode", xml_declaration=True)


if __name__ == "__main__":
    main()
