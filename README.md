# Echo In Mirror [![Release](https://github.com/EchoInMirror/EchoInMirror/actions/workflows/release.yml/badge.svg)](https://github.com/EchoInMirror/EchoInMirror/actions/workflows/release.yml) ![GitHub release (with filter)](https://img.shields.io/github/v/release/EchoInMirror/EchoInMirror) ![GitHub (Pre-)Release Date](https://img.shields.io/github/release-date-pre/EchoInMirror/EchoInMirror) ![GitHub last commit (by committer)](https://img.shields.io/github/last-commit/EchoInMirror/EchoInMirror)

An open source DAW (Digital Audio Workstation) written in pure Kotlin.

## Screenshots

![image](screenshots/0.png)

## Features

- [x] VST/VST3/AU plugin support
- [x] Audio effects
- [x] Audio sample support
- [x] ASIO support
- [x] MIDI edit
- [x] Latency compensation
- [x] Audio edit
- [ ] Audio recording
- [ ] Midi input
- [ ] Plugin api
- [ ] CLAP and ARA plugin support
- [ ] Android support
- [ ] iOS support

## Requirement

- Java 21

## Build

```bash
git clone https://github.com/EchoInMirror/EchoInMirror.git

cd EchoInMirror

gradlew :daw:shadowJar
```

## Run

```bash
java --enable-preview -jar daw/build/libs/daw.jar
```

## Author

Shirasawa

## License

[AGPL-3.0](./LICENSE)
