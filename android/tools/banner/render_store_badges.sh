#!/usr/bin/env bash
set -euo pipefail

# Desaturated placeholders for the README; swap in the unmodified upstream badges once each
# store listing is live.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
OUT_DIR="${REPO_DIR}/docs/images"

PLAY_URL="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
FDROID_URL="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"

command -v rsvg-convert >/dev/null || { echo "rsvg-convert not found (brew install librsvg)" >&2; exit 1; }

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

render() {
  local url="$1" out="$2"
  curl -sSfL -o "${tmp}/in.png" "${url}"

  # Both upstream badges are 646x250; rsvg needs the intrinsic size stated explicitly.
  base64 -i "${tmp}/in.png" | tr -d '\n' > "${tmp}/in.b64"
  {
    printf '%s' '<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="646" height="250" viewBox="0 0 646 250">'
    printf '%s' '<filter id="g"><feColorMatrix type="saturate" values="0"/></filter>'
    printf '%s' '<image x="0" y="0" width="646" height="250" filter="url(#g)" opacity="0.6" xlink:href="data:image/png;base64,'
    cat "${tmp}/in.b64"
    printf '%s' '"/></svg>'
  } > "${tmp}/out.svg"

  rsvg-convert -w 646 -h 250 "${tmp}/out.svg" -o "${out}"
  echo "wrote ${out}"
}

mkdir -p "${OUT_DIR}"
render "${PLAY_URL}" "${OUT_DIR}/google-play-badge-unavailable.png"
render "${FDROID_URL}" "${OUT_DIR}/fdroid-badge-unavailable.png"
