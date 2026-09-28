# PhysicsTracker

Native Android physics analysis: track motion in recorded videos, measure image/video brightness, calibrate experiments, and inspect or export results. Previously named PhyZX.

## Using the app

- **Start:** open a motion video, start light analysis with a photo/video, or open Live camera.
- **Projects:** reopen saved work; filter All, Motion, Light, or Live.
- **Learn:** browse bundled demos and optional learning resources.

Light uses the motion-style workspace: full-height media on the left and tools on the right in landscape, with a bounded bottom tool tray in portrait. Point, Line and Box tools sample real pixels; graphs, tables, comparison and PDF/CSV/XLSX export remain available. Pixel brightness is **not calibrated lux**. Motion and light share presentation components, not measurement records.

## Development setup

Use JDK 17 or a compatible Android Studio bundled JDK, Android SDK platform 36, and this repository's Gradle wrapper. Set the SDK location through your local Android development configuration. Minimum Android API: 24.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
./gradlew :app:lintDebug
```

On this workstation, builds have used `JAVA_HOME=/opt/android-studio/jbr`.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

| Build | Launcher name | Application ID |
|---|---|---|
| Debug | PhysicsTracker Test | `com.physicstracker.test` |
| Release | PhysicsTracker | `com.physicstracker` |

Namespace: `com.physicstracker.app`. Debug and production have separate local data. A locally signed APK cannot replace an installation signed with a different Google Play app-signing key.

## Installation

```bash
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Select a device explicitly if more than one is attached. Never uninstall or clear data to bypass a signing mismatch without the user's informed approval. **Current user preference: install only; do not automatically open the app or run phone UI tests.**

## Release bundle

Configure local signing using `keystore.properties.example`, then:

```bash
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`. Keep credentials and keystores private. The configured version is currently 1.0.2 / code 3; verify the next unused Play Console version code and correct upload certificate before a release. A successful build alone does not establish publish readiness.

## Demos and saved projects

The app includes 29 supplied media files (27 videos, two images), plus one synthetic guided light clip. Originals remain in `demo_videos/`; app copies are under `app/src/main/assets/demos/`.

```bash
bash tools/prepare_demo_media.sh
bash tools/validate_demo_media.sh
```

These scripts require FFmpeg/ffprobe. Sorenson sources are converted to Android-compatible H.264 and labelled as converted; compression can change intensity values. Confirm redistribution rights and attribution before publishing bundled footage.

Motion/live records use Room schema 8. Light projects use app-private versioned snapshots, separate from Room. Keep original imported media accessible. Uninstall/data clearing removes private projects; export important results. Missing-media relinking and portable light-project import/export remain planned.

## Current verification — September 28, 2026

- Debug APK and Android test APK build succeeded; 94 JVM tests passed.
- Latest PhysicsTracker Test APK installed on MT2111, including the clean home tabs and Light split layout.
- Latest build was **not opened**; new phone UI tests compiled but were not executed.
- Earlier September 27 lint result: zero errors, 104 warnings; not a fresh lint run for the final home/layout edits.
- Tracking accuracy improvements and complete photo/video/share/reopen acceptance are not yet established.

## Contributor documentation

Start with [AGENTS.md](AGENTS.md), [project-context.md](project-context.md), and [Project Home](Knowledge-Vault/00-Home/Project%20Home.md).

- [Context handover](CONTEXT_HANDOVER.md): current state, evidence, files and next steps.
- [Implementation instructions](IMPLEMENTATION_INSTRUCTIONS.md): safe change and validation checklist.
- [Architecture](Knowledge-Vault/03-Architecture/System%20Architecture.md).
- [Development plan](Knowledge-Vault/04-Planning/Development%20Plan.md).
- [Tracking benchmark](tools/tracking_benchmark/README.md).

This is the Kotlin/Compose application, not the separate Flutter prototype. The vault is the living record; implementation code is authoritative.
