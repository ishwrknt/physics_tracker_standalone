# PhyZX Scientific UI Redesign

## Product principle

PhyZX is a measurement instrument. The video is the specimen, the timeline is the acquisition axis, trackers are measurement channels, and graphs/tables are representations of recorded observations. UI decoration must never displace measurement controls or obscure measurement state.

## Critical audit

### Structure

- The project library and tracking workspace use unrelated palettes, typography, radii, and navigation models.
- `TrackingScreen.kt` owns navigation, video rendering, pointer interpretation, tracking algorithms, calibration, analysis, export, dialogs, and most reusable controls. This makes UI changes risky and causes broad recomposition.
- Modes, tools, panels, drawers, trays, full-screen flags, and bottom tabs overlap conceptually. A user cannot predict whether an action changes the active instrument, opens settings, or replaces the workspace.
- Graph, table, analysis, light analysis, and tracking controls compete for the same space instead of following a stable workspace hierarchy.

### Workflow

- The main navigation is organized around implementation features instead of the scientific sequence: prepare, acquire, review, analyze, export.
- Playback is represented as a destination even though it is a persistent transport action.
- Advanced automatic-tracking settings are mixed with frequent target-selection actions.
- Precision controls appear in several layouts and are visually inconsistent.
- Project rename/delete relies on undiscoverable long press.
- Important state—active tracker, observation status, current frame, calibration state, and save state—is distributed across panels.
- Many horizontal scrolling rows conceal actions and make large touch targets impossible.

### Visual and accessibility

- Monospace typography is used for nearly all text, reducing reading speed and weakening the distinction between labels and data.
- Excess accent colors, symbols used as icons, translucent surfaces, and dense borders create visual noise.
- Several controls are near or below a reliable 48 dp touch target.
- Icon-only actions lack persistent labels and consistent semantics.
- Floating windows can cover other controls and are not bounded to the available viewport.
- Dark surfaces sometimes differ by too little luminance; disabled and secondary text can fall below useful contrast.
- Full-screen system-bar suppression removes familiar escape/navigation affordances.

### Performance

- Broad local state in the primary composable invalidates large portions of the workspace.
- Multiple nested scroll containers and frequently rebuilt lists add layout work.
- UI state and scientific state are not clearly separated, making deterministic state restoration difficult.
- Bitmap preview and canvas responsibilities are embedded in the same composition as navigation chrome.

## Information architecture

```text
Project Library
├── New experiment
├── Recent experiments
├── Learn
└── App settings

Experiment Workspace
├── Prepare
│   ├── Video information and trim
│   ├── Coordinate system
│   └── Calibration
├── Track
│   ├── Manual
│   ├── Automatic
│   ├── Trackers
│   └── Advanced setup
├── Review
│   ├── Observation table
│   ├── Corrections
│   └── Quality flags
├── Analyze
│   ├── Graph
│   ├── Statistics and fitting
│   └── Light analysis
└── Export
    ├── CSV
    ├── XLSX
    ├── PDF
    └── Share
```

Playback transport, timeline, undo/redo, save state, active tracker, precision magnifier, and the workspace exit are persistent instrument controls rather than destinations.

## Primary journeys

### New experiment

`Library → New experiment → Choose video → Name experiment → Prepare/Calibrate → Track`

The video picker precedes naming. Cancel returns without creating data. Recent projects expose explicit open and overflow actions.

### Manual acquisition

`Track → Manual → choose tracker → tap → fine-adjust → confirm + next → repeat`

The active tracker, magnifier, joystick, coordinates, frame identity, and confirm action remain visible. No automatic navigation occurs.

### Automatic acquisition

`Track → Automatic → select ROI → run → accept observations`

Profile defaults live in Setup. Confidence failure follows `pause → explain uncertainty → correct/accept/skip → retrack`.

### Review and correction

`Review → choose flagged observation → inspect video + data → adjust → preview retrack → commit/undo`

Raw observations remain identifiable and are never overwritten by a display filter.

### Analysis and export

`Analyze → choose representation/filter → inspect graph/table/statistics → Export`

Displayed statistics and exports explicitly use the selected raw or filtered representation.

## Adaptive layouts

### Compact portrait

- Top app bar: back, experiment name, save state, overflow.
- Video occupies the upper workspace.
- Persistent transport and timeline sit directly below video.
- Five-item bottom navigation: Prepare, Track, Review, Analyze, Export.
- Destination controls use a modal bottom sheet limited to two hierarchy levels.
- Precision instrument docks immediately above navigation.

### Landscape phone

- Video uses 62–68% of width.
- A stable right inspector uses the remainder.
- Transport/timeline remains attached to the video.
- A compact vertical destination rail replaces bottom navigation.
- Precision controls dock in the inspector and never cover the target.

### Tablet

- 72 dp navigation rail, flexible video canvas, 320–400 dp inspector.
- Graph/table may open as a resizable secondary pane.
- Panels have min/max sizes and remain bounded within safe drawing insets.

## Component specification

### Project library

- Quiet top bar with product name, version/help, and settings.
- One high-priority `New experiment` action.
- Recent projects use compact cards with name, modified time, and explicit overflow menu.
- Empty state explains the three-step workflow and offers the same primary action.
- Learning links are secondary and never compete with experiment creation.

### Workspace header

- Back/exit always occupies the top-left.
- Experiment name and current stage form the title.
- Active tracker and save state are small status chips.
- Undo, redo, focus, and save are labeled or have accessible descriptions.

### Video stage

- Edge-to-edge neutral black canvas with overlays above the decoded frame.
- Pinch zoom and two-finger pan never modify stored source coordinates.
- A double tap resets the view.
- Selection handles are at least 44 dp hit targets while visual handles remain precise.

### Transport and timeline

- Frame back, play/pause, frame forward are primary.
- Jump, speed, loop, and trim are secondary.
- Current frame and timestamp are always readable.
- Scrubbing pauses acquisition and resumes only by explicit user action.

### Tracking inspector

- First row: Manual/Automatic segmented control and active tracker.
- Frequent action: target/point selection, run/pause, confirm + next.
- Setup groups profile, algorithm, quality, recovery, camera compensation, and diagnostics.
- Destructive clear is separated, labeled, and confirmed.

### Precision instrument

- Reusable for tracking, calibration, orientation, measurement, photogates, and light selection.
- Always contains magnifier, crosshair, coordinate readout, 1/2/5 px step, joystick, and contextual confirm.
- Draft changes update the overlay immediately; persistence and derived data update on confirm.

### Analysis

- Analyze is a graph-first destination: the graph permanently occupies the primary
  workspace and is never replaced by tracking, magnifier, or ROI controls.
- The inspector remains in a bounded secondary pane and the graph can be maximized
  without losing the persistent top-right full-screen exit.
- Representation selector states Raw, Corrected, Filtered, or Fit.
- Data processing is collapsed under Analyze → Data after Curation Results. It
  exposes position averaging, its actual sample window, signal/outlier processing,
  representation state, and reset without reducing the graph viewport.
- Graph, table, statistics, fitting, and export use the selected representation
  consistently; raw observations remain preserved.

### Tracking precision and navigation

- The tracker seed is an exact draggable pixel indicator, not a user-resizable ROI.
- The magnifier shows an 11×11 pixel grid, highlights the selected centre pixel, and
  reports source-video X/Y coordinates.
- Joystick changes remain draft-only until confirmation.
- Automatic template dimensions are selected internally from target profile and
  refinement radius; no template-size box or resize handles clutter the video.
- Frame navigation provides −10, −5, −1, +1, +5, and +10 controls independently of
  automatic-tracking stride.
- Optional video information reports dimensions, rotation, nominal/average frame
  rate, timing variability, frame identity, and requested/actual timestamps.

## Design system

### Color

- Canvas `#070B10`
- Background `#0B1118`
- Surface `#121A23`
- Raised surface `#192431`
- Primary cyan `#52C7EA`
- Secondary amber `#F2B84B`
- Track A `#55D98D`
- Track B `#FFBE55`
- Error `#FF6B6B`
- Primary text `#F1F5F8`
- Secondary text `#A9B7C5`
- Divider `#2A3948`

Color communicates role, never decoration. Quality also uses shape/text so color is not the only indicator.

### Typography

- Sans serif for navigation, explanations, and actions.
- Monospace only for coordinates, timestamps, frame IDs, equations, and measurements.
- Minimum body size 14 sp; minimum supporting text 12 sp.
- Three primary levels: page title, section title, body/data.

### Geometry

- 4 dp base grid.
- Spacing tokens: 4, 8, 12, 16, 24, 32.
- Touch target: 48 dp minimum; precision hit zones may be larger than visible geometry.
- Radius: 8 dp controls, 12 dp panels, 16 dp prominent cards.
- Elevation is reserved for menus, dialogs, and movable panes.

## Interaction and motion

- State transitions: 120–180 ms; panel changes: 180–240 ms.
- No decorative continuous animation.
- Tracking status changes announce immediately and do not move primary controls.
- Long press is an accelerator only, never the sole way to discover an action.
- Back closes the deepest temporary surface first, then returns to the prior destination, then offers to exit.
- Keyboard: arrows step frames, space plays/pauses, Enter confirms, Escape closes temporary UI.

## States

- Empty: explain the next useful action; do not show blank bordered panels.
- Loading: retain layout geometry and show progress near the affected content.
- Error: state what failed, scientific consequence, and recovery action.
- Uncertain tracking: show frame, reason, confidence diagnostics, predicted region, and Accept/Correct/Skip/Stop.
- Offline/model unavailable: offer supported offline fallback and clearly state its quality implications.
- Export success: show format, representation, point count, and share/open action.

## Accessibility

- Every interactive control has a semantic label and selected/disabled state.
- 48 dp touch targets, scalable text, contrast of at least 4.5:1 for normal text.
- Critical states use icon/shape/text in addition to color.
- Panels support switch access and predictable focus order.
- System navigation can be revealed and every full-screen mode has a persistent exit.
- Motion respects platform animation-scale preferences.

## Implementation roadmap

1. Introduce a single design system and adaptive shell.
2. Replace the project library and project actions.
3. Separate persistent workspace chrome from destination content.
4. Normalize navigation to Prepare, Track, Review, Analyze, Export.
5. Extract reusable precision instrument, transport, and status components.
6. Move destination state into stable state holders and tracking algorithms outside composition.
7. Rebuild analysis as bounded panes with explicit data representation.
8. Add semantics, adaptive tests, screenshot tests, and macrobenchmarks.
9. Remove legacy tray/panel paths only after feature-parity verification.

The migration is incremental at the code boundary, but users see one coherent shell. Database entities, source-video coordinate persistence, tracking algorithms, and export engines remain reusable. Legacy navigation chrome, unrelated visual themes, ambiguous symbol-only controls, and scrolling action strips should be eliminated.

## Coordinate and workflow audit — July 2026

The implementation audit identified these high-impact measurement risks:

1. Calibration values were interpreted as both display and source-video coordinates.
   This could offset the selector and magnifier and made behavior dependent on layout.
2. Calibration joystick deltas were converted into display deltas before being applied
   to stored calibration values. Repeated source-pixel steps were therefore not stable.
3. The magnifier sampled the scaled `TextureView` surface and used an even crop, placing
   the crosshair between source pixels and shifting the crop near frame edges.
4. Existing-point dragging persisted display coordinates immediately on drag end,
   bypassing the confirmation boundary and the source-coordinate update path.
5. Landscape Track automatically reserved graph space and data-mode graph opening could
   hide the video, interrupting manual selection and review.

Corrections now use a tested `SourceCoordinateTransform` boundary, create new calibration
in `SOURCE_VIDEO`, retain legacy calibration in `LEGACY_DISPLAY`, sample magnifier imagery
from the decoded video frame, keep point edits draft-only until confirmation, and make
Track/Review graph and table panes explicitly opened floating overlays.

Automated coordinate coverage includes multiple resolutions, aspect ratios, all
quarter-turn rotations, letterboxing, corner/centre/interior points, layout changes, and
ten repeated one-source-pixel joystick movements. Integer and fractional round trips are
required to remain within 0.5 source pixel.

Exports use the currently selected representation. CSV includes processing strength and
averaging window. XLSX is a real zipped workbook with Metadata, Raw observations,
Selected representation, Statistics, and Quality information sheets. Raw observations
remain unchanged in the project and in the workbook.
