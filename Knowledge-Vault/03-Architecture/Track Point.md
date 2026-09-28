# Track Point

Room entity `TrackPointEntity`, stored in `track_points`.

## Responsibility

A time-indexed object observation. It includes source pixel position, `trackLabel`, `trackerId`, mass, requested/actual timestamp, source dimensions, coordinate space, observation type, algorithm, diagnostic scores, and manual/protected flags.

## Integrity contract

The unique key is `(projectId, frameNumber, trackLabel)`. This record is immutable in identity even when the UI compares tracks together.

## Used by

[[Motion Tracking]] · [[Calibration and Kinematics]] · [[Scientific Analysis]] · [[Scientific Export]]
