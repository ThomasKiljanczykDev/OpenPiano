#!/usr/bin/env bash
# Regenerates every launcher-icon raster from tools/icon/icon.svg.
#
# Usage: android/tools/icon/render_icon.sh
# Requires rsvg-convert (brew install librsvg), cwebp (brew install webp),
# and python3 (stdlib only, used to flatten background+foreground for the
# legacy/Play-Store outputs).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
SVG="${SCRIPT_DIR}/icon.svg"
RES_DIR="${ANDROID_DIR}/app/src/main/res"

for bin in rsvg-convert cwebp python3; do
  command -v "${bin}" >/dev/null 2>&1 || {
    echo "error: ${bin} not found on PATH" >&2
    exit 1
  }
done

DENSITIES=(mdpi hdpi xhdpi xxhdpi xxxhdpi)
LAYER_SIZES=(108 162 216 324 432)   # 108dp adaptive-icon layers
LEGACY_SIZES=(48 72 96 144 192)     # 48dp legacy launcher bitmaps

render_layer() {
  local export_id="$1" out_file="$2" size="$3"
  rsvg-convert -i "${export_id}" -w "${size}" -h "${size}" "${SVG}" -o "${out_file}"
}

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

echo "Rendering adaptive-icon layers..."
for i in "${!DENSITIES[@]}"; do
  density="${DENSITIES[$i]}"
  size="${LAYER_SIZES[$i]}"
  dir="${RES_DIR}/mipmap-${density}"
  mkdir -p "${dir}"
  render_layer background "${dir}/ic_launcher_background.png" "${size}"
  render_layer foreground "${dir}/ic_launcher_foreground.png" "${size}"
  render_layer monochrome "${dir}/ic_launcher_monochrome.png" "${size}"
done

echo "Flattening background+foreground for legacy bitmaps..."
python3 "${SCRIPT_DIR}/compose_svg.py" "${SVG}" "${TMP_DIR}/flat_square.svg"
python3 "${SCRIPT_DIR}/compose_svg.py" "${SVG}" "${TMP_DIR}/flat_round.svg" --round

for i in "${!DENSITIES[@]}"; do
  density="${DENSITIES[$i]}"
  size="${LEGACY_SIZES[$i]}"
  dir="${RES_DIR}/mipmap-${density}"
  mkdir -p "${dir}"

  rsvg-convert -w "${size}" -h "${size}" "${TMP_DIR}/flat_square.svg" -o "${TMP_DIR}/${density}_square.png"
  rsvg-convert -w "${size}" -h "${size}" "${TMP_DIR}/flat_round.svg" -o "${TMP_DIR}/${density}_round.png"

  cwebp -quiet -lossless "${TMP_DIR}/${density}_square.png" -o "${dir}/ic_launcher.webp"
  cwebp -quiet -lossless "${TMP_DIR}/${density}_round.png" -o "${dir}/ic_launcher_round.webp"
done

echo "Rendering Play Store master icon (512x512, no mask)..."
rsvg-convert -w 512 -h 512 "${TMP_DIR}/flat_square.svg" -o "${ANDROID_DIR}/app/src/main/ic_launcher-playstore.png"
PLAY_ICON_DIR="${ANDROID_DIR}/../fastlane/metadata/android/en-US/images"
mkdir -p "${PLAY_ICON_DIR}"
cp "${ANDROID_DIR}/app/src/main/ic_launcher-playstore.png" "${PLAY_ICON_DIR}/icon.png"

echo "Done."
