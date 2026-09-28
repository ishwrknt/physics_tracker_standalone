# Project Library

**Status:** Implemented

## Saved-work entry and swipe navigation — September 28, 2026

The Motion and Light Start cards now distinguish two actions. Tapping a card body opens its filtered saved-project list; tapping Open video or Open photo or video starts a new media import. The focused list has Back to Start and Android Back support. Opening a saved project and returning preserves the focused list, so the user can select another project or go back. The generic Projects tab still offers All/Motion/Light/Live filters. Swipe left/right across home content changes Start/Projects/Learn; tapping tab labels remains available. Light's new-project button opens the system photo/video picker directly; cancelling creates no project. See [[ADR-010 Home cards separate new work from saved work]].

Motion and Light history continue to use their existing independent stores. A Compose interaction test for card, back and swipe navigation compiles; phone gesture acceptance remains pending.

## Purpose

The experiment entry point: users create, reopen, rename, and delete locally stored video-analysis projects.

## Dependencies

- [[Project]] for persisted metadata and local video URI.
- Android document/media access to select video.
- [[System Architecture]] navigation through `MainActivity` and `ProjectLibraryScreen`.

## Used by

[[Motion Tracking]] · [[Light Analysis]] · [[Scientific Export]]

## Focused home tabs — September 28, 2026

The front page now groups content into **Start**, **Projects**, and **Learn**, rather than showing creation, history, demos and external links simultaneously. Start contains Motion and Light intensity cards plus Live camera. Projects contains the existing All/Motion/Light/Live filters and unchanged project-open/action callbacks; an empty filter offers Start a project without implying that saved data was deleted. Learn contains Browse demos and optional external learning resources. The header is limited to branding and version. Tab/filter state survives rotation, large buttons remain labelled, and narrow layouts scroll without squeezing card titles beside long buttons. No project persistence, analysis or export logic changed.

Validation: debug build and Android test build passed; 94 JVM tests passed. A Compose tab/callback regression test compiles but was not run. The updated PhysicsTracker Test APK (including the Light split-layout correction) installed successfully on MT2111. The app was not opened and phone UI tests were not run, respecting the user's install-only request.

## PhysicsTracker branding and bundled demos — September 27, 2026

Implemented: launcher/app label, home and motion workspace use PhysicsTracker and the supplied `logonew.png`. Settings/help and generated motion reports use the new name. Application ID, signing configuration, database and saved project identity are unchanged. The black startup background is retained; the supplied logo artwork itself is unchanged.

Home's **Try demo photos & videos** opens a Light/Motion picker with plain instructions. The catalogue includes all 29 supplied files (27 videos and two images), plus the existing synthetic guided light clip. Each sample opens in the normal [[Motion Tracking]] or [[Light Analysis]] workspace and its resulting project can appear in History. Demo copies live in durable app-private storage, not cache. No preset physical calibration is invented.

`tools/prepare_demo_media.sh` retains the originals: H.264 MOVs are remuxed, older Sorenson videos are transcoded to H.264 with presentation spacing and frame count retained, and MP4/JPEG files are copied. Converted samples are labelled in the picker and project names. Light snapshots and exports additionally retain the conversion warning in pixel provenance; compressed copies are teaching material, not original intensity references. `tools/validate_demo_media.sh` verifies dimensions, frame count, relative presentation times and host decoding. Actual Android codec/UI verification remains a separate device test.

Before store publication, confirm redistribution rights/required attribution for the supplied third-party-looking footage. Local user provision does not establish a redistribution licence. No media is stored in the vault.

Validation: 94 JVM tests passed; debug APK and Android test APK built. Lint reports zero errors and 104 warnings. All 29 supplied demos passed host decoding, dimensions/frame-count and relative-presentation-time checks; the final APK assets were compared byte-for-byte with the validated copies. Device tests and installation remain pending: the phone first reported USB permission failure and the final device check found no connected device.

Follow-up installation: the debug build now uses PhysicsTracker Test (`com.physicstracker.test`) to avoid the Google Play signing conflict. `assembleDebug` passed; installation on MT2111 succeeded and Android confirmed a successful cold launch and resumed MainActivity. No production app uninstall or data clearing was performed. This verifies installation/startup, not the full light sampling, gestures, history and export acceptance suite. See [[System Architecture]] for installation isolation.

## Future enhancements

- Project search/filter and recent-project affordances.
- Missing-video recovery for inaccessible URIs.
