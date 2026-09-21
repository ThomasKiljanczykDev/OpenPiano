#!/usr/bin/env python3
"""Validates fastlane/metadata/android against Play Store limits. Stdlib only."""

import argparse
import struct
import sys
from pathlib import Path

METADATA = Path(__file__).resolve().parent / "metadata" / "android"
LOCALES = ["en-US", "de-DE", "es-ES", "fr-FR", "pl-PL", "pt-BR"]
TEXT_LIMITS = {"title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000}
CHANGELOG_LIMIT = 500
SCREENSHOTS_MIN, SCREENSHOTS_MAX = 2, 8
SCREENSHOT_SIDE_MIN, SCREENSHOT_SIDE_MAX = 320, 3840


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as file:
        header = file.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path} is not a PNG")
    return struct.unpack(">II", header[16:24])


def validate(expect_images: bool, version_code: int | None) -> list[str]:
    errors = []
    for locale in LOCALES:
        root = METADATA / locale
        for name, limit in TEXT_LIMITS.items():
            path = root / name
            if not path.is_file():
                errors.append(f"{locale}: missing {name}")
                continue
            length = len(path.read_text(encoding="utf-8").strip())
            if length == 0 or length > limit:
                errors.append(f"{locale}/{name}: {length} chars, limit {limit}")
        for changelog in sorted((root / "changelogs").glob("*.txt")):
            length = len(changelog.read_text(encoding="utf-8").strip())
            if length > CHANGELOG_LIMIT:
                errors.append(f"{locale}/changelogs/{changelog.name}: {length} chars, limit {CHANGELOG_LIMIT}")
        if version_code is not None and not (root / "changelogs" / f"{version_code}.txt").is_file():
            errors.append(f"{locale}: missing changelogs/{version_code}.txt")
        if expect_images:
            screenshots = sorted((root / "images" / "phoneScreenshots").glob("*.png"))
            if not SCREENSHOTS_MIN <= len(screenshots) <= SCREENSHOTS_MAX:
                errors.append(f"{locale}: {len(screenshots)} phone screenshots, need {SCREENSHOTS_MIN}-{SCREENSHOTS_MAX}")
            for screenshot in screenshots:
                width, height = png_size(screenshot)
                long_side, short_side = max(width, height), min(width, height)
                if short_side < SCREENSHOT_SIDE_MIN or long_side > SCREENSHOT_SIDE_MAX or long_side > 2 * short_side:
                    errors.append(f"{screenshot.relative_to(METADATA)}: {width}x{height} outside Play limits")
    if expect_images:
        for name, size in {"featureGraphic.png": (1024, 500), "icon.png": (512, 512)}.items():
            path = METADATA / "en-US" / "images" / name
            if not path.is_file():
                errors.append(f"en-US: missing images/{name}")
            elif png_size(path) != size:
                errors.append(f"en-US/images/{name}: {png_size(path)}, expected {size}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--expect-images", action="store_true")
    parser.add_argument("--version-code", type=int)
    args = parser.parse_args()
    errors = validate(args.expect_images, args.version_code)
    for error in errors:
        print(f"::error::{error}")
    print(f"{len(LOCALES)} locales checked, {len(errors)} error(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
