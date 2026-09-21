#!/usr/bin/env bash
# Copies rendered Play screenshots into fastlane/metadata/android/<locale>/images/phoneScreenshots/.
# Run after :tools:gplay-screenshots:updateDebugScreenshotTest.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
REFERENCE="${SCRIPT_DIR}/src/screenshotTestDebug/reference"
METADATA="${REPO_DIR}/fastlane/metadata/android"

renders=()
while IFS= read -r render; do
  renders+=("${render}")
done < <(find "${REFERENCE}" -path '*PlayScreenshotsKt/*.png' 2>/dev/null | sort)
if [ ${#renders[@]} -eq 0 ]; then
  echo "error: no renders under ${REFERENCE}" >&2
  exit 1
fi

for locale_dir in "${METADATA}"/*/; do
  rm -rf "${locale_dir}images/phoneScreenshots"
done

# Screenshot<N><Screen>_<locale>_<hash>_<index>.png
for render in "${renders[@]}"; do
  name="$(basename "${render}" .png)"
  screen="${name%%_*}"
  rest="${name#*_}"
  locale="${rest%%_*}"
  order="${screen#Screenshot}"
  order="${order%%[!0-9]*}"
  target="${METADATA}/${locale}/images/phoneScreenshots"
  if [ ! -d "${METADATA}/${locale}" ]; then
    echo "error: no fastlane locale directory for ${locale}" >&2
    exit 1
  fi
  mkdir -p "${target}"
  cp "${render}" "${target}/${order}_${screen#Screenshot[0-9]}.png"
done

find "${METADATA}" -path '*phoneScreenshots/*.png' | sort
