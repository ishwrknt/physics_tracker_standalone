# PhysicsTracker Kotlin Standalone

This is the native Android version of PhysicsTracker.

Project location:
- `app/src/main/java/com/physicstracker/app/MainActivity.kt`
- `app/src/main/java/com/physicstracker/app/ui/TrackingScreen.kt`

Package name for Play Console:
`com.physicstracker`

Build:
```bash
./gradlew installDebug
```

Release bundle:
```bash
./gradlew bundleRelease
```

Release signing:
- Copy `keystore.properties.example` to `keystore.properties`
- Point it at your upload keystore before building the release bundle

Notes:
- This folder is separate from the Flutter prototype in `app_codex`.
- Use this folder for Kotlin/Jetpack Compose work only.
