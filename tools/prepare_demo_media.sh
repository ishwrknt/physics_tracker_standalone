#!/usr/bin/env bash
# Build Android-compatible teaching assets without modifying supplied originals.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/src/main/assets/demos/supplied
for source in demo_videos/*; do
    name=$(basename "$source")
    case "$name" in
        *.jpg|*.mp4) cp "$source" "app/src/main/assets/demos/supplied/$name" ;;
        *.mov)
            codec=$(ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of csv=p=0 "$source")
            output="app/src/main/assets/demos/supplied/${name%.mov}.mp4"
            [[ "$name" != Pendulum.mov ]] || output="app/src/main/assets/demos/supplied/classic_pendulum.mp4"
            if [[ "$codec" == h264 ]]; then
                ffmpeg -v error -y -i "$source" -map 0:v:0 -an -c:v copy -movflags +faststart "$output"
            else
                # Preserve frame count and presentation spacing, not decoded pixel values.
                # These are labelled converted teaching clips, never original intensity references.
                ffmpeg -v error -y -i "$source" -map 0:v:0 -an -c:v libx264 -profile:v high -crf 12 -pix_fmt yuv420p -fps_mode passthrough -enc_time_base 1:1000000 -video_track_timescale 1000000 -movflags +faststart "$output"
            fi ;;
        *) echo "Unsupported demo: $source" >&2; exit 1 ;;
    esac
done
