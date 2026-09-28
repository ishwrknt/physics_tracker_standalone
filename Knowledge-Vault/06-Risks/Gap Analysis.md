# Gap Analysis

## Bundled demo publication gate — September 27, 2026

[[Project Library]] now includes supplied teaching media. Redistribution rights and required attribution must be confirmed before publishing those assets. Legacy Sorenson copies are H.264 teaching derivatives, explicitly labelled as converted; they must not be presented as original light-intensity evidence. Host decoding and timing checks do not substitute for Android codec and touch-workflow acceptance. The attached phone currently reports USB access denied (`no permissions`), preventing installation and device tests in this session.

## Current gaps

| Area | Gap / risk | Impact | Recommended mitigation |
|---|---|---|---|
| Light analysis | Persistent snapshots/history are implemented, but device cold-reopen and media-permission acceptance remain unverified | Missing sources can prevent new measurements; old in-memory sessions are unrecoverable | Verify [[ADR-008 Shared workspace and persistent light projects]] on device; retain results on URI failure; portable project backups/relinking remain planned |
| Light analysis interaction | Direct point/line/box dragging and timestamp-paced live graph compile and pass host tests, but gesture/playback timing on a device is unverified | Decoder throughput can lag playback and touch targets can feel different across screen sizes | Run the [[Light Analysis]] device acceptance flow with representative videos; keep actual decoded PTS and raw samples authoritative |
| Workspace interaction | Mode-specific navigation, tool-type guard and bounded result windows compile, but touch behavior and panel reachability are unverified on a phone | A visual regression could still hide controls at unusual display sizes or font scales | Verify [[Light Analysis]] and [[Motion Tracking]] in portrait/landscape on device before acceptance |
| Light results usability | Full-window Graph/Table/Compare and explicit region controls compile, but small-screen, large-font and source-frame return behavior are not device-accepted | Users could still miss data or controls despite simpler labels | Verify region switching, table headers, processing options, Float, frame return and exports on a phone |
| Light precision and pacing | Larger handles, shared direction pad, speed-linked publication and multi-region saves need phone acceptance | Touch targets or decoding lag could make the selected source pixel or live graph feel wrong | Verify selected pixel and magnifier agree at edges/zoom, compare playback and graph at slow/fast rates, and confirm Stop pauses video |
| Mixed Light region tools | Add Region can inherit Point/Line/Box, each letter retains its own geometry, and Start/Stop stays fixed, but device interaction remains unverified | A user might mistake an overlapping new selection for the old one, alter the wrong letter, or miss controls at large font sizes | Verify letter/tool labels, dragging, fixed Start/Stop, resize button and grip, comparison and save/reopen on a phone |
| Home navigation | Card-body saved-work entry, media-button creation and tab swipes compile but need touch acceptance | Nested click targets or swipe conflicts could open the wrong flow | Verify card/button taps, Back from focused lists, return from workspace, vertical project scrolling and horizontal filter scrolling on device |
| UI architecture | `TrackingScreen.kt` is a large integration hotspot | Kotlin method-size/build risk; difficult safe changes | Extract state reducers and workspace composables behind narrow contracts |
| Scientific provenance | Export requirements call for context, but provenance completeness needs explicit contract/tests | Users may export results without enough audit information | Include timing basis, calibration, coordinate space, filters, tracker identity, and algorithm diagnostics |
| Validation | A/B and coordinate/timing regressions are high-risk | Incorrect data may look convincing | Add focused tests around source transforms, timing, track filters, and export scope |
| Model lifecycle | Optional MediaPipe/LiteRT models can be packaged or imported | Version, compatibility, and user communication risks | Record model version/source and show capability/failure state clearly |
| URI resilience | Projects store local video URI strings | Revoked/moved document URIs can strand a project | Persist read permissions where feasible; add missing-media recovery UI |
| Live tracking validation | [[Live Tracking]] v3 (KLT + experiment conversion) is device-verified through record/stats; save/open-in-experiments flow needs final human confirmation | Conversion timestamp/identity regressions would silently corrupt analysis | Complete manual save→Experiments→open verification; regression check video workflow |
| Live experiments | Live experiments have `videoUri = ""` (no recorded video) | Replay limited to data analysis; media area shows error placeholder | Documented limitation; optional video association planned |
| Room schema | Schema is at version 8 (`live_sessions.metaJson`) | Downgrade installs (older APK over new DB) would crash | Do not ship downgrades; keep migrations additive |
| Live calibration depth | Live scale is stick-length only, positions first-sample relative | Absolute origin/axis alignment unavailable for live runs | Extend live calibration with origin/axes parity to video workflow |
| Security/privacy | Videos and experiment data are local but may be shared via export | Sensitive content can leave the device intentionally | Clearly identify export destination/scope; no analytics or remote upload is documented |

## Documentation gaps intentionally recorded

- No formal public API contract exists because there is no backend.
- No analytics/event taxonomy is implemented or documented.
- The exact automated build/device test matrix is not documented.

## Watch list

[[Motion Tracking]] now has a debug-only, translation-only blur experiment, not a validated replacement. Real-video accuracy, phone workflow checks, rotation/scale, review persistence and segment correction remain incomplete. Blur thresholds are uncalibrated. Pixel-buffer/search allocations need profiling. Indexed PTS still does not establish returned-frame identity. Full lint currently fails on a Camera2 opt-in at the existing LIVE entry in `MainActivity.kt`; this was not suppressed or changed as part of offline tracking. See [[ADR-007 Benchmark-gated blur-aware offline tracking]].

[[Dependency Graph]] identifies calibration, timing, source-coordinate transforms, and track identity as the highest-leverage correctness bottlenecks.

## Research validation evidence

See [[Research Evidence Audit]] for reproduced offline benchmarks and publication limits. The colour-derived reference is not independent; the six-source pendulum dataset and matched calibration/video provenance are missing. Growth and crossing cases lose substantial coverage despite zero threshold-defined false locks. Restoring independent experimental validation is **Planned**.
