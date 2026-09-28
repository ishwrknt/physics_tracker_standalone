# Product Mind Map

```text
PhyZX: scientific video analysis on Android
├── Users
│   ├── Physics students and educators
│   └── Experimenters needing mobile motion/light measurements
├── Features
│   ├── [[Project Library]]
│   ├── [[Motion Tracking]]
│   ├── [[Calibration and Kinematics]]
│   ├── [[Light Analysis]]
│   ├── [[Scientific Analysis]]
│   └── [[Scientific Export]]
├── Workflows
│   ├── Import → calibrate → track → analyse → export
│   └── Import → select light geometry → sample → compare → export
├── Screens
│   ├── Project Library
│   ├── Tracking workspace
│   └── Analysis workspace
├── Local services
│   ├── Room persistence
│   ├── Media3 playback/frame access
│   └── Tracking engines
├── Data
│   ├── [[Project]]
│   ├── [[Calibration]]
│   ├── [[Track Point]]
│   └── [[Tracker]]
├── Integrations
│   ├── Android document/media APIs
│   ├── MediaPipe Tasks Vision
│   └── LiteRT
└── Future roadmap
    ├── Durable light-analysis persistence
    ├── Provenance-rich exports
    └── Validation and performance hardening
```

## Product principles

- Scientific correctness, traceability, and clear workflow outrank visual novelty.
- Portrait mobile usability is the primary design target.
- Advanced controls are available without obscuring the video stage or graphs.
- TRACK and LIGHT are separate mental models sharing calibration and media input.
