#!/usr/bin/env bash
set -euo pipefail
# Synthetic reference footage only: horizontal gradient with varying brightness.
mkdir -p app/src/main/assets/demos
ffmpeg -nostdin -y -f lavfi -i "nullsrc=s=320x240:r=24:d=4,geq=lum='32+160*(X/W)*(0.55+0.45*sin(2*PI*N/48))':cb=128:cr=128" \
  -c:v libx264 -pix_fmt yuv420p -crf 16 -an -color_range tv -colorspace bt709 \
  -color_primaries bt709 -color_trc bt709 -movflags +faststart app/src/main/assets/demos/light-pulse.mp4
