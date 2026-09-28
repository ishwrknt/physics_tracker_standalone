# PhysicsTracker project context

Current checkpoint: **September 28, 2026**. This replaces older PhyZX/two-destination home guidance.

See [AGENTS.md](AGENTS.md), [CONTEXT_HANDOVER.md](CONTEXT_HANDOVER.md), and [[Knowledge-Vault/00-Home/Project Home|Project Home]].

## Purpose and priorities

Native Android/Kotlin/Compose application for physics video tracking and photo/video light-intensity analysis. Keep the experience simple, the media stage large, and scientific correctness ahead of decorative UI. The project began after difficulty using a laptop video-import/analysis workflow.

Use the established **Research–Plan–Execute–Validate** cycle. Keep paper methodology separate from measured application results.

## Current product

- Name/logo: PhysicsTracker and supplied `logonew.png`.
- Home: Start (Motion, Light intensity, Live camera), Projects (saved work/filters), Learn (demos/resources).
- Home card bodies open focused saved Motion or Light projects with Back to Start; their media buttons create new projects. Home content also swipes between tabs, and the focused list survives a workspace round trip. See [[ADR-010 Home cards separate new work from saved work]].
- Motion: calibration, manual/automatic tracking, graphs, tables and scoped exports.
- Light: photos/videos, directly draggable Point/Line/Box, brightness/R/G/B, lettered independent regions, profiles/time measurement, a live graph beside video playback, graph/table/compare, PDF/CSV/XLSX and saved history.
- Light Data opens a full-window Graph/Table/Compare desk. Region selectors and explicit First/Second comparison choices replace verbose result text; Options keeps processing controls available. View video frame closes the results view and reveals the selected source frame.
- Latest Light update: larger source handles and Motion-style pixel direction pad; fixed Point/Line/Box/Add Region and Start/Measure/Stop controls; 0.1×–4× video speed; Stop pauses measurement and video; multi-region colour comparison and a Stats/linear-fit tab. New regions inherit the active Point/Line/Box tool. Region labels extend beyond D with a protective 256-region cap. The tool tray stays visible during recording and the landscape divider resizes only by drag. See [[ADR-009 Scalable light regions and playback-paced results]].
- Landscape Light layout matches the motion proportions: media left 68%, tools right 32%; tool panel can expand. Portrait bounds the bottom tray.
- Shared controls do not imply shared measurement state or one monolithic composable. Motion retains its own workspace; Light uses `ExperimentWorkspaceFrame`.
- 29 supplied demo media files plus a synthetic guided clip are bundled. Converted footage is labelled; publication rights remain to be confirmed.

## Architecture landmarks

All application Kotlin paths start at `app/src/main/java/com/physicstracker/app/`.

| Component | Responsibility |
|---|---|
| `MainActivity.kt` | Home/workspace navigation, demo routing, activity-scoped Light model |
| `ui/ProjectLibraryScreen.kt` | Start/Projects/Learn |
| `ui/TrackingScreen.kt` | Motion integration and shared button/panel controls |
| `ui/ExperimentWorkspaceFrame.kt` | Light presentation shell and shared split constants |
| `ui/VideoPlayerViewModel.kt` | Motion playback, project state, analysis |
| `ui/light/LightAnalysisViewModel.kt` | Independent Light acquisition and persistence state |
| `ui/light/LightAnalysisDesk.kt` | Full-window Light graph, table, comparison and options presentation |
| `ui/light/LightStatistics.kt` | Derived descriptive intensity statistics and linear fit |
| `ui/light/LightProjectStore.kt` | Versioned app-private Light snapshots/history |
| `ui/light/LightMedia.kt` | Real photo/video pixel decoding and timing |
| `data/` | Room schema 8; motion/live persistence |
| `tracking/` | Classic/optional engines, timeline and integrity policy |
| `live/` | Separate live camera pipeline |

See [[System Architecture]], [[Project Library]], [[Light Analysis]], [[Motion Tracking]], [[Live Tracking]], [[Calibration and Kinematics]] and [[Scientific Export]].

## Scientific invariants

- Calibration governs physical units and derived quantities.
- Source coordinates and actual timing/provenance stay authoritative.
- Tracker A/B and Light regions stay independent; BOTH is presentation only.
- Selector/magnifier share the selected coordinate. Do not substitute an object centroid for a selected point.
- Keep raw data, derived processing, predictions and manual corrections distinguishable.
- Light samples source pixels, not rendered overlays; brightness is not lux.
- TRACK/LIGHT/LIVE remain distinct scientific workflows with consistent presentation.

## Persistence and boundaries

Motion/live use Room schema 8. Light uses app-private streaming snapshots, not Room. Settled edits and completed/partial results are saved; explicit Save and failure status are available. Source media must remain accessible for new measurement. Saved results survive a missing URI, but source relinking and portable Light project import/export remain planned. Clearing app data/uninstall removes projects.

Original Classic fallback remains intact. Debug-only BLUR TEST is experimental; independent real-video benchmark improvement is unproven. Do not treat earlier confidence-proxy scoring as independent appearance/shape evidence.

## Verification and deployment

- Current debug package: `com.physicstracker.test` (PhysicsTracker Test).
- Release package: `com.physicstracker`; namespace `com.physicstracker.app`.
- Configured version: 1.0.2/code 3; SDK min 24, compile/target 36.
- Latest build: Light mixed-region and fixed Start/Stop debug APK and Android test APK compiled; 101 JVM tests passed; lint passed with zero errors and 106 warnings.
- Latest install: the September 29 Light mixed-region and fixed Start/Stop debug APK installed successfully on MT2111 as `com.physicstracker.test` using `adb install -r`.
- Latest launch/UI acceptance: **not run**, at the user's explicit install-only request.
- Latest lint: zero errors, 104 warnings.
- No new release bundle/push/publication for the latest changes.

Do not automatically open the app or run device UI tests until requested. Do not uninstall the Play app to bypass signing. Debug/release histories are independent.

## Next work, only when requested

Collect user feedback and verify home navigation, portrait/landscape tools, selection/magnifier mapping, video timing, save/reopen and real shared files on-device. Keep [[Development Plan]] and [[Gap Analysis]] current. No claim of complete workflow acceptance or publish readiness is warranted yet.
