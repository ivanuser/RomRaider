# Building RomRaider (Windows & Linux)

This is a complete, step-by-step guide to building RomRaider from source on
Windows and Linux. It targets the modernized build (Java 17+ and Apache Ant).

If you use an IDE, see also `Building_RomRaider.txt` (Eclipse) and
`Building_RomRaider_VSCode.md` (VS Code).

---

## 1. Overview

RomRaider builds with **Apache Ant** using the `build.xml` in the repository
root. The build:

- compiles the Java sources (targeting **Java 17** bytecode),
- generates `RomRaider.jar` with a manifest classpath pointing at the bundled
  libraries in `lib/`,
- and can package standalone ZIPs and (optionally) native installers.

All third-party libraries are bundled in `lib/common` (cross-platform) and
`lib/<os>` (platform-specific). Native code for JNA, com4j, hid4java and
jSerialComm is carried inside those jars, so there is nothing extra to install
for them.

| Output | Location |
| --- | --- |
| Compiled classes | `build/classes/` |
| Runnable jar | `build/<os>/lib/RomRaider.jar` |
| Standalone ZIP | `build/dist/<os>/RomRaider<version>-<os>.zip` |
| Installers (optional) | `build/dist/` |

---

## 2. Prerequisites (both platforms)

1. **JDK 17 or newer (64-bit).**
   Recommended: [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17)
   or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/).
   A JRE is not enough — you need the full JDK (it provides `javac`).

2. **Apache Ant 1.10.x.**
   Download from <https://ant.apache.org/bindownload.cgi>.

3. **Git** (to clone the repository) — <https://git-scm.com/downloads>.

---

## 3. Building on Linux

### 3.1 Install the tools

On Debian/Ubuntu:

```bash
sudo apt-get update
sudo apt-get install openjdk-17-jdk ant git
```

On Fedora/RHEL:

```bash
sudo dnf install java-17-openjdk-devel ant git
```

Verify:

```bash
java -version     # should report 17 (or newer)
javac -version
ant -version
```

If `java -version` reports an older JDK, set `JAVA_HOME` to your JDK 17+
installation and put its `bin` on your `PATH`:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

### 3.2 Clone and build

```bash
git clone https://github.com/RomRaider/RomRaider.git
cd RomRaider

ant build          # compile + build RomRaider.jar
```

### 3.3 Run what you built

```bash
ant standalone                     # produce the distributable ZIP
cd build/dist/linux
unzip RomRaider*-linux.zip
cd RomRaider
chmod +x run.sh
./run.sh                           # launches the editor
```

`run.sh` automatically selects the 64-bit native library path and writes a log
to `~/.RomRaider/romraider_sout.log`.

---

## 4. Building on Windows

### 4.1 Install the tools

1. Install a **64-bit JDK 17+** (Temurin or Oracle). During install, let it set
   `JAVA_HOME`, or set it manually afterwards (see 4.2).
2. Download **Apache Ant**, unzip it to a folder, e.g. `C:\ant`.
3. Install **Git for Windows**.

### 4.2 Set environment variables

Open *Settings → System → About → Advanced system settings → Environment
Variables* and add/confirm:

- `JAVA_HOME` = your JDK folder, e.g. `C:\Program Files\Eclipse Adoptium\jdk-17`
- `ANT_HOME` = where you unzipped Ant, e.g. `C:\ant`
- Append to `Path`: `%JAVA_HOME%\bin` and `%ANT_HOME%\bin`

Open a **new** Command Prompt (so it picks up the new variables) and verify:

```bat
java -version
javac -version
ant -version
```

### 4.3 Clone and build

```bat
git clone https://github.com/RomRaider/RomRaider.git
cd RomRaider

ant build
```

### 4.4 Run what you built

```bat
ant standalone
cd build\dist\windows
```

Unzip `RomRaider<version>-windows.zip`, then in the extracted `RomRaider`
folder double-click `run.bat` (or run it from a Command Prompt). It launches the
editor; edit `run.bat` to start the logger or full-screen/touch modes instead.

---

## 5. Ant targets reference

Run `ant <target>` from the repository root:

| Target | What it does |
| --- | --- |
| `help` | List all targets (default if you just run `ant`). |
| `build` | Compile and build `RomRaider.jar` for Windows and Linux. |
| `build-linux` / `build-windows` | Build the jar for a single platform. |
| `rebuild` | `clean` then `build`. |
| `standalone` | Rebuild and produce the distributable ZIP packages. |
| `installer` | Build native installers (requires the tools in section 6). |
| `all` | Full rebuild + installers + standalone ZIPs. |
| `unittest` | Compile and run the JUnit test suite. |
| `javadoc` | Generate the API documentation. |
| `clean` | Delete the `build/` directory. |

### Running the unit tests

```bash
ant unittest
```

---

## 6. Optional: building installers

The `installer` and `all` targets build native installers and need two extra
tools placed under `3rdparty/` (see `3rdparty/README.txt`):

- **IzPack** standalone compiler → `3rdparty/IzPack/izpack-standalone-compiler.jar`
- **launch4j** → `3rdparty/launch4j/launch4j.jar`

Without these tools, `ant build` and `ant standalone` still work fully; only the
installer step is skipped/disabled.

---

## 7. Reproducible build with Docker

A ready-to-use build environment is described in the `Dockerfile`
(Debian 13 + OpenJDK 21). From the repository root:

```bash
docker build -t romraider-builder .
docker run --rm -v "$PWD":/home/romraider/RomRaider -w /home/romraider/RomRaider \
    romraider-builder ant standalone
```

---

## 8. Troubleshooting

- **`javac` not found / wrong Java version** — you have a JRE or an old JDK on
  the `PATH`. Install a JDK 17+ and point `JAVA_HOME`/`PATH` at it.
- **`Unable to create javax script engine for javascript`** — you are on an old
  copy of `build.xml`; the current build no longer uses the removed Nashorn
  engine. Pull the latest sources.
- **`lib/windows does not exist`** — the `lib/windows` directory must be present
  (it is kept in the repo even though it is normally empty). Pull the latest
  sources; do not delete it.
- **launch4j taskdef warning** — harmless unless you are building installers;
  see section 6.
