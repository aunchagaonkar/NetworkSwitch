<div align="center">
<img src="media/app_icon.png" width="160" height="160" style="display: block; margin: 0 auto"/>
<h1>Network Switch</h1>
<p>Modern Android app for network mode switching</p>

[![Build Status](https://img.shields.io/github/actions/workflow/status/aunchagaonkar/NetworkSwitch/build.yml)](https://github.com/aunchagaonkar/NetworkSwitch/actions)
[![License](https://img.shields.io/github/license/aunchagaonkar/NetworkSwitch)](https://github.com/aunchagaonkar/NetworkSwitch/blob/main/LICENSE)
[![Downloads](https://img.shields.io/github/downloads/aunchagaonkar/NetworkSwitch/total)](https://github.com/aunchagaonkar/NetworkSwitch/releases)
[![Release](https://img.shields.io/github/v/release/aunchagaonkar/NetworkSwitch)](https://github.com/aunchagaonkar/NetworkSwitch/releases/latest)
[![Awesome](https://awesome.re/mentioned-badge-flat.svg)](https://github.com/timschneeb/awesome-shizuku)

</div>

---

A modern Android application that enables users to toggle between network modes, supporting 34 different network configurations including 2G, 3G, 4G, and 5G combinations. Features dual control methods: Root access for rooted devices and Shizuku for non-rooted devices. Built using Jetpack Compose and Material Design 3.

## Purpose

Network Switch provides control over your device's network modes, supporting 34 different configurations from basic 2G-only to advanced 5G combinations. This includes:

- **Basic Modes**: GSM (2G), WCDMA (3G), LTE (4G), NR (5G) only
- **Combined Modes**: Various 2G/3G/4G/5G combinations
- **Regional Modes**: CDMA support for US carriers, TD-SCDMA for Chinese networks
- **Global Modes**: Network support for international usage

### Use Cases
- Quick network switching through customizable Quick Settings tile
- Battery optimization by selecting power-efficient modes
- Speed optimization by forcing high-performance modes
- Coverage optimization in areas with poor signal quality
- Data usage management with specific network restrictions
- Testing network compatibility and performance

The app provides two methods of operation:
- **Root Method**: Direct system access for rooted devices
- **Shizuku Method**: System access through Shizuku service for non-rooted devices

## Features

- **Network Control**: 34 different network modes including pure and combined configurations
- **Privacy-Focused**: No internet permissions, all operations are local
- **Configurable Toggle Modes**: Choose any two network modes for quick switching
- **Quick Settings Tile**: Shows current and next modes with customizable configuration
- **Dual Control Methods**: Root and Shizuku support for compatibility
- **Modern Material Design 3 Interface**: Clean, intuitive UI with extensive configuration options

## Configuration

### Network Mode Selection
The app supports 34 different network modes:

**Basic Modes:**
- GSM Only (2G)
- WCDMA Only (3G) 
- LTE Only (4G)
- NR Only (5G)

**Combined Modes:**
- 2G/3G combinations with preference settings
- 3G/4G combinations (LTE/WCDMA)
- 4G/5G combinations (NR/LTE)
- Multi-generation support (2G/3G/4G/5G)

**Regional Modes:**
- CDMA support for US carriers (Verizon)
- TD-SCDMA support for Chinese networks
- Global modes for international roaming

### Toggle Configuration
1. Open the app and tap the configuration icon
2. Select **Mode A** and **Mode B** from the dropdown menus
3. Preview your configuration
4. Save the configuration
5. Use the main toggle or Quick Settings tile to switch between modes

## Screenshots

<div align="center">
<img src="media/screenshot_main.jpg" alt="Main Screen" width="240" />
<img src="media/screenshot_settings.jpg" alt="Settings Screen" width="240"/>
</div>

## Requirements

### Root Method
- Rooted Android device (Android 10+)
- Root permissions granted to the app

### Shizuku Method
- Non-rooted Android device (Android 10+)
- Shizuku app installed and running
- ADB or Wireless ADB access to start Shizuku

## Installation
<div align="center">

[<img src="media/get_it_github.png" alt="Get it on GitHub" height="80" align="center">](https://github.com/aunchagaonkar/NetworkSwitch/releases)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it at IzzyOnDroid" height="80" align="center">](https://apt.izzysoft.de/packages/com.supernova.networkswitch)
[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="55" align="center">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/aunchagaonkar/NetworkSwitch/)
[<img src="https://www.openapk.net/images/openapk-badge.png" height="80" align="center">](https://www.openapk.net/network-switch/com.supernova.networkswitch/)
</div>

1. Download the latest APK from the Releases page
2. Install on your Android device
3. Choose your preferred control method:
   - **Root**: Grant root permissions when prompted
   - **Shizuku**: Install Shizuku app and grant permission
4. Configure your preferred network modes in the app
5. Add the "Network Switch" tile to Quick Settings for instant access

## Project Structure

```
app/
├── src/main/java/com/supernova/networkswitch/
│   ├── presentation/          # UI Layer
│   │   ├── ui/
│   │   │   ├── activity/     # Activities
│   │   │   ├── composable/   # Reusable Compose components
│   │   │   └── components/   # Shared UI components
│   │   ├── viewmodel/        # ViewModels with state management
│   │   └── theme/            # Material Design 3 theme
│   ├── domain/               # Business Logic Layer
│   │   ├── model/            # Domain models
│   │   ├── repository/       # Repository interfaces
│   │   └── usecase/          # Business use cases
│   ├── data/                 # Data Layer
│   │   ├── repository/       # Repository implementations
│   │   └── source/           # Data sources
│   ├── service/              # System Services
│   │   ├── NetworkTileService # Smart Quick Settings tile
│   │   ├── RootNetworkControllerService # Root-based network control
│   │   └── ShizukuControllerService     # Shizuku-based network control
│   └── di/                   # Hilt dependency injection modules
├── src/main/aidl/            # AIDL interfaces for IPC
├── build.gradle.kts          # App build configuration
└── proguard-rules.pro        # ProGuard rules

hiddenapi/                    # Android Hidden API access module
├── src/main/aidl/            # AIDL interfaces
└── build.gradle.kts          # Hidden API build config

.github/workflows/            # CI/CD pipelines
├── ci.yml                    # Continuous Integration
└── build-release.yml         # Release workflow

gradle/
├── libs.versions.toml        # Version catalog
└── wrapper/                  # Gradle wrapper

build.gradle.kts              # Root build configuration
settings.gradle.kts           # Project settings
```


## Building from Source

```bash
# Clone the repository
git clone https://github.com/aunchagaonkar/NetworkSwitch.git
cd NetworkSwitch

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## TODO
- [x] Add unit tests for all core components
- [x] Add customizable network mode support (34 modes)
- [x] Implement adaptive app icon, with icon-pack launcher compatibility
- [ ] Implement scheduled/automatic network switching
- [ ] Add network speed monitoring and performance metrics
- [ ] Add multi-language support
- [ ] Add network performance benchmarking tools

## Contributing

Contributions are welcome! Please follow these guidelines:

### Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/feature-name`)
3. Make your changes following the project's code style
4. Test your changes thoroughly
5. Commit your changes (`git commit -m 'Add feature-name'`)
6. Push to the branch (`git push origin feature/feature-name`)
7. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add documentation for public APIs
- Maintain clean architecture principles
- Follow Material Design guidelines for UI changes

### Testing
This project includes a suite of unit tests to ensure code quality and stability.

To run the unit tests, execute the following command from the root of the project:

```bash
./gradlew test
```

**Testing Guidelines:**
- Add unit tests for new functionality
- Test on both rooted and non-rooted devices
- Verify compatibility across different Android versions (10+)
- Test network mode switching with various configurations
- Validate AIDL interface implementations
- Test Quick Settings tile functionality
- Verify configuration persistence and restoration

### Reporting Issues
- Use the GitHub issue tracker
- Provide detailed reproduction steps
- Include device information and Android version
- Attach relevant logs when possible

## LICENSE

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

## Dependencies and Credits

### Core Dependencies
- **Shizuku**: Root-less system API access
- **libsu**: Root access management
- **Hilt**: Dependency injection
- **DataStore**: Modern preferences storage
- **Jetpack Compose**: Modern UI framework
- **Material Design 3**: UI components and theming
- **Kotlin Coroutines**: Asynchronous operations

Special thanks to the Android development community and the maintainers of the open-source libraries used in this project.
