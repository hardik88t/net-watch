# NetWatch: Codebase Analysis & Issues Report

Based on a comprehensive review of the codebase against the Product Specification, the UI Inspirations, and the current project state, here is the consolidated list of issues, missing features, bugs, untested logic, and suggested improvements.

## 1. Missing Features (Compared to Specification)

*   **PDF Report Generation**: The `LocalReportExporter` currently only supports TXT, CSV, and JSON exports. The spec calls for formatted PDFs with charts and summaries.
*   **Dual-SIM Routing Accuracy**: `AndroidNetworkSnapshotProvider` uses a basic `SubscriptionManager.getActiveDataSubscriptionId()`. It does not gracefully handle dual-SIM persistence, switching consistency, or exact per-subscription telephony API data logging.
*   **Data Usage Tracking**: Although `AndroidNetworkSnapshotProvider` queries `TrafficStats`, there is no complex data usage tracking (daily/monthly budgets) or "Configurable Limits" enforcement per the spec's "User Controls & Preferences".
*   **VPN and Proxy Flags Verification**: While VPN/Proxy booleans are captured, there is no UI emphasis to flag this data visibly during speed tests or exports to ensure it "does not falsely represent the raw ISP performance".

## 2. Code Bugs & Data Integrity Issues

*   **Stats Fidelity (Truncation Error)**: In `RoomMonitoringRepository.weeklyStats()`, durations are calculated as `(nextTimestamp - snapshot.timestampMs) / 60_000`. This integer division truncates to minutes, resulting in a loss of precision. It should use exact ms-based aggregation.
*   **Missing DB Write Validation**: The `RoomMonitoringRepository` inserts events and snapshots directly without strict data integrity validation. It must validate timestamp order (no time-traveling events), lat/lng bounds, signal sanity (-150 to -30 dBm), and valid profile keys before writing to SQLite.
*   **Permission Flow (Onboarding)**: The `OnboardingScreen` has a "Continue" button that is disabled until all permissions are met, but it provides no explicit UI feedback indicating *why* it is disabled. It relies on a "Skip" dialog to show missing permissions.

## 3. UI/UX Improvements

*   **Timeline Screen**: 
    *   No grouping by Day/Date, making it hard to read over long periods.
    *   No sticky headers for dates.
    *   Lacks a search bar or advanced filtering (e.g., search by annotation text or specific network).
    *   Event cards are dense and lack tap-to-expand details affordances.
*   **Dashboard / Live Map**: UI needs to mirror the rich, transparent aesthetics seen in the Figma/HTML inspirations.

## 4. Test Coverage & Untested Logic

*   **No UI or Android Instrumented Tests**: The `androidTest` directory does not exist. Critical flows like permission requesting in `OnboardingScreen.kt` and `TimelineScreen.kt` rendering are entirely untested.
*   **Repository Math Untested**: `RoomMonitoringRepository.kt` (which handles stats math and weekly aggregations) has no unit tests.
*   **Snapshot Provider Untested**: `AndroidNetworkSnapshotProvider.kt` relies heavily on Android system services (Telephony, Connectivity) but lacks simulated logic tests using Robolectric or mocked managers.
*   **Export Validation**: `LocalReportExporter.kt` export formats (CSV/JSON/TXT) are not unit-tested to prevent regression or malformed outputs.
*   *Current Coverage*: Only 3 domain-level test files exist (`DeadAirAnomalyDetectorTest`, `HeavyTestTriggerEngineTest`, `NetworkTransitionAnalyzerTest`).

## 5. CI/CD & Publishing Setup Issues

*   **F-Droid Metadata**: `fdroid/metadata/com.netwatch.app.yml` points the `commit` directly to `HEAD`. For F-Droid publishing, it must be pinned to explicit tags/commits, and it is missing Fastlane-style changelogs, localized descriptions, and screenshots.
*   **Release Pipeline**: `.github/workflows/release.yml` builds an unsigned APK instead of a fully signed Keystore-based release (APK/AAB) with checksum artifacts and automatic changelog generation.

## 6. Possible New Features (Future Scope)

*   **Background Wi-Fi Scanning Context**: Log specific SSIDs visible in the background even when not connected to map coverage holes better.
*   **Crowdsourced Dead-Zone Mapping**: Allow opted-in users to anonymously share their dead-air anomaly coordinates to build a global open-source cellular blindspot map.
*   **Battery Optimization Triggers**: Dynamically pause the heavy network monitoring if the battery drops below 15% to prioritize device lifeline.
