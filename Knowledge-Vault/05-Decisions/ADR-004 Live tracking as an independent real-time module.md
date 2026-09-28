# ADR-004 Live tracking as an independent real-time module

## Decision

Live (camera) tracking is implemented as a separate module under
`com.physicstracker.app.live` with its own pipeline, screen, and `live_sessions` Room
table (schema v7, additive migration). It reuses stable, proven components read-only —
`ClassicTrackerEngine` and the calibration unit math — but duplicates no video-workflow
state and shares no storage with video projects.

Real-time transport uses CameraX `Preview` + `ImageAnalysis`; a shared `UseCaseGroup`
viewport aligns overlay coordinates with analysed frames, and `ViewPort`/orientation are
resolved at bind time. Frames are processed on a dedicated background pipeline thread.

## Why

- The video workflow is the app's high-value correctness surface; coupling a real-time
  camera pipeline into `TrackingScreen`/`VideoPlayerViewModel` would risk regressions.
- Sensor timestamps and irregular camera cadence differ fundamentally from video
  timeline indexing, so timing logic must not be shared.
- Isolation keeps the first version small while allowing future physics experiment
  modules ([[Live Tracking]] extension points) without touching the video path.

## Impact

- Schema bump 6 → 7 adds one table only; all existing tables are untouched.
- CameraX is a new packaged dependency (offline, on-device).
- `LiveTracker` and `LivePhysicsModule` are the two sanctioned extension interfaces.
- Graphs/rendering use a bounded rolling window; recording buffers are capped.

## Related

[[Live Tracking]] · [[System Architecture]] · [[Data Model]] · [[Development Plan]]
