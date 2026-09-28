# ADR-005 Robust live tracker and estimated scale

## Decision

The live pipeline's tracker is `RobustLiveTracker`: it fuses an SSD template match
(reusing `NativeTemplateTracker`), an HSV color-signature centroid, and a
constant-velocity Kalman filter driven by ACTUAL sensor timestamps, with adaptive search
radius, anti-switch gating, and a REACQUIRING state for temporary loss.

Live scale calibration offers an AUTOMATIC estimate from camera optics — reported focal
length, physical sensor size, active crop, and per-frame focus distance
(`CaptureResult.LENS_FOCUS_DISTANCE` via Camera2 interop) — shown as an estimate that the
user must explicitly confirm. The validated reference-stick calibration stays available
and remains the fallback on devices whose focus distance is fixed or unreliable.

## Why

- A frame-cadence template tracker designed for indexed video underperforms on irregular
  camera timing; prediction must use seconds, not frame counts.
- Single-channel matching jump-locks onto visually similar regions; fusing appearance
  (template) with identity (color) plus motion gating prevents object switching without
  heavy ML dependencies, preserving offline low-latency operation.
- Absolute physical scale cannot be truthfully inferred from a normal camera without a
  reference; the optics estimate uses the most reliable on-device signals while keeping
  uncertainty visible instead of fabricating precision.

## Impact

- Tracker reliability work is confined to `live/RobustLiveTracker.kt`; the video
  workflow's `ClassicTrackerEngine` is not changed.
- Camera2 interop is read-only (no manual sensor controls); if signals are missing the
  feature degrades to manual calibration.
- Rotation remapping uses `FrameRotation` math shared between converter and tracker;
  filtering restarts at orientation changes (recorded data is never silently rescaled).

## Related

[[Live Tracking]] · [[Motion Tracking]] · [[Gap Analysis]] · [[Development Plan]]
