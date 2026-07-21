# PhysicsTracker Project Context

## Purpose
`PhysicsTracker` is an Android physics video-analysis app. Users import an experiment video, calibrate the scene, track one or two objects, inspect tables and graphs, and export scientific results.

## Product Priorities
- Keep the video and tracking workflow fast, clear, and precise.
- Preserve scientific correctness over decorative UI.
- Treat calibration as the source of truth for all derived lengths, speeds, accelerations, separations, and momentum values.
- Keep advanced controls available, but grouped so the main workflow stays clean.

## Tech Stack
- Kotlin Android app
- Jetpack Compose UI
- Media3 `PlayerView` for video playback
- Room for projects, points, and calibration persistence
- Hilt for DI
- Classical tracking plus optional MediaPipe and deep-tracking paths

## Core App Structure
- [MainActivity.kt](/media/ishwar/Both_Volume/Agent/bmad-project-scaffold-v2/PhysicsTracker/app/src/main/java/com/physicstracker/app/MainActivity.kt)
  Entry flow, landing screen, project selection, and handoff into the experiment workspace.
- [TrackingScreen.kt](/media/ishwar/Both_Volume/Agent/bmad-project-scaffold-v2/PhysicsTracker/app/src/main/java/com/physicstracker/app/ui/TrackingScreen.kt)
  Main workspace for tracking, calibration, graphing, table/data views, exports, and analysis overlays.
- [VideoPlayerViewModel.kt](/media/ishwar/Both_Volume/Agent/bmad-project-scaffold-v2/PhysicsTracker/app/src/main/java/com/physicstracker/app/ui/VideoPlayerViewModel.kt)
  Playback state, persistence, tracked-point writes, project data, and analysis data.
- [ExperimentExport.kt](/media/ishwar/Both_Volume/Agent/bmad-project-scaffold-v2/PhysicsTracker/app/src/main/java/com/physicstracker/app/ui/ExperimentExport.kt)
  CSV/XLSX/PDF/report export paths.
- [LightAnalysis.kt](/media/ishwar/Both_Volume/Agent/bmad-project-scaffold-v2/PhysicsTracker/app/src/main/java/com/physicstracker/app/ui/LightAnalysis.kt)
  Light-intensity sampling UI, graph/table/compare helpers, and light export shaping.
- [ClassicTrackerEngine.kt](/media/ishwar/Both_Volume/Agent/bmad-project-scaffold-v2/PhysicsTracker/app/src/main/java/com/physicstracker/app/tracking/ClassicTrackerEngine.kt)
  Classical seed/template tracking implementation.

## UI Structure Rules
- The landing screen is simple and branded: title/logo plus two major destinations:
  - `Experiments`
  - `More`
- The tracking workspace should keep:
  - one thin top header
  - a large primary video stage
  - compact bottom controls
  - graph/data panels below the header, never hidden behind it
- Graphs must stay visible in graph mode. Extra controls expand downward, not over the graph.
- Avoid debug-style clutter, repeated status text, or too many always-visible controls.

## Workspace Rules
- `TRACK` and `LIGHT` are separate analysis worlds.
- `TRACK` owns motion tracking, kinematics graph/table/analysis, and tracking-specific export access.
- `LIGHT` owns light geometry selection, intensity graph/table/compare, and light-specific export access.
- `CAL` and `MORE` are shared utility areas, but they must not blur the mental model between `TRACK` and `LIGHT`.
- The bottom navigation should stay minimal and large-tap friendly.

## Tracking Rules
- `A` and `B` are independent trackers.
- Each track must preserve its own:
  - selected points
  - pending/manual point
  - auto seed
  - preview path
  - saved tracked data
- `A` data must never overwrite or merge into `B` data, and vice versa.
- On-video trails, blinking/current points, labels, and vector-like preview paths must stay separated by `trackLabel`.
- If a workflow choice exists between a flashy simultaneous mode and a correct sequential mode, prefer the correct mode.

## Auto-Tracking Rules
- Auto tracking must support independent seed selection and confirmation for `A` and `B`.
- If both tracks are prepared, the app may run them sequentially from the same playback start position to preserve correctness.
- Auto preview visuals must be labeled and separated by track.
- Pixel picker drag must work in both:
  - seed awaiting state
  - selection/adjust state
- Exact selection must preserve the visible selected point unless the user explicitly enables snapping behavior.

## Manual Tracking Rules
- Manual tracking must support:
  - place
  - drag
  - confirm
  - advance by the selected frame step
- Manual mode needs a way to avoid selecting old points from other frames when trajectories overlap or reverse.
- The selected point and confirmation action must correspond to the actual visible selector position.

## Graph and Data Rules
- Graph/table data must respect track identity strictly.
- `A`, `B`, and `BOTH` are comparison/view modes, not permission to merge underlying data.
- The primary graph source must come from the selected track-specific data source, not a mixed table source.
- Graph colors and legends must follow the same A/B color identity used in tracking overlays.
- For track-specific motion series such as `x`, `y`, `speed`, `ax`, `ay`, and `accel`, the visible graph styling should follow the active track color.
- Comparison overlays may show both tracks together, but the underlying identities remain separate.

## Light Analysis Rules
- `LIGHT` is a major top-level mode, not a secondary add-on.
- `LIGHT` should start with a clean selection stage over the video/image.
- Supported measurement geometry:
  - `POINT`
  - `LINE`
  - `BOX`
- Two-measurement mode is optional. `A/B` controls should only appear when enabled from `LIGHT > MORE`.
- `LIGHT` needs dedicated sub-tabs:
  - `SELECT`
  - `GRAPH`
  - `TABLE`
  - `COMPARE`
  - `MORE`
- `LIGHT > GRAPH` and `LIGHT > TABLE` must support `A / B / BOTH`.
- `TIME` light mode needs explicit user-controlled acquisition:
  - `START`
  - `STOP`
  - `CLEAR`
- Light measurements must come from real pixel sampling of the video frame, not overlay-only positions.
- Light drag/geometry adjustments must remain correct under zoom/pan by using inverse-transformed overlay coordinates.

## Calibration Rules
- The main scale stick defines the calibration reference.
- Any optional measurement stick uses the math derived from the main calibration stick and must not replace it.
- All displayed physical values depend on calibration consistency, so calibration edits must not silently desync graph/data calculations.

## Export Rules
- Export must present experiment results as scientific output, not raw dumps.
- PDF/report export should include the relevant graph/data context:
  - graph series
  - axis limits
  - calibration context
  - derived values
  - track comparison context when applicable
- Export scope should follow the active workspace and visible selection where practical.
- Single-track export should include only that selected track.
- Dual-track export should keep `A` and `B` separate unless the export is explicitly a comparison view.
- Light export should include:
  - selected light mode
  - selected channel
  - geometry summary
  - graph data
  - table data
  - stats

## Design Rules
- Prefer a restrained dark lab-style palette.
- Keep strong contrast for track points and selectors.
- Use consistent A/B color identity across overlays, graphs, legends, and related data accents.
- Reduce wasted spacing between stacked sub-tabs and control rows without making them hard to tap.
- Prioritize portrait mobile usability first.

## Engineering Rules
- Work only in `PhysicsTracker` unless explicitly told otherwise.
- Prefer modifying the existing architecture rather than layering duplicate UI.
- Preserve working tracking, calibration, export, and graph flows unless the change intentionally improves them.
- Use `TrackingScreen.kt` as the main integration point, but move reusable logic into helpers when the file becomes harder to reason about.
- After meaningful changes, build the app and run it on the connected device.

## Current High-Risk Areas
- `TrackingScreen.kt` is the main complexity hotspot.
- `TrackingScreen.kt` is large enough to hit Kotlin's `Method too large` bytecode limit if too many UI branches remain inline.
- A/B separation can regress if graph, table, preview, or auto-run code falls back to shared sources.
- Graph rendering and compare logic are sensitive to accidental mixed data sources.
- Light workspace drag and export logic are sensitive to mismatches between visible selection state and exported scope.
- KSP/Hilt generated output can occasionally require clearing stale build output before a successful rebuild.

## Immediate Implementation Guidance
- Favor scientific correctness over pseudo-simultaneous behavior.
- Keep A/B workflows explicit and separate.
- Keep the video stage primary.
- Keep graph/data visible and readable.
- When in doubt, make the state model clearer before adding more UI controls.
