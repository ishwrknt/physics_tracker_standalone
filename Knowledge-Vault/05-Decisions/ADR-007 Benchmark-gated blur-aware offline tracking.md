# ADR-007 Benchmark-gated blur-aware offline tracking

**Status:** Accepted for experimental implementation; default promotion **Planned**.

## Context

[[Motion Tracking]] needs blur reliability without extra normal-user choices. Inspection found correlated confidence proxies, prediction blending and centroid substitution in Classic. Existing Python benchmarks do not validate Android engines or independently establish accuracy; see [[Research Evidence Audit]].

## Decision

Keep the shared Classic/LIVE default unchanged and provide an explicit original-Classic comparison policy. Add a separate debug-only offline tracker with selected-anchor preservation, real patch/colour/context evidence, relative quality checks, timestamp motion, bounded recovery and final integrity gating. Predictions guide search but never become saved measurements. Appearance freezes during uncertainty; reacquisition needs repeated evidence.

No model dependency or Room migration is added. Tracking logic stays outside `TrackingScreen.kt`; that screen adapts per-track state, coordinates and persistence. Indexed PTS remains distinct from verified decoder PTS.

## Consequences

This translation-only foundation may reject more frames than Classic. Rejection alone is not an accuracy improvement. Independent held-out Android comparisons must evaluate error and coverage together before promotion. Optical flow/rotation/scale, sequential PTS-verified decoding, profiling, persisted review and segment rerun/undo, accessible primary UI and device workflows remain **Planned**. [[Development Plan]] and [[Gap Analysis]] track these gaps. [[Live Tracking]], LIGHT and calibration are not redesigned.

## Research basis

Correlation follows the [OpenCV template-matching definition](https://docs.opencv.org/3.4.4/df/dfb/group__imgproc__object.html). Future flow must validate features and spatial support; see [OpenCV tracking API](https://docs.opencv.org/4.5.2/dc/d6b/group__video__track.html). These describe methods, not evidence of improved PhysicsTracker accuracy.

Related: [[System Architecture]] · [[ADR-002 Source coordinates and timing provenance]] · [[ADR-003 Independent tracker identity]]
