# Physics Tracker — Google Play release pack

Use this document to create the first production release in Google Play Console. The signed Android App Bundle is kept outside Git at the project root as `PhysicsTracker-release.aab`.

## App identity

| Field | Value |
|---|---|
| App name | Physics Tracker |
| Package name | `com.physicstracker` |
| Version name | 1.0.0 |
| Version code | 1 |
| Category | Education |
| Tags to consider | Physics, Science education, Video analysis, STEM |
| Support email | ishwrknt@gmail.com |
| Privacy policy | `https://ishwrknt.github.io/physics_tracker_standalone/privacy-policy.html` |

## Store listing copy

### Short description (under 80 characters)

Track motion and light intensity from videos and photos.

### Full description

Physics Tracker turns ordinary recordings and images into clear, usable physics measurements. Import a video, calibrate it using a known distance, mark an object frame by frame or use assisted tracking, and view position, velocity, acceleration, speed, and comparison graphs.

Use the app for classroom demonstrations, practical experiments, sports motion studies, pendulum investigations, projectile motion, collisions, and independent STEM projects. Select graph axes and data views that fit your experiment, then inspect the results in tables and analysis panels.

Physics Tracker also includes light-intensity analysis. Measure grayscale or red, green, and blue intensity from a paused video frame or from a picture selected from your device. Use a point, line, or box to create intensity profiles, compare multiple regions, and export the results.

Export your work as CSV, Excel, or PDF. PDF graphs include a clear title, named X- and Y-axes, units, a legend, and visible numeric ranges so results remain understandable when shared or printed.

Your experiment content is processed on your device. Physics Tracker does not include ads, analytics, or a developer-operated cloud account.

## Screenshots and graphics to upload

Create and upload these **real in-app screenshots** in Play Console (do not use a mock-up):

1. Import/Tracking screen with a clearly visible object and marker.
2. Calibration screen showing the known-length line and entered unit.
3. Analysis Desk showing a labelled motion graph.
4. Light analysis on a video frame using a line or box.
5. Light analysis on a user-selected photo using `IMAGE`.
6. Export screen or a generated PDF graph page.

Recommended asset checklist:

- App icon: 512 × 512 PNG (no transparency).
- Feature graphic: 1024 × 500 PNG or JPEG.
- At least 2 phone screenshots; use 1080 × 1920 or a matching device-native resolution.
- Optional tablet screenshots if tablet layout will be promoted.

The project’s actual Analysis Desk capture is available at `docs/assets/analysis-desk.png` and is also shown on the user guide site. Use it as a reference, but capture final Play assets from the release build.

## App content and Data safety answers

These answers describe the current source code. Recheck them if features or SDKs change.

| Play Console question | Current answer |
|---|---|
| Does the app collect or share user data? | No. Video, photo, project, measurement, and export data are processed/stored locally; exports are shared only after the user chooses a share target. |
| Does the app use advertising or analytics SDKs? | No. |
| Does the app require a user account? | No. |
| Does the app access location, microphone, camera, contacts, or health data? | No. |
| Why is media access used? | To let the user select a video for motion analysis. Pictures are selected through Android’s document picker for light analysis. |
| Privacy policy URL | `https://ishwrknt.github.io/physics_tracker_standalone/privacy-policy.html` |

Complete the Play Console content-rating questionnaire truthfully. The app is an educational scientific tool; do not select answers on the developer’s behalf where the questionnaire asks about regional distribution, target age, or commercial status.

## Release steps

1. Back up `upload-keystore.jks` and the ignored `keystore.properties` file in a secure private location. They are required for future app updates.
2. Enable GitHub Pages for the repository using the `main` branch and `/docs` folder. Confirm that the privacy-policy URL above opens publicly.
3. Create the app in Google Play Console with package name `com.physicstracker`.
4. Enrol in Play App Signing. Upload `PhysicsTracker-release.aab` from the project root as the first release artifact.
5. Add the store listing copy and final screenshots/feature graphic.
6. Complete App content, Data safety, Ads, Content rating, and Target audience sections truthfully.
7. Set countries/regions, pricing, and any required developer contact details.
8. Start with Internal testing, install from Play, and exercise import, calibration, manual/auto tracking, image and video light analysis, and CSV/XLSX/PDF export.
9. Promote to production only after the Play pre-launch report and internal testing show no blocking issues.

## Release notes

**Version 1.0.0 — First release**

- Track motion from video with manual and assisted tools.
- Calibrate measurements and analyse position, velocity, speed, and acceleration.
- Compare two tracks and export scientific data.
- Analyse light intensity from both video frames and selected photos.
- Export labelled CSV, Excel, and PDF reports.
