# ADR-003 Independent tracker identity

## Decision

Each tracker owns its points, selected point, seed, preview, persisted `trackerId`, and visual identity. A/B/BOTH are view modes.

## Why

Physics comparisons are invalid if two trajectories can overwrite, merge, or accidentally source one another's points.

## Impact

All graph, table, tracking, correction, and export code must filter by identity. Sequential auto tracking is preferred if it protects correctness.

## Related

[[Motion Tracking]] · [[Track Point]] · [[Scientific Analysis]] · [[Scientific Export]]
