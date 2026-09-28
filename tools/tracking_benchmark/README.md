# Android tracking comparison — experimental, not yet run on footage

The instrumentation runner `TrackingComparisonInstrumentedTest` runs the real Kotlin
Classic Accurate engine in ORIGINAL_BASELINE and INITIAL_HYBRID modes, and the new
blur-aware engine. It does not substitute the older Python algorithms for Android code.
The original mode restores the geometry-selection branch from commit
`2b2d8fc478c5c270c7ebe8167ca7d8c4dd05836f`. The default remains INITIAL_HYBRID,
including shared LIVE callers. Baseline smoothing/centroid limitations are intentional
in the comparison path, not corrected away.

Pre-edit source SHA-256 (13 September inspection):

- ClassicTrackerEngine.kt: `ebe24880647cdb4061c4d7d2bae125f95f5c8865d4ef60755fe1c91d0521c990`
- TrackingPolicy.kt: `8a9a53494c92a9fd446e83e45a4e4af1ee5bcfbed90b23619913287cde779026`

## Prepare consented annotations

Do not commit personal footage. Prepare full-resolution, orientation-normalized PNG
frames, with the **decoder-returned PTS** of each frame and a source-media hash in the
annotation provenance. Document the rotation transform. All pixel coordinates must be
in that same image grid; integer coordinates name pixel centres. Declare independent
manual annotation method, annotator, tolerance, target identity, and visibility. Do not
derive ground truth with the same colour detector used by a tested tracker.

Create `manifest.json` in the app's external-files `tracking-benchmark` directory:

```json
{
  "consented": true,
  "annotation_provenance": "REPLACE with media hash, annotator, method, tolerance and transform",
  "split": "held-out",
  "clips": [{
    "id": "replace_with_clip_id",
    "reference": "SELECTED_POINT",
    "seed_x": 100.0,
    "seed_y": 80.0,
    "large_error_px": 5.0,
    "frames": [
      {"image": "clip/frame000.png", "timestamp_us": 0, "visible": true, "x": 100.0, "y": 80.0},
      {"image": "clip/frame001.png", "timestamp_us": 33367, "visible": true, "x": 102.0, "y": 81.0},
      {"image": "clip/frame002.png", "timestamp_us": 66734, "visible": false}
    ]
  }]
}
```

These numbers are a **format example**, not annotations or benchmark results. Frames
with unknowable point location are `visible: false`, with no invented x/y. Use
`OBJECT_PART` or `EXPLICIT_OBJECT_CENTRE` only when that is the annotated reference.

Run on a connected device:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.physicstracker.app.tracking.TrackingComparisonInstrumentedTest
```

Without a manifest the test **skips**, not passes a benchmark. Outputs `frame-results.csv`
and `summary.csv` include actual returned coordinates, errors, coverage, errors above
the declared tolerance, measurements on unobservable frames, and tracking time. PNG
decode is outside reported tracking time; Bitmap-to-pixel extraction is inside it.
Paused Classic runs remain paused, matching the application. There are no implicit
oracle reseeds. Missing measurements remain missing. No gap interpolation occurs.

## Required promotion study (Planned)

Use separate development and held-out clips: pendulum, projectile, coloured/tiny marker,
object part, rotation/scale, low contrast, patterned background, look-alikes/A–B
crossings, short/prolonged autofocus loss, motion blur, shake, occlusion, VFR,
portrait/landscape/rotated metadata, and long cancellation/memory cases.

The present runner does **not** yet measure decoder fidelity, wrong-object identities,
recovery episode delay, manual action counts, native/Java peak memory, UI responsiveness,
or correction/undo workflows. Add explicit annotations and device instrumentation for
those; a large position error alone is not proof of an object-identity switch.
Translation-only matching does not implement rotation/scale optical flow. Add and
ablate those methods only after this baseline is collected. Judge coverage and false
acceptance together; lower error obtained just by rejecting more frames is not a win.

No real-footage accuracy gain is established. Existing Python results and synthetic
JVM regression tests are not substitute evidence. Do not enable the debug-only BLUR
TEST as a release default until held-out comparisons and full workflow checks pass.
