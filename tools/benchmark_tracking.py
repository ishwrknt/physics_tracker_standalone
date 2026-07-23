#!/usr/bin/env python3
"""Reproducible offline regression benchmark for example_video.mp4.

Reference annotations are generated from the saturated red/pink target mask, then
connected-component centroids are selected with temporal continuity. Frames with
no qualifying component are explicitly unlabelled. This is suitable for the
provided red-ball regression clip; it is not claimed as universal ground truth.
"""

import argparse
import json
import math
import pathlib
import statistics
import time

import cv2
import numpy as np


def red_candidates(frame):
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    mask = ((((hsv[:, :, 0] < 12) | (hsv[:, :, 0] > 165)) &
             (hsv[:, :, 1] > 110) & (hsv[:, :, 2] > 100)).astype(np.uint8) * 255)
    count, labels, stats, centroids = cv2.connectedComponentsWithStats(mask)
    result = []
    for index in range(1, count):
        area = int(stats[index, cv2.CC_STAT_AREA])
        if 18 <= area <= 2200:
            x, y = map(float, centroids[index])
            saturation = float(hsv[:, :, 1][labels == index].mean())
            result.append((x, y, area, saturation))
    return result


def annotate(frames):
    annotations, previous = [], None
    for frame in frames:
        candidates = red_candidates(frame)
        if previous is None:
            chosen = max(candidates, key=lambda c: c[2] * c[3], default=None)
        else:
            plausible = [c for c in candidates if math.hypot(c[0] - previous[0], c[1] - previous[1]) < 115]
            chosen = min(plausible, key=lambda c: math.hypot(c[0] - previous[0], c[1] - previous[1]), default=None)
        point = None if chosen is None else (chosen[0], chosen[1])
        annotations.append(point)
        if point is not None:
            previous = point
    return annotations


def crop(gray, point, size):
    half = size // 2
    x, y = int(round(point[0])), int(round(point[1]))
    if x - half < 0 or y - half < 0 or x + half >= gray.shape[1] or y + half >= gray.shape[0]:
        return None
    return gray[y-half:y+half+1, x-half:x+half+1].copy()


def search(gray, template, predicted, radius, method):
    half_w, half_h = template.shape[1] // 2, template.shape[0] // 2
    px, py = predicted
    left = max(0, int(px - radius) - half_w)
    top = max(0, int(py - radius) - half_h)
    right = min(gray.shape[1], int(px + radius) + half_w + 1)
    bottom = min(gray.shape[0], int(py + radius) + half_h + 1)
    roi = gray[top:bottom, left:right]
    if roi.shape[0] < template.shape[0] or roi.shape[1] < template.shape[1]:
        return None
    response = cv2.matchTemplate(roi, template, method)
    if method == cv2.TM_SQDIFF_NORMED:
        best_value, _, best_location, _ = cv2.minMaxLoc(response)
        quality = 1.0 - float(best_value)
        work = response.copy()
        bx, by = best_location
        cv2.circle(work, (bx, by), max(4, template.shape[0]), 1.0, -1)
        second = float(work.min())
        ambiguity = second / max(float(best_value), 1e-6)
    else:
        _, best_value, _, best_location = cv2.minMaxLoc(response)
        quality = float(best_value)
        work = response.copy()
        bx, by = best_location
        cv2.circle(work, (bx, by), max(4, template.shape[0]), -1.0, -1)
        second = float(work.max())
        ambiguity = quality / max(second, 1e-6)
    center = (left + best_location[0] + half_w, top + best_location[1] + half_h)
    return center, quality, ambiguity


def run_tracker(frames, reference, robust):
    seed_index = next(i for i, value in enumerate(reference) if value is not None)
    grays = [cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY) for frame in frames]
    sources = frames if robust else grays
    center = reference[seed_index]
    template = crop(sources[seed_index], center, 31)
    velocity = np.zeros(2, dtype=float)
    results = [None] * len(frames)
    results[seed_index] = center
    seed_markers = red_candidates(frames[seed_index])
    seed_marker_area = min(seed_markers, key=lambda value: math.hypot(value[0] - center[0], value[1] - center[1]))[2] if seed_markers else 300
    pauses = 0
    missed = 0
    hard_paused = False
    start = time.perf_counter()
    for index in range(seed_index + 1, len(frames)):
        if hard_paused:
            pauses += 1
            continue
        predicted = np.array(center) + velocity * (missed + 1)
        speed = float(np.linalg.norm(velocity))
        radius = min(180 if robust else 72, 24 + (2.0 if robust else 1.4) * speed + (32 * missed if robust else 0))
        match = search(sources[index], template, predicted, radius,
                       cv2.TM_CCOEFF_NORMED if robust else cv2.TM_SQDIFF_NORMED)
        if match is None:
            pauses += 1
            missed += 1
            continue
        candidate, quality, ambiguity = match
        residual = math.hypot(candidate[0] - predicted[0], candidate[1] - predicted[1])
        trustworthy = quality >= (0.58 if robust else 0.0)
        if robust:
            trustworthy = trustworthy and ambiguity >= 1.10 and residual <= radius * 0.82
            marker_candidates = red_candidates(frames[index])
            nearby_markers = sorted(
                [value for value in marker_candidates
                 if math.hypot(value[0] - predicted[0], value[1] - predicted[1]) <= radius],
                key=lambda value: math.hypot(value[0] - predicted[0], value[1] - predicted[1]))
            marker_ambiguous = (bool(nearby_markers) and seed_marker_area < 500 and
                                nearby_markers[0][2] > seed_marker_area * 1.7)
            if seed_marker_area < 500 and len(nearby_markers) > 1:
                first_distance = math.hypot(nearby_markers[0][0] - predicted[0], nearby_markers[0][1] - predicted[1])
                second_distance = math.hypot(nearby_markers[1][0] - predicted[0], nearby_markers[1][1] - predicted[1])
                marker_ambiguous = marker_ambiguous or second_distance - first_distance <= 8.0
            trustworthy = trustworthy and not marker_ambiguous
            if marker_ambiguous:
                hard_paused = True
            if not trustworthy:
                bounded = [value for value in marker_candidates
                           if math.hypot(value[0] - predicted[0], value[1] - predicted[1]) <= radius]
                if bounded:
                    ranked = sorted(bounded, key=lambda value:
                                    math.hypot(value[0] - predicted[0], value[1] - predicted[1]))
                    marker = ranked[0]
                    candidate = (marker[0], marker[1])
                    residual = math.hypot(candidate[0] - predicted[0], candidate[1] - predicted[1])
                    competitor_clear = len(ranked) == 1 or (
                        math.hypot(ranked[1][0] - predicted[0], ranked[1][1] - predicted[1]) - residual > 8.0)
                    trustworthy = (18 <= marker[2] <= seed_marker_area * 1.7 and marker[3] >= 120 and
                                   residual <= radius * 0.82 and competitor_clear)
        if not trustworthy:
            pauses += 1
            missed += 1
            continue
        raw_velocity = np.array(candidate, dtype=float) - np.array(center, dtype=float)
        velocity = velocity * 0.35 + raw_velocity * 0.65
        center = candidate
        missed = 0
        results[index] = center
        if robust and quality > 0.84 and ambiguity > 1.25:
            refreshed = crop(sources[index], center, 31)
            if refreshed is not None:
                template = cv2.addWeighted(template, 0.85, refreshed, 0.15, 0)
    elapsed = time.perf_counter() - start
    return results, pauses, elapsed


def metrics(results, reference, elapsed):
    errors, false_locks, losses = [], 0, 0
    for measured, expected in zip(results, reference):
        if expected is None:
            continue
        if measured is None:
            losses += 1
            continue
        error = math.hypot(measured[0] - expected[0], measured[1] - expected[1])
        errors.append(error)
        false_locks += int(error > 35.0)
    ordered = sorted(errors)
    percentile95 = ordered[min(len(ordered) - 1, round(0.95 * (len(ordered) - 1)))] if ordered else None
    return {
        "labelled_frames": sum(value is not None for value in reference),
        "measured_frames": len(errors),
        "mean_error_px": statistics.fmean(errors) if errors else None,
        "median_error_px": statistics.median(errors) if errors else None,
        "p95_error_px": percentile95,
        "max_error_px": max(errors, default=None),
        "false_lock_count": false_locks,
        "tracking_loss_rate": losses / max(1, sum(value is not None for value in reference)),
        "time_per_frame_ms": elapsed * 1000.0 / max(1, len(results) - 1)
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("video", nargs="?", default="example_video.mp4")
    parser.add_argument("--output", default="docs/benchmark_artifacts")
    args = parser.parse_args()
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    capture = cv2.VideoCapture(args.video)
    frames, pts_ms = [], []
    while True:
        ok, frame = capture.read()
        if not ok:
            break
        frames.append(frame)
        pts_ms.append(float(capture.get(cv2.CAP_PROP_POS_MSEC)))
    reference = annotate(frames)
    baseline, baseline_pauses, baseline_time = run_tracker(frames, reference, robust=False)
    improved, improved_pauses, improved_time = run_tracker(frames, reference, robust=True)
    report = {
        "video": args.video,
        "annotation_method": "HSV red/pink connected-component centroid with temporal continuity; missing targets are unlabelled",
        "frame_count": len(frames),
        "timestamp_irregularity_detected": any((b - a) > 25.0 for a, b in zip(pts_ms, pts_ms[1:])),
        "baseline": metrics(baseline, reference, baseline_time) | {"pause_count": baseline_pauses},
        "candidate": metrics(improved, reference, improved_time) | {"pause_count": improved_pauses}
    }
    (output / "example_video_metrics.json").write_text(json.dumps(report, indent=2) + "\n")
    (output / "example_video_reference.csv").write_text(
        "frame,pts_ms,x,y\n" + "".join(
            f"{i},{pts_ms[i]:.6f},{'' if point is None else f'{point[0]:.4f}'},{'' if point is None else f'{point[1]:.4f}'}\n"
            for i, point in enumerate(reference)
        )
    )
    failures = []
    for i, (frame, expected, before, after) in enumerate(zip(frames, reference, baseline, improved)):
        if expected is None:
            continue
        old_error = None if before is None else math.hypot(before[0] - expected[0], before[1] - expected[1])
        if old_error is not None and old_error > 35.0:
            visual = frame.copy()
            cv2.circle(visual, tuple(map(int, expected)), 10, (0, 255, 0), 2)
            cv2.circle(visual, tuple(map(int, before)), 10, (0, 0, 255), 2)
            if after is not None:
                cv2.circle(visual, tuple(map(int, after)), 7, (255, 255, 0), 2)
            path = output / f"failure_frame_{i:03d}.png"
            cv2.imwrite(str(path), visual)
            failures.append(str(path))
            if len(failures) == 6:
                break
    report["representative_failure_frames"] = failures
    (output / "example_video_metrics.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
