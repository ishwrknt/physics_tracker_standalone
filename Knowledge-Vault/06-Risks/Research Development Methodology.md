# Research Development Methodology

**Status:** Documented development process and manuscript organization. Additional device, physical-accuracy, and usability validation remain **Planned**.

The research paper uses the author's established **Research → Plan → Execute → Validate cycle**. Its primary result is the PhysicsTracker application and evidence of its working. The author reports that difficulties importing experiment videos into a laptop workflow motivated the Android application. The paper uses **PhysicsTracker** as the current name and **PhyZX** for historical branding visible in original screenshots; this documentation does not itself change application UI assets.

## Stages and evidence

- **Research:** define the video-analysis problem and the scientific requirements behind [[Motion Tracking]], [[Calibration and Kinematics]], [[Scientific Analysis]], and [[Light Analysis]].
- **Plan:** use the recovered BMAD scaffold's brief, product requirements, constraints, architecture, data model, roadmap, and acceptance criteria. Preserve the distinction between intent and implemented behavior in [[Development Plan]].
- **Execute:** describe the current Kotlin/Compose/Room/media implementation in [[System Architecture]], with calibration, source-coordinate, timing, and identity invariants.
- **Validate:** inspect source, run focused tests, compare saved CSV/PDF outputs, reproduce numerical diagnostics, and retain unresolved issues in [[Research Evidence Audit]] and [[Gap Analysis]]. Controlled device accuracy and usability studies remain **Planned**.

The original `mainPaper/figs/cycle.png` is the workflow diagram in the paper. It describes the process, not completion of every planned validation. Screenshots, the 160-point application export, fifteen focused JVM tests, and offline diagnostics supply the reported evidence. No measured development-productivity advantage is inferred from the use of BMAD or AI.

## Source records and output

The historical workspace named in `project-context.md` contains nine relevant planning records. Hashed copies are retained under `mainPaper/reconstructed_20260911/analysis/planning_sources/`; proposed latency and physical-error thresholds remain targets. Current code takes precedence over older architecture or implementation notes.

The updated manuscript and supplement are at `mainPaper/reconstructed_20260911/manuscript.pdf` and `mainPaper/reconstructed_20260911/supplementary.pdf`. The detailed scope record is `METHOD_REORGANIZATION.md` in that delivery. The earlier measurement-focused version is preserved in its analysis archive.

Related: [[Project Home]] · [[Research Evidence Audit]] · [[Development Plan]] · [[System Architecture]] · [[Scientific Export]]
