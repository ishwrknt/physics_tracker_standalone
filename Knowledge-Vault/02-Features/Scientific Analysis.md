# Scientific Analysis

**Status:** Implemented

## Purpose and value

Turns recorded observations into understandable tables, graphs, fits, statistics, motion summaries, and signal/spectrum information.

## Dependencies

- [[Motion Tracking]] with valid [[Calibration and Kinematics]] for motion series.
- [[Light Analysis]] for intensity series.
- `AnalysisWorkspace`, `GraphProfiling.kt`, and `CalibrationMath.kt`.

## Modules

Data, Motion, Graph, Fitting, Statistics, Signal, Measurements, and Export.

## Guardrails

Graph/table selection must keep track identities separate. Fit/filter controls must be captured in the export provenance contract once introduced.

## Related

[[Scientific Export]] · [[Dependency Graph]] · [[Gap Analysis]]
