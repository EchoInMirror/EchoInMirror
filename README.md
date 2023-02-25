# Echo In Mirror [![Release](https://github.com/EchoInMirror/EchoInMirror/actions/workflows/release.yml/badge.svg)](https://github.com/EchoInMirror/EchoInMirror/actions/workflows/release.yml) ![Lines of code](https://img.shields.io/tokei/lines/github/EchoInMirror/EchoInMirror)

A open source DAW (Digital Audio Workstation) written in pure Kotlin.

## Screenshots

![image](screenshots/0.png)

## Features

- [x] VST/VST3 support
- [x] Audio effects
- [x] ASIO support
- [x] MIDI edit
- [ ] Audio edit
- [ ] Audio recording
- [ ] Midi input
- [ ] Plugin api
- [ ] Android support

## Requirement

- Java 19

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
