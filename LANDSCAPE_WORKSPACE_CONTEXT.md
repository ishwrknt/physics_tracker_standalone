# Physics Tracker: Landscape Workspace Context

This document outlines the architecture, layout systems, and interactive features implemented for the Physics Tracker scientific analysis workspace.

## 1. Overview
The landscape workspace is designed as a dual-pane scientific console, keeping the **Analysis Desk** and **Interactive Graph** visible and synchronized simultaneously. 

Primary File: [`TrackingScreen.kt`](file:///media/ishwar/Both_Volume/Agent/app_codex/PhysicsTracker-kotlin-standalone/app/src/main/java/com/physicstracker/app/ui/TrackingScreen.kt)

---

## 2. Interactive Graph System (`GraphCanvasWidget`)
The monolithic graphing engine has been refactored into a reusable, state-synchronized `GraphCanvasWidget`. It supports two visualization modes:
*   **Overlay Mode (`GraphLayoutMode.OVERLAY`)**: Renders all selected data series (Track A, Track B, Delta) on a single unified canvas sharing the same axes.
*   **Stacked Mode (`GraphLayoutMode.STACKED`)**: Dynamically partitions the available screen space vertically into separate subplot canvases for each active series, utilizing independent Y-axis scaling for maximum detail.

### Synchronization & Interaction Features
*   **Shared Timeline & Navigation**: Graph pan, pinch-to-zoom, and range selection are fully synchronized across all active subplots in stacked mode (changes to X-axis bounds in one plot are instantly reflected in all other plots).
*   **Crosshair Tooltips**: Hovering/touching any coordinate displays aligned crosshair markers and value readouts across all subplots at that exact timestamp.
*   **Box Zoom (`⬚`)**: Clicking the box zoom icon toggles marquee-drag selection mode. Users can drag a selection rectangle on the canvas to zoom into the exact horizontal and vertical range.
*   **Double-Tap Reset**: Double-tapping the graph canvas resets the scale and viewport back to the complete dataset boundaries.

---

## 3. Data Filtering & Enhancement Engine
To deal with experimental noise (such as camera tracking dropout spikes and pixel quantization noise) without compromising scientific integrity, the app includes a **Data Filtering & Blending** section:

### Outlier Filtering (Hampel Filter)
*   **Method**: A local rolling window filter calculating Median Absolute Deviation (MAD).
*   **Usage**: Detects and drops single-frame tracking slips (spikes) from both position and velocity axes, leaving valid physical data points untouched.
*   **Toggleable**: Accessible via the **Filter Outliers** switch. Keeping it toggleable teaches students how raw experimental noise contrasts with cleaned physics data.

### Trajectory Smoothing
*   **Method**: A moving average window smoothing.
*   **Usage**: Reduces quantization staircasing in velocity and acceleration plots, accessible via the **Smooth Trajectory** switch.

### Trajectory Blending
*   **Modes**:
    *   `ORIGINAL`: Raw experimental data with fitted curve overlays.
    *   `CURATED`: Pure physics equation values calculated from curve fitting.
    *   `OVERLAY`: Solid ideal curve drawn directly over a faded raw dataset.
    *   `BLEND`: Weighted blend based on the Alpha slider: $\alpha \times \text{ideal} + (1-\alpha) \times \text{raw}$.

---

## 4. Landscape Split-Pane System
*   **Default State**: Rotating to landscape orientation automatically splits the screen, showing the Analysis Desk beside the graph workspace.
*   **Resizable Divider**: A vertical slider lets users drag the pane ratio from `25%` to `65%` of the screen width.
*   **Collapsible Desk**: The desk can be collapsed or expanded at any time (even in full-screen graph mode) using the `◀ Analysis Desk` and `Collapse` toggle buttons.
