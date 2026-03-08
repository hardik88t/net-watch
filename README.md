# Net-Watch
Tracks every change in your network along with real internet speed and signal data. Build a clear, timestamped record of your actual network performance.


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

## Build Requirements

- JDK 17 is required for Android Gradle Plugin 8.5.2.
- Use:

```bash
./gradlew test
./gradlew assembleDebug
```


## Starting Folder Structure 


### Inside assets folder

- background.svg : The Background Layer of app icon
- foreground.svg : The Foreground Layer of app icon
- logo.svg : The Full Logo of the app (Both layers)
- Net-Watch-UI-Inspiration : This file contains inspiration for UI/UX in HTML and PNG format. these are only for inspiration not a mandatory to follow instructions.
- poster.png : Poster for playstore


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
│   │   ├── add_event_note
│   │   ├── animation_motion_principles
│   │   ├── app_settings_triggers
│   │   ├── custom_trigger_builder
│   │   ├── data_viz_indicators_reference
│   │   ├── empty_error_states_reference
│   │   ├── form_input_components
│   │   ├── illustration_iconography_styles
│   │   ├── list_log_card_components
│   │   ├── live_monitoring_dashboard
│   │   ├── modals_bottom_sheets_reference
│   │   ├── network_event_timeline
│   │   ├── network_stats_reports
│   │   ├── notification_alert_patterns
│   │   ├── report_export_preview
│   │   ├── signal_coverage_map
│   │   ├── sim_network_profiles
│   │   ├── speed_test_execution
│   │   └── transparent_onboarding
│   ├── poster-2.png
│   └── poster.png
└── README.md
```



## NetWatch: Open-Source Network Diagnostics & Monitoring App Product Specification

### Objective

Build an open-source Android application called NetWatch (developed in Kotlin) that acts as a targeted, on-demand network diagnostic tool. The purpose of the app is to create a transparent, timestamped, and location-aware record of how an ISP or Wi-Fi network actually delivers service. It must empower users to document real-world performance, detect "dead air" anomalies, and build reliable evidence of network behavior over time without unnecessarily draining battery or data.

### 1. Core Monitoring & Tiered Diagnostics

Passive State Logging: The app must continuously but lightly monitor the device’s active network connection. It must record every change in network technology (5G, 4G, LTE, 3G, Wi-Fi) and create a log entry containing the exact timestamp, location, previous state, new state, and signal strength.

The "Absolute Zero" State: The app must explicitly log exact timestamps and durations of complete outages, including "No Service," "Emergency Calls Only," or total network dropouts, treating them as distinct, measurable states in the timeline.

Anomaly Detection (Lightweight Checks): To detect situations where a device shows full signal but lacks actual internet access, the app must periodically perform ultra-lightweight connectivity checks (e.g., simple pings or handshakes) rather than full bandwidth tests.

On-Demand & Triggered Heavy Testing: Full performance measurements (download speed, upload speed, latency) should only run when specifically triggered. Triggers must include manual user initiation or automated triggers based on detected anomalies (e.g., a sudden drop in signal, switching from 5G to LTE, or a failed lightweight check).

### 2. Multi-Profile Support (Cellular & Wi-Fi)

SIM Profiles & Swaps: The app must uniquely identify and separate data logs for different SIM cards, reliably handling SIM swaps. For Dual SIM devices, the system must accurately track and log the metrics only for the SIM actively handling the data payload.

Wi-Fi Profiles: The app must handle Wi-Fi networks gracefully, treating different Wi-Fi access points as distinct profiles. It must differentiate between routers with the same network name (SSID) to ensure accurate logging of specific hotspots.

### 3. Contextual Data, Geospatial Tagging, & Network Modifiers

Location Context: Every network transition, anomaly, and speed test must be tagged with exact geospatial coordinates. This ensures users can map out exactly where coverage drops or speeds degrade.

VPN and Proxy Awareness: The app must detect and log when a VPN or custom proxy is active during a performance measurement, flagging this data so it does not falsely represent the raw ISP or Wi-Fi network's baseline performance.

Data Usage Tracking: The app should track overall device data usage during monitoring periods to help users determine if poor speeds are related to carrier throttling/congestion rather than signal quality.

### 4. User Controls & Preferences

Strict Constraints: Heavy use of the network and battery is permitted as per user preference. Users must have granular control over testing constraints. 

Configurable Limits: The app must include a dedicated preferences section where users can set hard limits on maximum data usage per day/month for automated tests, define the maximum duration for tests, and customize the exact conditions/anomalies that trigger an automated heavy test etc.

### 5. Data Storage, UI, & Exports

Local Structured Logging: All collected data must be stored locally in a structured, chronological format.

Clear UI/UX: The interface must be feature-rich but clear, allowing power users to easily review a chronological timeline of network events, understand connection duration states, and identify instability patterns.

User Context & Manual Annotations: The interface must allow users to manually add text annotations to specific timestamped events or timeline periods (e.g., "entered a concrete basement," "riding a high-speed train") to provide real-world context to sudden signal drops/up.

Comprehensive Reports: The app must generate statistical summaries (e.g., total time on 5G vs. LTE, average speeds, switch frequency).

Export Capabilities: Users must be able to export their logs as formatted reports (for ISPs/regulators) and as raw data files (CSV/JSON) for external analysis.

### 6. System Resilience & Permissions

Boot Resilience: The application must automatically resume its configured monitoring state (active or paused) upon device reboot without requiring manual user intervention.

Transparent Onboarding: Because accurate network and location logging requires high-level OS permissions, the app must feature a clear onboarding flow. It must explain to the user exactly why background location and usage access are necessary to diagnose their network, reinforcing the open-source and privacy-first nature of the tool.

### 7. Data Integrity & Automated Testing

The system must prioritize correctness. Invalid or inconsistent data must fail early so incorrect records are never saved.

The core logic (detecting transitions, calculating metrics, profiling SIMs/Wi-Fi, and triggering tests) must be treated as critical. It must be validated through automated tests confirming expected behavior under varying simulated scenarios (rapid network switching, signal drops, and Dual SIM routing changes).
