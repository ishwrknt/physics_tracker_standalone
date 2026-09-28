# Scientific Export

**Status:** Implemented; provenance hardening planned

## Purpose and value

Exports experiment results as CSV, XLSX, and PDF/report output rather than an unscoped raw dump.

## Dependencies

- Active [[Scientific Analysis]] or [[Light Analysis]] selection.
- [[Calibration and Kinematics]] context.
- Track identity and timing/provenance data from [[Track Point]].
- Android file/share mechanisms and `ExperimentExport.kt`.

## Scope rules

- Single-track output contains only that tracker.
- Comparison output preserves A/B identity rather than merging records.
- Light exports include geometry, channel, graph/table data, and statistics.

## Future enhancement

Attach a machine-readable provenance manifest: calibration, coordinate space, timing, tracker/model diagnostics, filter/fit configuration, and export scope.
