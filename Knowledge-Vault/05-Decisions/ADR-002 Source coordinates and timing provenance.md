# ADR-002 Source coordinates and timing provenance

## Decision

Persist tracking observations in source-video coordinates and retain actual presentation timestamps, request timestamps, source dimensions, coordinate-space, and diagnostic metadata.

## Why

Screen coordinates can change with letterboxing, zoom, pan, and rotation. Nominal frame rate cannot reliably replace the video timing index. Scientific analysis needs an auditable measurement basis.

## Impact

[[Motion Tracking]], [[Calibration and Kinematics]], gestures, graphs, and [[Scientific Export]] must convert at boundaries and never silently downgrade timing/provenance.
