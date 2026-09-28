# Light Analysis

**Status:** Shared workspace controls and persistent projects implemented; device acceptance pending

## Region tool correction — September 29, 2026

Add Region replaces the misleading Add Point label. It creates the next letter (A, B, C, D, then onward) with the active region's Point, Line or Box tool and a separate, visible source-space selection. Each letter's control shows its own tool. After selecting any letter, users can switch that region to Point, Line or Box and measure it without changing another region's geometry or raw result. Full-window Graph/Table/Stats and Compare expose the same letter/tool labels. Existing save, source-pixel sampling, timing and export paths are reused. Host and Android-test compilation are recorded separately from phone gesture acceptance.

The selected region's `START TRACKING` (video over time) or `MEASURE FRAME` action sits in the fixed tool header beside Point/Line/Box. During acquisition it becomes `STOP · KEEP RESULTS`; it stays visible while the live graph updates. The Light tool tray no longer collapses automatically during recording or through a hide button, and changing bottom tabs does not force it open. The landscape divider has a dedicated 48 dp tap button that expands or restores the tools workspace, with a separate grip below for drag resizing. The tools remain visible in either width. These changes leave source sampling, recorded timestamps, and saved results unchanged. Phone reachability and gesture acceptance remain pending.

Validation: debug APK and Android test APK compiled; 101 JVM tests passed; lint passed with zero errors and 106 warnings. The APK was subsequently installed successfully on MT2111; launch and UI testing were not performed.

## Precision, playback and analysis update — September 28, 2026

Point/Line/Box and Add Point now stay in a fixed toolbar. Source selection handles and touch targets are larger, and the Light magnifier pairs a 112 dp source-pixel view with the Motion direction pad for one-pixel nudges. The chosen measured frame has an explicit time/region badge; the graph uses a bright cursor and larger selected marker. The same selected source pixel drives the marker, magnifier and nudge controls.

Play adds 0.1×, 0.25×, 0.5×, 1×, 2× and 4× presets plus a continuous speed slider. While measuring over time, future sample publication follows the chosen playback rate; stored PTS and intensity values do not change. The primary Stop and playback Stop pause video and retain the partial result. The preview returns to the stopped video's decoded frame when measurement ends.

Data's full-window desk now gives most space to the graph and places Graph, Table, Compare, Stats, Options, Float and Export controls at the bottom. Compare can select multiple colour-coded regions and overlays only datasets measured against the same axis. Add Point appends a new independent region; labels continue past Z, with a protective cap of 256. Statistics show sample count, mean, min/max, standard deviation, linear fit, slope and R². Fits are derived brightness trends, not lux or motion kinematics. See [[ADR-009 Scalable light regions and playback-paced results]]. Device interaction and synchronization acceptance remain pending.

The dated sections below record earlier checkpoints; this update supersedes their fixed 1/2/4-region and two-panel comparison descriptions.

## Focused results desk — September 28, 2026

Data now opens a full-window Light results desk with Graph, Table and Compare tabs, using the shared workspace button style. Graph and Table select one region at a time. Compare has explicit First and Second region controls, separate labelled plots, and a direct Measure action when either region lacks data. The table has stable row, axis, value and optional raw-value headers. Processing options are collapsed; the main Light tray no longer shows sample narration or redundant brightness descriptions. The short controls use Over time and Current frame. The Play tray shows only playback time and the seek bar.

Selecting View video frame closes the results view and any floating panel, pauses playback and reveals the decoded source frame in the media stage. Floating graph/table/compare windows remain available through Float. All result views consume the existing datasets and preserve source timestamps, raw values, region identity and export paths. Graph/table readability and touch behavior need device acceptance; host compilation alone does not establish usability.

## Focused workspace revision — September 28, 2026

Light and [[Motion Tracking]] continue to use the same media/tool split, bottom navigation component, button style and floating-window chrome. The navigation now shows only the active project's analysis controls: Light has Light; Motion has Calibrate and Tracking; Data, Play, View and More remain common. Users choose the other analysis type from [[Project Library]], keeping one scientific workflow visible at a time. Light's main tray uses short status text and a compact graph; advanced explanations and settings remain in their existing panels.

Light gesture updates can no longer change the selected Point/Line/Box tool type. Only an explicit tool button can change it. Optional result panels still drag and resize, but only one is open at once and each is bounded to the media stage. The Motion overlay chrome now reserves the bottom control strip when moved or resized. Source coordinates, raw samples, time stamps, saves, tables and exports are unchanged. The debug build, JVM suite and Android test APK compiled; new device interaction tests were not run.

## Direct selection and live graph — September 28, 2026

The Light workspace now defaults a newly opened video to video-time measurement. Its primary tray shows Point/Line/Box, the selected region, a single mode toggle, and Track Light/Stop or Measure. Touching a point or line/box endpoint drags that handle; dragging inside a box or along a line moves the whole selection; dragging elsewhere draws a new selection. The magnifier and one-pixel arrows live in the tool tray rather than a floating window. Geometry is still quantized and stored in source-video pixels. Each edit invalidates only that region's prior result, and settled edits autosave through the existing [[ADR-008 Shared workspace and persistent light projects]] snapshot path.

During video-time analysis, measured source-pixel samples are published approximately every 250 ms to a graph in the tool column. Acquisition is paced by actual decoded presentation timestamps while the video plays, with Stop retaining a labelled partial result. The graph and table consume the same raw dataset; existing PDF/CSV/XLSX and project saves remain scoped to the selected region(s). Slow decoding can make the graph trail playback; no unmeasured frames are fabricated. The landscape tool column now uses its available height, and optional floating result panels open shorter so they leave more of the media visible. The debug build, Android test APK compilation and JVM tests passed. The new debug APK was installed successfully on MT2111, but device gesture/playback/export acceptance remains pending; no phone UI tests were run.

## Purpose and value

Measures real video-frame pixel intensity through POINT, LINE, and BOX geometries, with grayscale/R/G/B channels, profiles, time sampling, graphs, tables, comparisons, and statistics.

## Dependencies

- Local video frame pixels and display-to-video geometry conversion.
- [[Calibration and Kinematics]] for physical distance labels where applicable.
- `LightAnalysis.kt` and shared [[Scientific Export]] output shaping.

## Screen flow

`SELECT` → `GRAPH` / `TABLE` / `COMPARE` → `MORE`; time mode requires explicit START, STOP, and CLEAR acquisition actions.

## Dedicated workspace controls (September 2026)

The separate photo/video workspace in `ui/light/` now exposes Point, Line, Box/area, named brightness/R/G/B channels, spatial-profile versus video-time measurement, and 1/2/4 independent regions. Line/area defaults have a visible extent. Selection tools wrap rather than hiding in horizontal menus. The preview stays above scrolling tools on taller screens; on shorter screens it scrolls to avoid squeezing controls out of view. Measure/Start, Stop, Clear-region, and Export actions remain at the bottom.

Tabs provide Select, Graph, Table, Compare, and Export. Compare shows separately labelled region panels/ranges, not a shared-axis overlay. Graph options restore optional three-sample smoothing, minimum-baseline subtraction, and local peak positions. Raw state is retained; smoothing never bridges marked gaps. Minimum subtraction is explicitly **not** a measured-background correction. Derived values and processing are labelled in graphs/tables/export provenance; CSV and XLSX include raw values alongside processed values. XLSX is a real Open XML workbook; PDF/CSV/Excel sharing and saving use the dedicated light export scope. This does not touch [[Motion Tracking]] calibration or observations.

Device installation and full gesture/media/share verification for this revision are pending; the previously connected phone disconnected during implementation. Do not equate unit tests with on-device usability acceptance.

Validation: 86 JVM tests passed; `assembleDebug`, `lintDebug`, and `assembleDebugAndroidTest` passed. Five new unit tests cover visible geometry defaults, processing/raw provenance, gap-safe peaks, four-region labels, and XLSX content. The added four-region/Excel-sharing device test compiled but was not run because `adb devices` was empty.

## Current limitation

### Layout correction after phone feedback

The previous shared-button implementation still stacked Light chrome above/below the media, unlike the motion experiment layout. `ExperimentWorkspaceFrame` now places Light media in the full-height left 68% in landscape and tools in the right 32%, using the same proportions as [[Motion Tracking]]. The tool-panel edge expands/drags like motion; bottom navigation and button components are reused. Tool groups wrap in the bounded scrolling tray rather than shrinking the media. Portrait caps the tray height to preserve an image/video stage. Header/status/tutorial content stays in the tool column in landscape. Point/Line/Box, Profile/Time, Measure and Graph are the primary actions; properties, additional regions and clearing remain available through More. No sampling, timing, persistence, calibration or export algorithms changed. Device layout and regression checks are recorded separately from implementation.

September 28 validation: debug APK, JVM tests (94 passed) and Android test APK build succeeded. New landscape/portrait layout tests compile but have not run on a device. Installation is pending because the phone disconnected. The user requested **install only**, without opening or running UI tests on the phone; respect that until they request otherwise.

The earlier separate-page presentation above is superseded by shared workspace navigation, the central media stage, bottom tool trays and movable/resizable panels. Graph and Table can stay open together. Playback/seek, zoom/pan, fit-view and pixel precision are available without replacing the media stage. Motion/calibration stays scientifically separate; its controls apply to videos rather than photos.

Light now autosaves settled edits and completed results to versioned app-private snapshots, with explicit Save and readable failure status. History restores regions A–D, source metadata, actual PTS, raw values and processing preferences. Saved results remain readable if the original URI is unavailable, while new measurement is blocked without a decoded source. See [[ADR-008 Shared workspace and persistent light projects]]. Previously lost in-memory-only sessions cannot be recovered.

Home offers Motion and Light entry cards, History filters, and a guided synthetic light demo. The generated 320×240, 24 fps, four-second clip teaches line profile, time measurement, and saved history. Its generator is `tools/generate_light_demo.sh`. The demo contains no private footage.

The [[Project Library]] now also offers the supplied spectrum/light videos and two baseline images through a shared Light/Motion demo picker. Converted teaching clips carry compression warnings in names and saved/exported provenance. This does not make light and motion identical computations: their navigation and movable-panel controls are shared, while pixel intensity, physical calibration and tracked trajectories retain their respective scientific meanings.

Relinking missing media, portable project import/export and physical-device/codec verification remain pending. App data clear/uninstall removes internal snapshots; users should export important results.

September 27 validation: `testDebugUnitTest`, `assembleDebug`, `lintDebug`, and `assembleDebugAndroidTest` passed. Snapshot tests cover serialization, new-store reopen/history, replacement without duplication, interrupted backup recovery and failed-write preservation. Device tests for reopen and the bundled demo compile but were not run: `adb devices` returned no connected devices. This build is ready for device acceptance, not a claim of publish-ready or perfect operation.

## Related

[[Scientific Analysis]] · [[Scientific Export]] · [[Project]]
