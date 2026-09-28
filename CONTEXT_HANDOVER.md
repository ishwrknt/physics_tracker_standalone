# PhysicsTracker context handover

Checkpoint: **September 29, 2026**. Read together with [AGENTS.md](AGENTS.md), [project-context.md](project-context.md), and [[Knowledge-Vault/00-Home/Project Home|Project Home]].

## Immediate handoff

September 29 Light controls follow-up: `+ REGION` creates the next independent letter using the selected Point/Line/Box tool, and region selectors show each letter's tool. The fixed Light header now presents `START TRACKING` for video time mode or `MEASURE FRAME` for frame mode, switching to `STOP · KEEP RESULTS` during acquisition. The tool tray remains visible during recording; the former hide button and tap-to-toggle landscape divider were removed, while drag resizing remains. Existing source-pixel acquisition, raw datasets, actual PTS, snapshots and exports remain in place. The debug APK, Android test APK, 101 JVM tests and lint (zero errors, 106 warnings) passed. The same APK was subsequently installed successfully on MT2111 using `adb install -r`; it was not launched or UI-tested. See [[Light Analysis]] and [[ADR-008 Shared workspace and persistent light projects]].

September 28 home navigation follow-up: the Motion and Light Start card bodies open focused saved-project lists; their Open video / Open photo or video buttons start new media flows. Focused lists have Back to Start and Android Back, while the generic Projects tab retains All/Motion/Light/Live filters. Horizontal home-content swipes change Start/Projects/Learn. MainActivity keeps the home saveable state through workspace navigation, and Light's new-project button now opens Android's photo/video document picker directly. The Android interaction test for card, back and swipe compiled but was not run under the install-only device instruction. The debug APK, Android test APK, all 100 JVM tests and lint passed. `adb devices` is empty, so this build was not installed or UI-tested. See [[Project Library]] and [[ADR-010 Home cards separate new work from saved work]].

September 28 Light precision/playback follow-up: Point/Line/Box/Add Point now stay in the workspace header; selection handles and touch targets are larger. The selected measured frame has a region-coloured border/time badge and the graph has a bright selected cursor. The magnifier uses a 112 dp source-pixel view beside the Motion direction pad. Play has 0.1×–4× presets and a slider; future Light graph publication follows speed while raw timestamps remain unchanged. The main Stop pauses playback and preserves partial data. Compare accepts multiple independent colour-coded regions, and Data has bottom controls plus Stats/linear fit. Snapshot checks now allow labels beyond D up to a protective cap of 256; existing snapshot layout remains readable. Four new JVM tests cover region labels/save and fit, bringing the suite to 100 passing tests. The final debug APK, Android test APK, JVM suite and lint passed. The phone reconnected and `adb -s e4aec490 install -r` installed this build successfully on MT2111. It was not launched or UI-tested. See [[Light Analysis]] and [[ADR-009 Scalable light regions and playback-paced results]].

September 28 Light results-desk follow-up: Data now opens a full-window Graph/Table/Compare view using the existing Light datasets. Compare explicitly selects First and Second regions; the table has stable axis/value/raw headers and a selected-row highlight; processing controls sit behind Options. Main tray and Play copy are shorter. View video frame closes results/panels and reveals the decoded source frame. Float restores a bounded media-stage panel. Independent Light regions, actual PTS, raw samples, saves and exports remain authoritative. The debug APK, Android test APK, 96 JVM tests and lint passed. The earlier USB permission failure cleared, and `adb -s e4aec490 install -r` installed the new debug APK successfully on MT2111. The app was not launched or UI-tested, per the user's install-only instruction. See [[Light Analysis]] and [[ADR-008 Shared workspace and persistent light projects]].

September 28 focused-workspace follow-up: the shared bottom row now renders Motion-only Calibrate/Tracking or Light-only Light while keeping Data/Play/View/More in the common style. Light text and inline graph labels are shorter, result panels open one at a time within the media stage, and floating Motion chrome reserves the bottom controls during drag/resize. Light gestures cannot change Point/Line/Box type; only explicit tool selection does. The media stage shrinks as the Light tool sidebar expands. All 95 JVM tests passed; debug APK, Android test APK and lint passed (104 warnings). New navigation and pointer device tests compiled but were not run. The focused-workspace debug APK was installed on MT2111 with `adb -s e4aec490 install -r` (`Success`); it was not launched or UI-tested. See [[Light Analysis]], [[Motion Tracking]] and [[ADR-008 Shared workspace and persistent light projects]].

September 28 Light follow-up: direct Point/Line/Box handle dragging and whole-shape moving are implemented in source coordinates. Video opens in time mode; Track Light plays video while the source-pixel decoder publishes timestamp-paced graph samples in the tool column. Stop retains partial results. Magnifier/fine arrows are docked in tools, primary actions are reduced, the landscape tool column fills available height, and optional floating panels start shorter. Existing Light snapshots, table and PDF/CSV/XLSX paths are reused. All 95 JVM tests, the debug APK, Android test APK compilation and lint passed (104 warnings, zero errors). The new debug APK was subsequently installed with `adb -s e4aec490 install -r` on MT2111 (`Success`). It was not launched or UI-tested. See [[Light Analysis]] and [[Gap Analysis]].

The preceding checkpoint covered handover, agent and README files. Do not infer permission to push, publish, generate a release, or change tracking defaults.

The September 29 functional build **was installed successfully** as PhysicsTracker Test on MT2111. The user explicitly chose **install only; I will check it myself** for device work. Do not open the app, inspect unrelated phone screens, or run device UI tests until requested. No data was cleared and no production app uninstall was performed.

Next useful step: obtain the user's feedback on the installed home and Light layouts. If they authorize device verification, follow the acceptance checklist below.

## Identity and checkout

- Product: PhysicsTracker, formerly PhyZX; artwork: supplied `logonew.png`.
- Checkout: `/media/ishwar/Both_Volume/PhysicsTracker-kotlin-standalone`; branch observed: `main`.
- Release application ID: `com.physicstracker`.
- Debug application ID: `com.physicstracker.test`; launcher label: PhysicsTracker Test.
- Kotlin namespace: `com.physicstracker.app`.
- Configured version: 1.0.2, version code 3; compile/target SDK 36, min SDK 24.
- Room schema: 8; Light snapshots have their own format/version.

The worktree has extensive existing modifications and untracked source, vault pages, media, paper material and generated files. Read `git status` before editing. Preserve all unrelated changes; never use blanket staging or destructive resets. No push, release AAB or Play publication was performed for the latest UI work.

## Latest implemented changes

### Home

`ui/ProjectLibraryScreen.kt` has **Start / Projects / Learn** tabs:
- Start: Motion, Light intensity, optional Live camera.
- Projects: existing project actions and All/Motion/Light/Live filters.
- Learn: demos and collapsible external learning links.
- Header: logo/name/version; no link-toggle clutter.
- Main cards use labelled full-width buttons; content scrolls on narrow screens.

Callbacks and persistence were preserved. The existing `liveSessions` and `onOpenLiveSession` parameters are unused in this screen; do not claim a new dedicated live-session browsing flow was implemented.

### Light layout and tools

`ui/ExperimentWorkspaceFrame.kt` gives Light the established motion layout: default 68% full-height media at left, 32% tools at right in landscape, with a drag-resizable tool edge. Portrait caps the tool tray height. Motion consumes the same proportion constants but still has its own large workspace implementation; these are not one unified screen/state machine.

`ui/light/LightAnalysisScreen.kt` reuses `MainBottomNavRow`, `BottomDeckButton` and floating-panel chrome. Tool groups wrap in the bounded tray. Point/Line/Box, Add Region, fixed Start/Measure/Stop, profile/time measurement, Graph and magnifier remain available; More contains properties, clear, import, save and history. Graph/table/export remain movable panels.

The final layout change was presentation-only. Sampling, coordinate selection, timestamps, persistence, calibration and exports were not redesigned. Shared presentation does not merge TRACK, LIGHT or LIVE measurements.

### Light data

- `LightAnalysisViewModel`: independent region state, acquisition, exact source-frame preview and autosave.
- `LightProjectStore`: versioned streaming app-private snapshots under `filesDir/light-projects`, recoverable replacement and history.
- Regions A–D, source geometry/metadata, actual PTS, raw results and processing preferences are restored.
- Photo/video source-pixel sampling, named brightness/R/G/B channels, line profiles/area means, time acquisition and Stop/partial results.
- Optional smoothing/minimum subtraction/peaks derive from retained raw data. Minimum subtraction is not measured-background correction.
- PDF/CSV/XLSX save/share preserve light scope and provenance.
- If source media is missing, saved results remain available; relinking is not implemented.
- No portable project import/export or recovery of formerly lost in-memory sessions is implemented.

### Branding and demos

`PhysicsTrackerLogo`, launcher assets and visible name use the supplied new logo/PhysicsTracker name; the black startup background remains. Production package/signing identity is unchanged.

`DemoCatalog.kt` and `DemoPicker.kt` expose 29 supplied media files plus one synthetic guide. Demo assets are copied to durable private storage before opening the normal workspace. H.264 MOVs are remuxed; Sorenson clips are converted with frame/timestamp checks. Converted light demos preserve compression warnings in provenance. Original files remain unchanged. Redistribution rights/attribution still need confirmation before publishing.

### Tracking and calibration boundary

Keep existing Classic tracking and manual workflows intact. The initial hybrid scorer reuses confidence proxies; it is not proof of independent shape/context evidence or improved accuracy. Debug-only `BlurAwareTracker` / BLUR TEST remains experimental, with no independently demonstrated accuracy improvement. See [[ADR-007 Benchmark-gated blur-aware offline tracking]] and the benchmark README.

Source coordinates and selected anchors remain authoritative. Calibration governs physical units; A/B identities and actual timing provenance must remain separate. Earlier calibration work added source-space viewport mapping, origin/axis guides and lock/reset controls; latest home/layout work does not validate every calibration gesture.

## Evidence, not assumptions

| Check | Latest known result |
|---|---|
| Debug build | Passed after final home-card changes |
| JVM suite | 94 tests, zero failures/errors |
| Android test APK | Builds; new layout/tab tests compile |
| Lint | September 27: zero errors, 104 warnings; predates final home/layout edits |
| Latest phone installation | Success on MT2111, September 28 |
| Latest cold launch/UI acceptance | Not run: install-only request |
| Earlier cold launch | Separate test build opened successfully before latest layout/home revisions |
| Demo host checks | 29 supplied assets: decoding, dimensions, frame count, relative presentation spacing verified |
| Independent tracker benchmark | Still outstanding |
| Publish-ready release | Not established |

Do not repeat older claims that schema is 6, the name is PhyZX, home has only Live/Experiments, Light is unsaved, or lint still fails at the old Camera2 opt-in.

## Build and install

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Use the workstation JDK path only if present. Output is the **test** application. Never uninstall the Play-signed app to bypass mismatched certificates. Debug and release do not share private projects. No launch command is included intentionally.

## Device acceptance checklist — pending user authorization

1. Start/Projects/Learn: navigation, readable labels at large font size, saved-project filters/actions, demos.
2. Light portrait/landscape: large media stage, right-hand tools, resizing/scrolling, simultaneous graph/table, no overflow.
3. Photo Point/Line/Box and video profile/time: region/channel changes, Stop/clear, magnifier/locator agreement.
4. Save, leave, cold reopen: geometry/raw data/options/timing intact; missing-source results still readable.
5. PDF/CSV/XLSX: save and share actual readable files with selected regions and raw/processed provenance.
6. Check motion/manual tracking, calibration, graph/table and export regressions without mixing histories.
7. Exercise `ExperimentWorkspaceLayoutTest`, `ProjectLibraryTabsTest` and `LightWorkspaceInstrumentedTest` only when phone use is authorized.

## Key files and references

- `MainActivity.kt`: navigation, separate Light ViewModel, saveable workspace state, demo routing.
- `ui/ProjectLibraryScreen.kt`, `ui/ExperimentWorkspaceFrame.kt`: recent home/layout changes.
- `ui/TrackingScreen.kt`: large integration hotspot and shared controls; preserve DEX/method-size safety.
- `ui/light/`: measurement, decoding, playback, storage, panels, exports and guide.
- `app/src/debug/res/values/strings.xml`, `app/build.gradle.kts`: isolated debug identity.
- `tools/prepare_demo_media.sh`, `tools/validate_demo_media.sh`: reproducible teaching assets.
- [[Project Library]] · [[Light Analysis]] · [[System Architecture]] · [[Development Plan]] · [[Gap Analysis]].
- [[ADR-008 Shared workspace and persistent light projects]] records presentation/storage boundaries.

## Project history and research

The app grew from difficulty importing/analyzing experiment videos in a laptop workflow. Preserve the author's **Research–Plan–Execute–Validate** cycle and its workflow diagram in research documentation. The app and demonstrated behavior are results; proposed tracking improvements are not measured results.
