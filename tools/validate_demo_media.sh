#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
count=0
for source in demo_videos/*; do
    name=$(basename "$source")
    target="app/src/main/assets/demos/supplied/${name%.mov}"
    [[ "$name" != *.mov ]] || target="$target.mp4"
    [[ "$name" != Pendulum.mov ]] || target="app/src/main/assets/demos/supplied/classic_pendulum.mp4"
    [[ -s "$target" ]]
    if [[ "$name" == *.jpg ]]; then
        cmp "$source" "$target"
    else
        original=$(ffprobe -v error -select_streams v:0 -count_frames -show_entries stream=width,height,nb_read_frames -of csv=p=0 "$source")
        packaged=$(ffprobe -v error -select_streams v:0 -count_frames -show_entries stream=width,height,nb_read_frames -of csv=p=0 "$target")
        [[ "$original" == "$packaged" ]] || { echo "Dimensions/frame count mismatch: $name"; exit 1; }
        # Compare decoded presentation spacing (not decoder packet order), allowing 0.1 ms quantization.
        paste <(ffprobe -v error -select_streams v:0 -show_frames -show_entries frame=best_effort_timestamp_time -of csv=p=0 "$source" | sed '/^$/d' | cut -d, -f1) \
              <(ffprobe -v error -select_streams v:0 -show_frames -show_entries frame=best_effort_timestamp_time -of csv=p=0 "$target" | sed '/^$/d' | cut -d, -f1) |
            awk 'NR==1 {a=$1;b=$2} {d=($1-a)-($2-b); if (NF!=2 || d>0.0001 || d< -0.0001) exit 1}' || { echo "Timing mismatch: $name"; exit 1; }
        ffmpeg -v error -i "$target" -map 0:v:0 -f null -
    fi
    count=$((count+1))
done
echo "Verified $count supplied demos: dimensions, frame count, presentation spacing and full host decoding."
