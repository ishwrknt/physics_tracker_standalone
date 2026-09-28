# Motion Tracking

**Status:** Implemented; high-risk correctness area

## Purpose and value

Lets a user create one or more object trajectories from video frames, manually or through assisted tracking, for physically calibrated analysis.

## Adaptive hybrid tracking

**Status:** Initial scorer implemented; accuracy benefit unverified

Correction: the current `HybridCandidateScorer` repeats confidence proxies in appearance/shape fields; these are not independent context measurements. Classic also blends prediction with matched positions and can assign another candidate's confidence. Preserve this as a comparison path, not evidence of accuracy. `ClassicCandidatePolicy.ORIGINAL_BASELINE` restores the original geometry branch; `INITIAL_HYBRID` remains the default, including shared LIVE callers.

## Blur-aware experiment

**Status:** Experimental foundation implemented; full product scope and validation remain **Planned**.

`BlurAwareTracker`, `ConservativeEvidenceScorer`, `RegionQuality`, and `TimedTargetMotion` add a preserved selected anchor, initial/last-clear patches, full-resolution local translation search, real chroma/weak background-ring evidence, relative contrast/sharpness checks, timestamp motion, distinct-peak ambiguity checks and bounded recovery. Reacquisition requires repeated visual evidence. The final `TrackingIntegrityGate` assesses the returned position; predictions and rejected frames are never saved as measured.

Debug tracking settings expose **BLUR TEST**. Release/default behavior remains unchanged. State belongs to one `AutoTrackSeed`, preserving A/B ownership. Missing frames are counted in session feedback, not persisted as a review timeline. New seed confirmation resets model/motion and writes a protected manual observation. Automatic writes read current persisted observations and skip protected keyframes. Source transforms stay at integration boundaries; frame-dimension changes require reseeding.

**Limitations:** translation only; no optical flow/rotation/scale model, autofocus-versus-motion-blur classification, generated deblurring, detector fusion, or full segment correction/review workflow. Thresholds are development values. Full-frame pixel copies and candidate allocations require device profiling. `OPTION_CLOSEST` retrieval still does not establish decoder-returned PTS: experimental writes preserve indexed microseconds, not verified decoder timestamps. See [[Research Evidence Audit]].

The actual-engine Android benchmark runner and manifest instructions are in `tools/tracking_benchmark/`. Synthetic JVM tests are regression evidence only. No independently annotated real-video or connected-device workflow benchmark has passed. See [[ADR-007 Benchmark-gated blur-aware offline tracking]].

## Depends on

- [[Project Library]] and local video input.
- [[Calibration and Kinematics]] for meaningful scientific values.
- [[Tracker]] and [[Track Point]] for identity and observations.
- `VideoTimelineIndex` for presentation timestamps.
- `ClassicTrackerEngine`; optionally MediaPipe object detection or LiteRT support.

## Screens and APIs

- Primary screen: `TrackingScreen.kt`.
- Required platform APIs: Media3 playback/frame access and Android bitmap/media APIs.
- No network API is required or implemented.

## Guardrails

- Do not merge or overwrite track A and B state/data.
- Save source-video coordinates and actual timing identity.
- Prefer sequential auto tracking if simultaneous processing risks incorrect association.
- Manual corrections and protected keyframes require careful preservation.

## Related

[[Scientific Analysis]] · [[Scientific Export]] · [[Dependency Graph]] · [[Gap Analysis]]
