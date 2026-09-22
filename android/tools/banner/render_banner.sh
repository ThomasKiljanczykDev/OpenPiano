#!/usr/bin/env bash
# Regenerates the banner, Play feature graphic and GitHub social preview from banner.svg.
#
# Usage: android/tools/banner/render_banner.sh
# Requires rsvg-convert (brew install librsvg).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
SVG="${SCRIPT_DIR}/banner.svg"

OUT="${REPO_DIR}/docs/images/OpenPiano-banner.png"
PLAY_OUT="${REPO_DIR}/fastlane/metadata/android/en-US/images/featureGraphic.png"
SOCIAL_OUT="${REPO_DIR}/docs/images/OpenPiano-social-preview.png"

command -v rsvg-convert >/dev/null 2>&1 || {
  echo "error: rsvg-convert not found on PATH" >&2
  exit 1
}

mkdir -p "$(dirname "${OUT}")" "$(dirname "${PLAY_OUT}")"

echo "Rendering banner..."
rsvg-convert -w 1024 -h 500 "${SVG}" -o "${OUT}"
rsvg-convert -w 1024 -h 500 --background-color=white "${SVG}" -o "${PLAY_OUT}"

# GitHub's social preview is 1280x640; centre the 1024x500 artwork rather than stretch it.
rsvg-convert -w 1280 -h 625 \
  --page-width 1280 --page-height 640 --left 0 --top 7 \
  --background-color='#141414' "${SVG}" -o "${SOCIAL_OUT}"

echo "Done."
