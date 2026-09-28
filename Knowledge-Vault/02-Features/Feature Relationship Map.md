# Feature Relationship Map

| Feature | Purpose and user value | Depends on | Data / platform contracts | Future enhancement |
|---|---|---|---|---|
| [[Project Library]] | Create, reopen, rename, and remove experiments | [[Project]]; Android document picker | `projects`; video URI access | Recent/search/filter and recovery for revoked URIs |
| [[Calibration and Kinematics]] | Convert pixels to physical measurements | [[Project]], [[Calibration]], [[Track Point]] | `calibration`, `track_points`; coordinate transform | Calibration uncertainty and validation checks |
| [[Motion Tracking]] | Record independent object trajectories | video playback, [[Tracker]], [[Track Point]], [[Calibration and Kinematics]] | Media3; classic tracker; optional MediaPipe/LiteRT | Per-frame review queue and confidence editing |
| [[Light Analysis]] | Measure point/line/box pixel intensity | video frame extraction, [[Calibration and Kinematics]] | in-memory light geometry/series; pixel sampling | Persist measurements and reproducible light sessions |
| [[Scientific Analysis]] | Explore tables, graphs, fits, statistics, signal summaries | [[Motion Tracking]] or [[Light Analysis]] | kinematics and graph-profiling functions | Fit assumptions, uncertainty display, reproducible analysis presets |
| [[Scientific Export]] | Produce usable scientific output | [[Scientific Analysis]], [[Calibration and Kinematics]] | Android share/file APIs; CSV/XLSX/PDF shapers | Explicit provenance manifest and export tests per scope |
| [[Live Tracking]] | Real-time camera tracking with live physics feedback | CameraX; `ClassicTrackerEngine` (read-only reuse); [[Calibration and Kinematics]] scale math | `live_sessions`; sensor timestamps; Camera permission | AI tracker plug-in, origin/axis calibration, experiment-specific physics modules |

## Cross-feature rule

[[Motion Tracking]] uses track identity end-to-end. `A`, `B`, and `BOTH` control visibility/comparison only; they do not change underlying ownership of [[Track Point]] records.
