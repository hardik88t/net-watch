# NetWatch

NetWatch is an open-source Android application that acts as a targeted, on-demand network diagnostic tool. It tracks every change in your network along with real internet speed and signal data, helping you build a clear, timestamped record of your actual network performance.

<p align="center">
  <img src="assets/logo.png" width="150" alt="NetWatch Logo">
</p>

## Features

- **Live Network Monitoring**: Passively monitor 5G, 4G, LTE, 3G, and Wi-Fi transitions in real-time.
- **Dead-Air Anomaly Detection**: Lightweight connectivity checks to catch instances of full signal but no actual internet.
- **On-Demand Heavy Testing**: Trigger full performance measurements (download/upload speed, latency) manually or automatically on specific network events.
- **Multi-Profile Support**: Seamlessly handles Dual SIM scenarios and unique Wi-Fi network profiles.
- **Comprehensive Timeline**: Review chronological network events, complete with manual text annotations.
- **Privacy-First & Open-Source**: All data is stored locally. Transparent onboarding explains exactly why permissions are needed.
- **Robust Exports**: Export logs as formatted text, CSV, or JSON for ISPs, regulators, or personal analysis. *(PDF export coming soon)*

## Documentation

For a deeper dive into the app's requirements, features, and current development status:
- [Product Specification](PRODUCT_SPEC.md)
- [Project Status & Roadmap](PROJECT_STATUS.md)

## Getting Started

### Prerequisites
- JDK 17
- Android Studio / Android Gradle Plugin 8.5+

### Build Instructions
To build and test the app locally:
```bash
./gradlew test
./gradlew assembleDebug
```

For F-Droid compatible release builds:
```bash
./gradlew clean assembleRelease
```

## Versioning & Releases

The project uses SemVer-style versioning. You can find the latest APKs in the [GitHub Releases](https://github.com/net-watch).

---
*NetWatch - Empowering users with transparent network evidence.*
