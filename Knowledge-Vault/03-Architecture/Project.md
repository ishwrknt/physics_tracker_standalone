# Project

Room entity `ProjectEntity`, stored in `projects`.

## Fields and responsibility

Name, video URI, source dimensions, trim range, creation time, and update time define one local experiment.

## Relationships

One [[Project]] owns one [[Calibration]], many [[Track Point]] records, and many [[Tracker]] records. Child rows cascade-delete with the project.

## Used by

[[Project Library]] · [[Motion Tracking]] · [[Light Analysis]] · [[Scientific Export]]
