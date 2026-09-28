# ADR-008 Shared workspace and persistent light projects

**Status:** Implemented; physical-device acceptance pending.

## Decision

Use the same `MainBottomNavRow`, button components, and `FloatingOverlayChrome` for [[Light Analysis]] and [[Motion Tracking]]. Light uses the persistent central photo/video stage with graph/table/properties windows. Domain state and source measurements remain independent. The current workspace shows only its own analysis controls; users choose Motion or Light from [[Project Library]]. Shared Data, Play, View and More buttons remain familiar. A Light gesture cannot switch tool type; only explicit Point/Line/Box selection can do so. One Light result window is shown at a time, and floating windows stay within their usable stage so essential controls remain reachable.

The shared Data entry opens a full-window Light results desk with Graph, Table and Compare tabs. Its optional Float action restores the corresponding bounded panel over the media stage. This is a presentation layer over the same Light datasets, processing preferences and exports; it does not merge Light and Motion measurements. Compare requires separately chosen first and second regions. Selecting a video sample's frame returns to the media stage so the result window cannot conceal it.

The selected Light region's Start/Measure/Stop action remains in the fixed tool header. The tool tray stays visible during acquisition. The landscape divider uses a dedicated expand/restore tap button and a separate drag grip, so an intentional tap can change width without turning every divider touch into a toggle. The Light ViewModel still owns measurement and partial-result preservation.

Persist light projects as versioned, streaming binary snapshots in app-private `files/light-projects`, not sample lists inside a Room cursor. Snapshots contain source URI/dimensions, actual frame timestamps, independent region geometry, raw observations, provenance, and processing preferences. Autosave follows settled edits/completed analysis; explicit Save is available. Writes flush and sync before replacement, retain the prior snapshot during replacement, and recover interrupted replacement backups. Schema version 8 motion/live databases remain untouched. History reads compact headers rather than all observations.

## Consequences and limits

- [[Project]] history combines motion/live Room records and light snapshot summaries with separate filters.
- Imported source media is referenced with document permissions, not copied automatically. Reopening missing media retains saved results for graphs/export and reports the missing source. Relinking missing sources is still **Planned**.
- Snapshots are app-private and removed by uninstall/data-clear. Exports remain the portable backup; no cloud backup is promised.
- A bundled, generated four-second video provides a synthetic light-profile/time-series guide. It is demonstration material, not experimental validation or private user footage.
- Tests must cover snapshot round trips, independent identities, settings, exact PTS, interrupted writes, and shared media/window behavior. Device interaction and codec acceptance require a connected device.

Related: [[System Architecture]] · [[Scientific Export]] · [[Gap Analysis]] · [[Development Plan]].
