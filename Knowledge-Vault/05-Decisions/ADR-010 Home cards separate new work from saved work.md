# ADR-010 Home cards separate new work from saved work

**Status:** Implemented; device acceptance pending.

## Decision

Keep Start, Projects and Learn as the shared home tabs. The body of each Motion or Light Start card opens a focused saved-project list for that analysis type; its explicit media button starts a new project. Motion's button opens the video picker and name flow. Light's button opens Android's photo/video document picker and creates a fresh Light project only after media is selected. The generic Projects tab continues to show All/Motion/Light/Live filters.

The focused list has an in-screen Back to Start control and handles Android Back. Home state is retained through workspace navigation so returning from a saved project returns to the same focused list. Horizontal swipes across the page content move between Start, Projects and Learn; the tab labels remain direct and accessible. A tab or swipe away from a focused list clears that drill-down state.

## Consequences

- The card body and its media button have distinct actions and labels. A cancelled document picker creates nothing.
- Saved Motion, Light and Live records remain in their existing stores; this changes navigation only.
- Vertical project scrolling and horizontal filter scrolling must coexist with tab swipes; device acceptance is needed in both orientations and at large font sizes.

Related: [[Project Library]] · [[System Architecture]] · [[Development Plan]] · [[Gap Analysis]].
