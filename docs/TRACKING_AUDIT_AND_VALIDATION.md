# PhysicsTracker tracking audit and validation

Date: 2026-07-22

## Outcome

This milestone establishes a scientific-integrity boundary around automatic tracking: an ambiguous or motion-inconsistent candidate is not persisted as a measured observation. It also introduces source-video observation persistence, presentation-timestamp indexing, provenance, safe legacy classification, identity-separated derivatives, restrained tracking profiles, and repeatable real/synthetic benchmarks.

The implementation is deliberately **not** labelled professional-grade yet. Instrumented migration tests compile, but no Android device was connected for device execution, decoder verification, thermal profiling, or UI validation.

## Five largest original error sources

1. **Display coordinates were persisted as pixels — critical.** `TrackingScreen` converted engine results from video coordinates to screen coordinates and then passed those screen values to `addPointAtTracked`. Canvas size, letterboxing, orientation, and layout therefore changed the measurement. New manual/automatic observations now persist `SOURCE_VIDEO` coordinates plus source dimensions; rendering derives display coordinates. Historical rows are marked `LEGACY_DISPLAY` and are never silently reinterpreted.
2. **Frame identity used nominal-duration division — critical.** Auto tracking generated `frame = position / frameDuration` and sought `frame * frameDuration`. The regression video contains a 66.656 ms sample interval among approximately 16.667 ms intervals, so nominal frame arithmetic can request nonexistent frames or assign duplicate decoded images different identities. `VideoTimelineIndex` now records extractor sample PTS values and resolves the nearest stable frame identity. Requested and actual PTS are stored separately.
3. **Matches were saved without a shared integrity gate — critical.** Classic, MediaPipe, and deep results were persisted immediately when an engine returned a result. The new classic path uses confidence, ambiguity, residual motion, appearance, and search-radius diagnostics. Deep/MediaPipe observations must clear the selected quality threshold. Failure pauses before persistence.
4. **Weak appearance/ambiguity handling caused drift — high.** The old accurate path used brightness-sensitive mean absolute difference; fast tracking exposed only the best SSD match. The revised accurate path uses normalized cross-correlation, a spatially separated competing peak, confidence-gated template refresh, adaptive velocity search, and a marker hue/saturation model. `ROBUST`/`PRECISE` selects the accurate path automatically.
5. **Kinematics crossed tracker boundaries and silently smoothed raw data — critical.** `calibratedTrackPoints` from every tracker were passed to one derivative calculation. This could differentiate A against B when timestamps interleaved. `computeKinematics` now groups by stable `trackerId`, sorts each track by actual timestamp, computes each derivative independently, and does not silently smooth the raw series.

## Architecture implemented

- `TrackingPolicy.kt`: target profiles, quality/recovery policies, motion state, diagnostics, and deterministic integrity decisions.
- `VideoTimelineIndex.kt`: source metadata, complete presentation timestamp index, VFR indication, and nearest-frame identity.
- `ClassicTrackerEngine.kt`: adaptive prediction/search, NCC accurate matching, separated competing peak, marker appearance matching, confidence-gated refresh, and rejection before persistence.
- Room schema 6: tracker records and per-observation source coordinates, source dimensions, requested/actual PTS, observation kind, algorithm, confidence, match/ambiguity/residual diagnostics, correction status, and protected-keyframe status.
- `VideoPlayerViewModel`: source-observation writes, dynamic tracker records (maximum four), atomic segment replacement with one undo/redo action, and presentation-time frame resolution.
- `TrackingScreen`: compact `SETUP` and `ADV` subtabs, MARKER/OBJECT/ROBUST profiles, QUICK/SMART/PRECISE override, recovery override, optional video diagnostics, source-to-display rendering, and distinct uncertain/predicted styling. The always-visible manual magnifier/joystick workflow is preserved.
- Exports: CSV and XLSX include tracker/provenance/quality/timestamp fields; PDF appendix identifies observation kind and confidence.

## Compatibility

- Migration `5 → 6` only adds data. Existing projects, points, calibration, A/B labels, masses, and coordinates remain intact.
- Historical point and calibration coordinates are explicitly `LEGACY_DISPLAY`.
- New source-coordinate points are not combined with a legacy-display calibration. Calibration is omitted for that incompatible series instead of producing physically incorrect values.
- Existing A/B rows receive stable tracker IDs `<projectId>:A` and `<projectId>:B`; the tracker table can manage up to four identities.

## Real regression benchmark

Input: `example_video.mp4`, H.264, 848×478, 190 decoded video samples. Nominal frame rate is 60 fps; average frame rate is about 55.35 fps. Timestamp irregularity is detected.

Reference method: saturated red/pink connected-component centroid with temporal continuity. The generated CSV is reviewable at `benchmark_artifacts/example_video_reference.csv`. This is a reproducible clip-specific annotation, not universal ground truth. The baseline is an offline reproduction of the previous localized grayscale SSD behavior because no Android device was connected to run the historical UI tracker.

| Metric | Baseline | Gated candidate |
|---|---:|---:|
| labelled frames | 190 | 190 |
| measured frames | 190 | 186 |
| mean error | 88.769 px | 2.343 px |
| median error | 16.653 px | 1.005 px |
| 95th-percentile error | 247.013 px | 9.547 px |
| maximum error | 247.926 px | 15.214 px |
| false locks | 90 | 0 |
| pause/loss rate | 0% (including false results) | 2.105% |
| offline benchmark time/frame | 0.119 ms | 2.616 ms |

The candidate trades four withheld frames for eliminating 90 catastrophic false observations. It satisfies the rule that a lower average error cannot be purchased with more false locks.

## Synthetic validation

`tools/benchmark_synthetic.py` deterministically covers stationary, constant velocity, constant acceleration, circular motion, large displacement, scale change, temporary occlusion/reappearance, lighting, clutter, crossing objects, similar distractors, camera vibration/global motion, compression, and irregular timestamps.

The candidate produced zero false locks in every current scenario. In the deliberately discontinuous large-displacement case it paused on 77.3% of labelled frames; in identical-object crossings it paused from the ambiguity through the remaining uncorrected segment. These are safe failures awaiting manual correction, not successful recovery claims. Full results are in `benchmark_artifacts/synthetic_metrics.json`.

## Verification

- `./gradlew testDebugUnitTest assembleDebug --no-daemon`: passed.
- Integrity, motion-search, timestamp-identity, and cross-tracker derivative JVM tests: passed.
- `compileDebugAndroidTestKotlin`: passed.
- Room 5→6 migration instrumentation test: compiled; device execution pending.
- Connected device: debug APK installed on `MT2111` (Android 14); cold startup completed without a fatal exception or ANR. Full gesture/accuracy instrumentation remains pending.
- Current debug APK: 90,057,011 bytes.
- Clean-HEAD debug APK: 89,911,410 bytes.
- APK delta: +145,601 bytes (+0.162%). No new runtime/native dependency was added.

## Known limitations and next validation gates

- Planned Analysis workspace phase: move the graph into a user-controlled floating window and reclaim the fixed graph area for a dedicated tracking ROI panel. The panel must edit the real tracker template/ROI (position, width, height, handles, reset, and lock), provide a live preview, and remain touch-friendly. This is a recorded requirement, not an implemented capability.
- The exact Android decoding and tracker loop must be exercised on representative physical devices; `MediaMetadataRetriever` does not expose the returned bitmap PTS, so persisted `actualTimestampUs` is the extractor PTS selected for the request, not a decoder-reported bitmap timestamp.
- Camera-motion detection/compensation is not yet enabled in the production engine. The timestamp/status infrastructure and synthetic cases exist, but device-quality background feature estimation remains a validation gate.
- Persistent tracker management supports four tracker identities, but the current workspace still exposes the established A/B interaction and comparison UI. C/D editor, graph selection, and simultaneous device performance remain a product/UI gate.
- Atomic protected-keyframe segment replacement and undo exist in the ViewModel; preview/accept/reject and automatic forward-retracking UI are not yet connected.
- Crossing identical objects intentionally pauses. Identity recovery requires user correction; it is not claimed automatic.
- Synthetic benchmark timing is desktop/offline diagnostic timing and must not be treated as Android performance.
- Peak memory, thermal throttling, and calibrated-coordinate accuracy require device instrumentation and a source-coordinate calibration workflow.

## Reproduction

```bash
python3 tools/benchmark_tracking.py example_video.mp4
python3 tools/benchmark_synthetic.py
./gradlew testDebugUnitTest assembleDebug
./gradlew compileDebugAndroidTestKotlin
```
