# Project Status & Tracking

## What I need

- I should be able to get the apk from github releases.
- The app should follow rigorous testing.
- The app should follow the instructions mentioned in the product specification.
- The app should be developed in Kotlin and follow the latest Android development practices.
- The app should be developed in a way that it can be easily maintained and updated.

## Current Kotlin/Android Implementation

- Full Android app scaffold created using Kotlin + Jetpack Compose + Room + DataStore + WorkManager.
- Figma-inspired screens implemented:
  - Transparent onboarding
  - Live monitoring dashboard
  - Timeline with manual event annotations
  - Stats & reports view with exports
  - Monitoring preferences + trigger controls
- Foreground monitoring service implemented with boot resilience:
  - Periodic snapshot logging
  - Lightweight connectivity probing
  - Transition and outage event detection
  - Dead-air anomaly detection
  - Triggered/manual heavy test execution
- Local storage implemented with structured tables:
  - State snapshots
  - Network events
  - Speed test results
  - Profile records (SIM/Wi-Fi)
  - Annotations
- Export support implemented:
  - Formatted text report
  - Raw CSV
  - Raw JSON
- Unit tests added for critical monitoring logic:
  - Transition + outage detection
  - Dead-air anomaly detection
  - Heavy-test trigger decisioning

## Upcoming MVP Tasks (Priority Order)

1. Fix onboarding reliability on real devices first (your blocker): add explicit “why Continue is disabled” status + instrumentation tests for permission flows in OnboardingScreen.kt.
2. Implement production-grade dual-SIM routing: use active data subscription with per-subscription telephony APIs, and persist slot/subscription consistency from AndroidNetworkSnapshotProvider.kt.
3. Add strict data integrity validation before DB writes (timestamp order, lat/lng bounds, signal sanity, profile key checks) in RoomMonitoringRepository.kt.
4. Improve stats fidelity to exact durations (ms-based aggregation instead of minute truncation) in RoomMonitoringRepository.kt.
5. Upgrade exports: add proper PDF report generation (charts + summary + event tables) in LocalReportExporter.kt.
6. Improve timeline UX further: day grouping, sticky headers, search, and better event detail affordances in TimelineScreen.kt.
7. Complete signed release pipeline: keystore-based signed APK/AAB, checksum artifacts, changelog notes in .github/workflows/release.yml.
8. Make F-Droid publishing-ready: pin metadata to tags/commits (not HEAD), add release metadata/changelogs/screenshots in fdroid/metadata/com.netwatch.app.yml.
9. Expand test coverage beyond 3 monitoring unit tests: repository stats math, onboarding permission states, export format validation, map logic.
10. Final MVP hardening pass: battery/performance checks, permission-revocation behavior, and regression run in CI.

## Starting Folder Structure

```bash
$ tree -L 3
.
├── assets
│   ├── background.png
│   ├── foreground.png
│   ├── logo.png
│   ├── net-watch-bg.svg
│   ├── net-watch-fg.svg
│   ├── net-watch-logo.svg
│   ├── Net-Watch-UI-Inspiration
│   │   ...
│   ├── poster-2.png
│   └── poster.png
└── README.md
```
