# Calibration and Kinematics

**Status:** Implemented; scientific source of truth

## Purpose and value

Defines the scale, origin, and rotation used to transform pixel observations into physical positions, velocities, accelerations, separations, and momentum-related values.

## Dependencies

- [[Calibration]] persisted per [[Project]].
- [[Track Point]] source coordinates and timestamps.
- `SourceCoordinateTransform` and `CalibrationMath`.

## Required screen/data contract

The calibration stick is the primary reference. Optional measurement sticks use its derived scale and cannot replace it. Changes must not leave graphs, data, or export results silently out of sync.

## September 2026 orientation correction (implemented)

`CalibrationViewport` projects the source-coordinate calibration into the actual video viewport using the same pixel-centre mapping as selection. Tap, drag, and precision edits are converted back before persistence; drafts stay in source coordinates. Video metadata rotation and letterboxing are included. Untouched fields are preserved exactly, so moving zero or rotating axes cannot alter the scale stick.

The axis intersection is labelled `(0, 0)` for both coordinates. Perpendicular X/Y guides span the video (solid positive, dashed negative); the positive-X rotation handle uses the same radius for drawing and hit testing. Controls read `ZERO (0,0)` and `ROTATE X/Y`. Legacy display-coordinate calibrations are not silently migrated. Regression tests cover quarter-turn rotations, view scales, origin zero, axis signs, units, and scale preservation. Physical-device interaction verification remains pending until a device is connected.

Related: [[System Architecture]] · [[Scientific Export]].

Validation: `testDebugUnitTest`, `assembleDebug`, and `lintDebug` passed (78 JVM tests, including four calibration regression tests). `adb devices` reported no connected device; install, launch, and portrait/landscape gesture checks are not claimed.

## Quick actions (implemented)

In CAL → AXIS, **Centre zero** moves only the origin to the video centre; **Reset direction** makes displayed +X right and +Y up, including rotated metadata. Both retain the scale, units and other calibration fields and use the existing calibration save/undo path.

**Lock calibration** is available throughout CAL. It commits pending calibration edits, then blocks direct tap, drag, precision, origin/direction and known-distance edits. The separate measurement stick remains usable. Unlock is explicit. The lock is saved UI state for the current video and survives screen rotation; it is not a permanent database/project lock. Quick-action buttons have labelled 48dp minimum-height targets; axis actions are only shown on the AXIS tool.

Related: [[System Architecture]] · [[Scientific Analysis]].

Quick-action validation: `testDebugUnitTest`, `assembleDebug`, and `lintDebug` passed with 81 JVM tests (seven calibration tests). No device was connected; install/launch and touch interaction checks remain pending.

## Planned follow-up

- Uncertainty propagation and calibration quality diagnostics.
- Explicit user warning/repair flow when source dimensions or coordinate-space metadata no longer match.

## Related

[[Motion Tracking]] · [[Light Analysis]] · [[Scientific Export]] · [[ADR-002 Source coordinates and timing provenance]]

## Verified numerical behavior

Source review for [[Research Evidence Audit]] confirms centred neighbour differences internally and one-sided endpoint differences. Optional position averaging uses triangular weights, truncated and renormalized at segment edges. Tracks and nonconsecutive frame segments are separate; intentional sparse frame sampling also splits the current derivative routine. The raw default uses no position average. A selected indexed PTS does not independently verify the returned bitmap identity.
