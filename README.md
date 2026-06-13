# RomRaider

RomRaider is a free, open source tuning suite created for viewing, logging and
tuning of modern Subaru Engine Control Units. The intuitive tuning interface
and powerful datalogger are modelled to be familiar to experienced professional
tuners while providing all the power of expensive commercial products, without
license fees.

## 2026 modernization

This fork has been brought fully up to date for a modern Java runtime and a
refreshed user interface:

- **Java 17+ toolchain** — builds and runs on current JDKs (developed and
  verified against JDK 17 and 21). The legacy 32-bit Java 6 requirement is
  gone; RomRaider now runs on a standard 64-bit JVM.
- **Modern "2026" UI** built on [FlatLaf](https://www.formdev.com/flatlaf/):
  six selectable themes (Dark, Light, Carbon, Darcula, Arctic, IntelliJ) with
  animated theme switching, a signature accent color, rounded corners, pill
  scrollbars, an embedded title-pane menu bar, and window fade-in. Pick a theme
  under **View → Theme**; your choice is saved in `settings.xml`.
- **All third-party libraries updated** to current releases: FlatLaf,
  JFreeChart 1.5.6, JNA 5.19.1, jSerialComm 2.11.4, JAMA 1.0.3, reload4j
  (log4j replacement), JEP 2.4.2, com4j 2.1, hid4java 0.8.0, JUnit 4.13.2.
- **Native-free drivers written in-house** to retire end-of-life libraries:
  - The 3D table view no longer needs Java3D — it uses a pure-Java2D surface
    renderer (drag to rotate, wheel to zoom, click to pick, arrow keys to edit).
  - The Phidget InterfaceKit logger plugin no longer needs the phidget21 JNI
    wrapper — it talks to the hardware directly over USB HID via hid4java, which
    bundles native support for every platform. No separate driver install.

The only remaining native library is RomRaider's own J2534 ECU-communication
shim (`lib/linux/*/j2534.so`).

## Requirements

- A Java Runtime Environment, **version 17 or newer** (64-bit).
- For building: a JDK 17+ and [Apache Ant](https://ant.apache.org/).

## Building

From the repository root:

```
ant build        # compile and build RomRaider.jar for the current platform
ant standalone   # build the distributable ZIP packages
ant all          # full rebuild, including installers (needs 3rdparty tools)
ant unittest     # run the unit test suite
ant help         # list all available targets
```

Built artifacts are written to the `build/` directory.

See **`docs/BUILDING.md`** for a complete, step-by-step build guide for Windows
and Linux. For IDE setups, see `docs/Building_RomRaider.txt` (Eclipse) and
`docs/Building_RomRaider_VSCode.md` (Visual Studio Code).
A reproducible build environment is also described in the `Dockerfile`
(Debian 13 + OpenJDK 21).

## Links

 - https://www.romraider.com/
 - https://www.romraider.com/forum/
 - https://github.com/RomRaider/RomRaider (upstream project)
