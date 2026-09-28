# Development Plan

| Priority | Feature / phase | Complexity | Dependencies | Risks / open question |
|---|---|---:|---|---|
| P0 | Protect tracking and calibration invariants with regression tests | M | [[Motion Tracking]], [[Calibration and Kinematics]] | Define representative real-video fixtures without bloating the test suite |
| P0 | Split or further modularize `TrackingScreen.kt` | L | UI state boundaries, `VideoPlayerViewModel` | Method-size limit and regressions from moving coupled state |
| P0 | Validate saved light projects and shared workspace on device | M | [[Light Analysis]], [[ADR-008 Shared workspace and persistent light projects]] | Snapshots/history/demo implemented; cold reopen, codec, panel gestures and URI-loss acceptance pending |
| P0 | Accept direct Light selection and live graph on device | M | [[Light Analysis]] | Verify point/line/box drag at zoom and rotation, graph pacing against video playback, Stop/partial save, table and export on representative files |
| P0 | Accept focused Motion/Light navigation and bounded result panels on device | S | [[Light Analysis]], [[Motion Tracking]] | Confirm mode-specific buttons, pointer stability, panel drag/resize and reachable primary controls in both orientations |
| P0 | Accept full-window Light results desk on device | S | [[Light Analysis]] | Check graph/table labels, First/Second comparison selection, frame return, large fonts and both orientations; installation is not interaction validation |
| P0 | Accept Light precision, speed and multi-region analysis on device | M | [[ADR-009 Scalable light regions and playback-paced results]] | Check large handle drag, source-pixel magnifier, speed changes during acquisition, Stop/video pause, same-axis comparison, save/reopen beyond D and fitting presentation |
| P0 | Accept mixed Point/Line/Box Light regions on device | S | [[Light Analysis]], [[ADR-009 Scalable light regions and playback-paced results]] | Add while Line/Box is active, switch A/B/C tools independently, drag each geometry, use fixed Start/Stop, tap the explicit resize button and drag its separate grip, verify stable tool tray and reopen separate results |
| P0 | Accept home card/history and swipe navigation on device | S | [[ADR-010 Home cards separate new work from saved work]] | Verify card versus import-button hit areas, Back/return behavior, horizontal tab swipes versus vertical project and horizontal filter scrolling |
| P1 | Add export provenance | M | [[Scientific Export]], calibration, timing, tracker diagnostics | Choose CSV/PDF schema that remains readable yet reproducible |
| P1 | Calibration/coordinate validation UX | M | [[Calibration]], source transforms | Avoid accepting stale display-space records and warn before invalid analysis |
| P2 | Tracking review and correction workflow | L | [[Track Point]], tracker diagnostics | Keep manual correction history and preserve protected keyframes |
| P2 | Performance benchmark gates | M | tracker engines, device matrix | Establish acceptable latency and accuracy thresholds |
| P1 | Verify [[Live Tracking]] v2 on device matrix | M | physical cameras, rotations, high-FPS | Confirm fused tracker stability, estimate accuracy vs stick reference, rotation remap, latency, memory |
| P2 | Extend live physics modules (pendulum/SHM/projectile) | M | `LivePhysicsModule` contract, [[Live Tracking]] | Keep modules display-only; do not distort the tracking stream |
| P2 | Live calibration origin/axes support | S | [[Live Tracking]], [[Calibration and Kinematics]] | Positions are currently first-sample relative |
| P2 | Marker auto-detection for live scale | M | [[Live Tracking]], [[ADR-005 Robust live tracker and estimated scale]] | Keep honesty contract: detect only unambiguous known-size markers |
| P2 | Live experiment video association | M | [[Live Tracking]], MediaCodec/CameraX VideoCapture | Must not regress real-time pipeline; synchronize via sensor timestamps |
| P3 | IMU device-motion separation for live runs | L | rotation vector + focal mapping + frame homography | Only with rigorous sync; false precision risk otherwise |

## Recommended next logical step

Restore USB device access and verify [[Light Analysis]] shared panels, photo/video sampling,
history/cold reopen and PDF/CSV/XLSX sharing in portrait and landscape. Persistence and the
[[Project Library]] demo picker are implemented, not yet device-accepted. Confirm supplied
demo redistribution rights before publication. Continue independent [[Live Tracking]]
hardware mapping, timing and long-session checks without mixing these scientific workflows.

## Research manuscript validation — Planned

Follow [[Research Evidence Audit]]: recover matched pendulum inputs and geometry, use independent position annotations, and verify Android bitmap/timestamp correspondence before claiming absolute accuracy or cross-device equivalence. The 11–12 September 2026 manuscript reconstruction reproduced the offline metrics. After three concurrent tracking-source edits, the refreshed run passed all 15 focused coordinate, identity, timing, integrity-gate, and candidate-scoring JVM tests. Physical and on-device validation remain **Planned**.

## Research–Plan–Execute–Validate cycle

[[Research Development Methodology]] records the author's established development cycle and the recovered historical BMAD planning artifacts. The manuscript treats PhysicsTracker and its working sequence as the primary results, supported by original screenshots, exported records, and verification evidence. Findings about initialization, timing correspondence, calibration, and tracker recovery feed the next planning cycle. Historical latency and physical-error targets remain **Planned** validation criteria, not completed outcomes.
## Tracking reliability cycle

- **Research completed:** audited confidence duplication, predicted-position blending, geometry centroids and indexed-PTS limits in existing engines.
- **Executed, experimental:** separate blur-aware local tracker, measured colour/context, quality checks, timestamp motion, frozen references, bounded recovery, debug integration, original Classic comparison mode and Android benchmark runner. See [[ADR-007 Benchmark-gated blur-aware offline tracking]].
- **Validate:** JVM regressions and debug build pass; lint reports an existing Camera2 opt-in error. Independent real-video and connected-device validation remain **Planned**.
- **Next Research–Plan–Execute–Validate cycle, Planned:** independently annotated baseline, profiling, flow/rotation/scale ablations, decoder-returned PTS, persisted review status separate from provenance, protected segment correction/rerun/undo, and accessible primary UI. Keep only benchmark-supported improvements.
