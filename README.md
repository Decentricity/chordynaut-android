# Chordynaut Android

Native Android wrapper for the `chordynaut` web app from Berrry.

## Download

- Latest debug APK: `https://raw.githubusercontent.com/Decentricity/chordynaut-android/main/releases/0.1.0/chordynaut-v0.1.0-debug.apk`

## What It Is

Chordynaut is a mobile-first chord instrument and loop sketchpad. It combines:

- a chord grid for selecting harmonies
- a vertical strum lane for picking notes from the active harmony
- a synth engine with waveform and ADSR control
- metronome, performance recording, loop recording, and overdub
- tonic/mode control with melody-mode note lanes when no chord is held

This Android project bundles the current live web app into a WebView wrapper instead of rewriting the synth natively.

## Project Layout

- `app/`: Android wrapper source
- `app/src/main/assets/www/`: bundled Chordynaut web app files pulled from Berrry

## Build

```sh
./gradlew assembleDebug
./gradlew bundleRelease
```

On the Android/Termux Debian build host, use the local `aapt2` override if needed:

```sh
./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```
