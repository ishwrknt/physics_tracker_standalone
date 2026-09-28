# Data Model

## Persisted Room entities

| Entity | Responsibility | Key relationships |
|---|---|---|
| [[Project]] (`projects`) | Experiment metadata, video URI, dimensions, trim range | Parent for calibration, points, trackers; cascade deletes children |
| [[Calibration]] (`calibration`) | Scale stick, origin, rotation, coordinate-space metadata | One per project |
| [[Track Point]] (`track_points`) | Timed source-space observation plus tracking diagnostics | Many per project; unique `(projectId, frameNumber, trackLabel)` |
| [[Tracker]] (`trackers`) | User-visible tracker identity, colour, order, enabled state | Many per project |
| Live session (`live_sessions`) | Self-contained [[Live Tracking]] run: metadata, calibration snapshot, CSV sample series | Independent of projects; no foreign keys |

## Entity notes

- [[Track Point]] retains requested and actual timestamps, observation kind, algorithm, confidence, and correction/protection markers. These are the foundation for data provenance.
- [[Calibration]] and points include coordinate-space/dimension metadata to guard against display-space persistence errors.
- Light geometry and series are presently runtime state, not Room entities. See [[Gap Analysis]].
- `live_sessions` was added in schema version 7 by an additive migration; positions inside a session are stored in analysis-frame pixels with a meters-per-pixel snapshot, keeping [[ADR-002 Source coordinates and timing provenance]] provenance rules for the live pipeline.
