#!/bin/sh
# Downloads the 30 Salamander Grand Piano V3 samples (velocity layer 13) used by
# build_sf3.py.  ~46 MB.  Source: CC-BY 3.0, see LICENSE.txt.
set -e
cd "$(dirname "$0")"
mkdir -p samples
BASE=https://raw.githubusercontent.com/sfzinstruments/SalamanderGrandPiano/master/Samples
for n in A0 C1 D%231 F%231 A1 C2 D%232 F%232 A2 C3 D%233 F%233 A3 C4 D%234 F%234 \
         A4 C5 D%235 F%235 A5 C6 D%236 F%236 A6 C7 D%237 F%237 A7 C8; do
  out=$(printf '%s' "$n" | sed 's/%23/S/')
  curl -sfL -o "samples/${out}v13.flac" "$BASE/${n}v13.flac"
done
echo "downloaded $(ls samples | wc -l) samples"
