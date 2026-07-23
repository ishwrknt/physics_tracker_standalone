#!/usr/bin/env python3
"""Deterministic synthetic validation matrix for the offline tracker candidate."""

import json
import math
import pathlib
import random

import cv2
import numpy as np

from benchmark_tracking import metrics, run_tracker


WIDTH, HEIGHT, COUNT = 320, 180, 150


def trajectory(name, index):
    t = index / 60.0
    if name == "stationary": return 70.0, 80.0
    if name == "constant_velocity": return 35.0 + index * 1.4, 72.0
    if name == "constant_acceleration": return 30.0 + 0.008 * index * index, 55.0 + 0.3 * index
    if name == "circular_motion": return 150.0 + 52 * math.cos(t * 2.5), 88.0 + 52 * math.sin(t * 2.5)
    if name == "large_displacement": return 30.0 + (index * 7.5) % 250, 85.0
    if name == "crossing_objects": return 35.0 + index * 1.55, 65.0
    return 35.0 + index * 1.55, 70.0 + 18.0 * math.sin(t * 2.0)


def make_case(name):
    rng = random.Random(9173)
    frames, reference = [], []
    for index in range(COUNT):
        camera_x = int(round(3.0 * math.sin(index / 8.0))) if name in ("camera_vibration", "global_camera_motion") else 0
        camera_y = int(round(index * 0.10)) if name == "global_camera_motion" else 0
        frame = np.full((HEIGHT, WIDTH, 3), 42, np.uint8)
        for x in range(-20, WIDTH + 20, 32):
            cv2.line(frame, (x + camera_x, 0), (x + camera_x, HEIGHT), (58, 58, 58), 1)
        for y in range(-20, HEIGHT + 20, 28):
            cv2.line(frame, (0, y + camera_y), (WIDTH, y + camera_y), (58, 58, 58), 1)
        x, y = trajectory(name, index)
        x += camera_x
        y += camera_y
        occluded = name in ("temporary_occlusion", "reappearance") and 60 <= index < 76
        radius = int(round(9 + (index / COUNT) * 5)) if name == "scale_rotation" else 10
        color = (35, 45 + (index % 25 if name == "lighting_change" else 0), 238)
        if not occluded and 0 <= x < WIDTH and 0 <= y < HEIGHT:
            cv2.circle(frame, (int(x), int(y)), radius, color, -1, cv2.LINE_AA)
            reference.append((x, y))
        else:
            reference.append(None)
        if name in ("background_clutter", "similar_distractor", "crossing_objects"):
            for distractor in range(5):
                dx = 280 - (index * 1.3 if name == "crossing_objects" and distractor == 0 else distractor * 41)
                dy = 65 if distractor == 0 else 25 + distractor * 29
                cv2.circle(frame, (int(dx) % WIDTH, int(dy)), 10, (40, 80, 205), -1)
        if name == "compression_artifacts":
            ok, encoded = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 32])
            if ok: frame = cv2.imdecode(encoded, cv2.IMREAD_COLOR)
        noise = np.array([[rng.randrange(-2, 3) for _ in range(WIDTH)] for _ in range(HEIGHT)], dtype=np.int16)
        frame = np.clip(frame.astype(np.int16) + noise[:, :, None], 0, 255).astype(np.uint8)
        frames.append(frame)
    return frames, reference


def main():
    cases = [
        "stationary", "constant_velocity", "constant_acceleration", "circular_motion",
        "large_displacement", "scale_rotation", "temporary_occlusion", "reappearance",
        "lighting_change", "background_clutter", "crossing_objects", "similar_distractor",
        "camera_vibration", "global_camera_motion", "compression_artifacts"
    ]
    report = {"seed": 9173, "frame_count_per_case": COUNT, "cases": {}}
    for name in cases:
        frames, reference = make_case(name)
        baseline, baseline_pauses, baseline_time = run_tracker(frames, reference, robust=False)
        candidate, candidate_pauses, candidate_time = run_tracker(frames, reference, robust=True)
        report["cases"][name] = {
            "baseline": metrics(baseline, reference, baseline_time) | {"pause_count": baseline_pauses},
            "candidate": metrics(candidate, reference, candidate_time) | {"pause_count": candidate_pauses}
        }
    irregular_pts = [0]
    for index in range(1, COUNT):
        irregular_pts.append(irregular_pts[-1] + (66_656 if index == 10 else 16_667))
    report["variable_frame_timing"] = {
        "sample_count": len(irregular_pts),
        "maximum_delta_us": max(b - a for a, b in zip(irregular_pts, irregular_pts[1:])),
        "irregular": True
    }
    output = pathlib.Path("docs/benchmark_artifacts/synthetic_metrics.json")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
