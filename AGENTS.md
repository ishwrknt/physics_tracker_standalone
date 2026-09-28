# PhysicsTracker agent guide

## Source of truth

- Start each task with [[Knowledge-Vault/00-Home/Project Home|Project Home]] and [project-context.md](project-context.md).
- Read [CONTEXT_HANDOVER.md](CONTEXT_HANDOVER.md) for the latest checkpoint and [IMPLEMENTATION_INSTRUCTIONS.md](IMPLEMENTATION_INSTRUCTIONS.md) before implementation.
- Treat `Knowledge-Vault/` as the living product/architecture record. Code is authoritative for implemented behavior; label proposals **Planned**.
- Preserve existing dirty-worktree changes and user materials. Never assume untracked files are disposable.

## Research–Plan–Execute–Validate

1. Research the affected code, vault feature and dependencies.
2. Plan the smallest coherent change and name the invariants at risk.
3. Execute without unrelated refactors or new analysis pipelines for a layout-only request.
4. Validate with focused tests and proportionate builds; report what was not tested.
5. Update feature/architecture/plan/risk pages. Add an indexed ADR for material architecture or product decisions.

## Product and UI contract

- Visible name: PhysicsTracker; supplied logo: `logonew.png`.
- Home tabs: Start, Projects, Learn. Keep creation, history and learning content grouped.
- Motion and Light should feel like one product: large media stage, consistent controls, motion-style landscape split and bounded portrait tools.
- Reuse shared presentation helpers. Keep advanced controls available without exposing all options at once.
- Scientific workflows stay distinct even when the presentation is shared.
- Prefer small helpers outside `TrackingScreen.kt`; preserve DEX/method-size safety.

## Non-negotiable scientific invariants

- Calibration is the source of truth for physical units and derived kinematics.
- Keep tracker/region identities independent. A/B/BOTH is a presentation choice, not permission to merge data.
- Store/process source-video coordinates; transform only at view boundaries. Locator/magnifier must reference the same selected source point.
- Preserve actual presentation timestamps and requested-versus-actual timing. Do not assume constant FPS when metadata exists.
- Preserve observation origin and review status; prediction is not a measured point. Do not silently replace an anchor with an object centroid.
- Keep TRACK, LIGHT and LIVE state boundaries. Light samples real source pixels; brightness values are not calibrated lux.
- Preserve raw observations separately from processing/smoothing.
- Exports must preserve scope, calibration context, timing, provenance and identity.

## Device and release safety

- Debug: `com.physicstracker.test` / PhysicsTracker Test. Release: `com.physicstracker`.
- The latest user instruction is **install only**. Do not automatically launch the app, inspect unrelated phone screens, or run phone UI tests until requested.
- Never uninstall or clear data to overcome a signing mismatch without informed approval.
- Installation success is not workflow validation. Record build, install, launch and UI-test results separately.
- Do not push, publish or generate a release merely because a development task completed.
- Before publishing, verify signing certificate, unused version code, demo redistribution rights and device acceptance.
- Do not expose/stage credentials, keystores, certificates, private media or generated output.

## Code landmarks

Paths below are under `app/src/main/java/com/physicstracker/app/` unless stated otherwise.

- `MainActivity.kt`: app entry/navigation and demo routing.
- `ui/ProjectLibraryScreen.kt`: Start/Projects/Learn.
- `ui/ExperimentWorkspaceFrame.kt`: Light motion-style presentation; shared proportion constants.
- `ui/TrackingScreen.kt`: motion integration hotspot and shared UI controls.
- `ui/VideoPlayerViewModel.kt`: motion playback/persistence/analysis state.
- `ui/light/`: Light ViewModel, source sampling/decoding, snapshots, playback and exports.
- `data/`: Room entities/DAOs/repository; schema version **8**.
- `tracking/`: timing, engines and integrity policy; blur-aware path is experimental.
- `ui/analysis/`, `GraphProfiling.kt`, `CalibrationMath.kt`: scientific transformations.
- `app/src/test/`, `app/src/androidTest/`: host and device tests.

## Verification and documentation

- Use the narrowest relevant tests; broader changes warrant `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- Compile Android tests with `:app:assembleDebugAndroidTest`; this does not run them.
- Run `:app:lintDebug` when relevant, especially for release readiness.
- Keep Classic baseline reproducible; do not enable experimental tracking defaults without independent benchmark evidence.
- Use relative Markdown file links and `[[Wiki Links]]` for vault concepts.
- Diagrams must distinguish implemented from planned behavior.
- Do not store secrets, private footage, certificates or build outputs in the vault.
