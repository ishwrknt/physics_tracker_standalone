# PhysicsTracker implementation instructions

Updated September 28, 2026. Follow [AGENTS.md](AGENTS.md) and the checkpoint in [CONTEXT_HANDOVER.md](CONTEXT_HANDOVER.md).

## 1. Research

Read `project-context.md`, [[Knowledge-Vault/00-Home/Project Home|Project Home]], and the affected feature/architecture pages. Inspect code and `git status`; preserve unrelated changes and user files.

Distinguish implemented code, historical test results, and planned work. In particular:
- Home now has Start/Projects/Learn.
- Light snapshots/history and motion-style layout exist.
- Room schema is 8.
- The old Camera2 lint error was fixed; the last recorded lint run predates final UI edits.
- Tracker improvement remains experimental, not benchmark-proven.

## 2. Plan

Identify presentation, analysis and persistence boundaries before editing. For a UI request, reuse existing callbacks, data models and shared controls; do not invent another analysis pipeline.

Respect source coordinates, actual timestamps, calibration, tracker/region separation, raw/derived data and export provenance. Never convert an estimate to a measurement merely because the user accepts it.

## 3. Execute

- Keep changes small and modular; avoid growing `TrackingScreen.kt` further.
- Keep the media stage primary. Home content belongs under its relevant tab.
- Light and motion may share layout/buttons without merging measurements.
- Preserve source-media permission handling and saved-project compatibility.
- Do not change release signing/package/version for an ordinary UI fix.
- For tracking changes, preserve reproducible Classic baseline and LIVE behavior. New cues must be independent measurements, not duplicated confidence proxies.
- Freeze appearance updates on unreliable evidence; recovery must not silently redefine the selected reference point.
- Keep debug-only blur-aware defaults experimental until independent benchmarks justify a change.

## 4. Validate

Build commands from repository root:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:lintDebug
```

Use a suitable JDK; this workstation uses `/opt/android-studio/jbr`. Device-test compilation is not execution.

For layout/home work, compile `ExperimentWorkspaceLayoutTest` and `ProjectLibraryTabsTest`. For Light changes, use the measurement/store/export JVM tests and `LightWorkspaceInstrumentedTest`. Actual phone tests require renewed user permission to use the screen: **the current instruction is install only**.

For tracking work, compare accuracy and coverage, wrong-object switches, accepted errors, correction count, recovery, timing and memory on independent annotated clips. Refer to `tools/tracking_benchmark/README.md` and [[ADR-007 Benchmark-gated blur-aware offline tracking]]. Do not claim synthetic demos prove tracking accuracy.

## 5. Device installation

```bash
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug installs as `com.physicstracker.test`. If multiple devices are attached, choose the intended serial. If disconnected/unauthorized, report it. Never uninstall/clear data to bypass a signature conflict without informed approval. Do not automatically launch or inspect unrelated apps.

## 6. Handoff

Update the affected vault feature, [[System Architecture]], [[Development Plan]] and/or [[Gap Analysis]]. Add an indexed ADR if the architecture/product decision materially changes.

Record separately:
- files/behavior changed;
- exact checks run and failures/warnings;
- build artifact and application ID;
- install, launch, and workflow-validation status;
- remaining evidence and user decisions.

No push, release or public-site changes unless requested. Before a release, verify upload signing, an unused Play version code, media redistribution rights, device acceptance, privacy claims and export behavior. Never include credentials, private footage, certificates or generated build output in the vault or blanket git staging.
