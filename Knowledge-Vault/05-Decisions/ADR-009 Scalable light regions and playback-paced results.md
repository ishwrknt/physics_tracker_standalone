# ADR-009 Scalable light regions and playback-paced results

**Status:** Implemented; device acceptance pending.

## Decision

Allow users to append independent Light regions with stable labels A–Z, AA onward, up to a protective cap of 256. A new region inherits the active region's Point, Line or Box tool and starts with its own source-space geometry. Selecting a letter and changing its tool affects only that region. Persist each region's geometry and measured dataset independently in the existing version 1 Light snapshot format. The snapshot reader accepts old 1/2/4-region saves and newer counts without changing the on-disk field order. The cap bounds corrupted snapshots and excessive memory use; it is a practical limit, not a scientific requirement.

Present selected regions together in one intensity plot only when their axes have the same meaning (video time or source position). Keep raw observations, actual frame timestamps and per-region identity. Light playback speed (0.1×–4×) affects the Media3 player and the timestamp-paced publication of future Light samples, not their recorded timestamps or values. Stopping measurement stops playback and retains a labelled partial dataset.

Add descriptive statistics and an ordinary least-squares linear intensity trend as derived analysis. The fitted line and R² are descriptive of sampled pixel intensity; they do not represent physical kinematics or calibrated lux. Raw samples remain unchanged and export scope follows selected regions.

## Consequences

- The Light workspace can compare more than two selected regions while preserving a unique colour, tool, geometry and result for each.
- Snapshot round-trip tests cover labels beyond D and independent region E data; older saves remain readable.
- Full-screen Graph/Table/Compare/Stats uses bottom controls and keeps the graph area large. Motion and Light share interaction language, not scientific data.
- Phone checks are still required for handle size, magnifier targeting, speed changes, synchronized stop, layout and large-font use.

Related: [[Light Analysis]] · [[System Architecture]] · [[Gap Analysis]] · [[Development Plan]].
