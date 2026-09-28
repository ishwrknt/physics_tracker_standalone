# ADR-006 Live runs become Experiments

## Decision

A saved Live Tracking run is converted into a first-class video-workflow experiment
(`projects` + `calibration` + `trackers` + `track_points`) via
`LiveExperimentConverter`, while the raw capture is retained in `live_sessions`. CSV
sharing is an optional export, not storage. The landing screen offers exactly two
primary destinations: **Live** and **Experiments**.

## Why

One analysis engine: live data must flow into the existing [[Scientific Analysis]]
workspace (graphs, tables, fitting, export) instead of growing a parallel live-only
analysis silo. Provenance follows [[ADR-002 Source coordinates and timing provenance]]:
analysis-frame coordinates declared as `SOURCE_VIDEO` with actual sensor timestamps and
per-sample confidence/algorithm metadata.

## Impact

- `track_points` accepts real-time samples with `observationKind = AUTO_MEASURED`.
- Live experiments have `videoUri = ""`; the workspace fails soft on the missing media
  (`VideoTimelineIndex` error state) while all data/gaph areas keep working.
- Room schema 7→8 adds `live_sessions.metaJson` for calibration history; original raw
  samples remain in `live_sessions` regardless.
- No IMU fusion or bundled video capture; both are documented future extensions that
  must not compromise the real-time path.

## Related

[[Live Tracking]] · [[Motion Tracking]] · [[Project Library]] · [[ADR-004 Live tracking as an independent real-time module]] · [[ADR-005 Robust live tracker and estimated scale]]
