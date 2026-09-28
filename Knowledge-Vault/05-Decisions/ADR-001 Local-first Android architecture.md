# ADR-001 Local-first Android architecture

## Context

PhyZX analyses locally selected experiment videos on Android. The implemented app has no backend or network API.

## Decision

Use Jetpack Compose, Room, Hilt, Media3, and on-device tracking integrations. Persist experiment data in the local Room database.

## Alternatives considered

- Cloud upload and server-side analysis
- A cross-platform client with a remote backend

## Consequences

- The app can work without an application server, but cloud sync/collaboration are separate future initiatives.
- Android URI permission lifecycle and local-device performance are first-class concerns.

## Related

[[System Architecture]] · [[Project]] · [[Gap Analysis]]
