# Chordynaut Android

Native Android wrapper for Chordynaut.

## Download

- [Latest debug APK](https://raw.githubusercontent.com/Decentricity/chordynaut-android/main/releases/0.1.0/chordynaut-v0.1.0-debug.apk)

## What It Is

Chordynaut is a mobile-first chord instrument and loop sketchpad. It is built to be played, not navigated like a normal app. The screen is split into two performance surfaces:

- the chord grid on the left selects the harmony
- the vertical strum lane on the right plays notes from the active harmony or melody lane

Around that core instrument, the app gives you:

- a synth engine with waveform and ADSR control
- tonic and mode selection
- metronome and tempo controls
- performance recording
- loop recording and overdub
- microphone sampling as an alternate voice source

This Android project bundles the current live web app into a WebView wrapper instead of rewriting the synth natively.

## How To Play

1. Pick a tonic and a mode if you want to change the harmonic center.
2. Press a chord button on the left to set the current chord.
3. Strum or tap the right-side note lane to play notes from that chord across octaves.
4. Turn on `Latch` if you want the chord to stay held while you keep playing the note lane.
5. Adjust waveform, ADSR, and mic/sample voice options to shape the sound.
6. Use the metronome and loop controls when you want to build repeating phrases.

When no chord is held, Chordynaut can switch into melody behavior using the selected tonic and mode, so the right lane still works as a playable instrument.

## Performance Workflow

- `⏺` starts performance recording after the countdown.
- `▶` replays the recorded performance.
- `🔁` starts a loop pass after the countdown.
- `⬤` overdubs onto an active loop immediately.
- `C` clears the current loop.
- `⬇️` exports recorded material.

## Touch Workflow

- tap chord pads with one finger
- drag or tap across the strum lane with another finger
- use the settings and config controls between takes
- keep the device in landscape for the intended instrument layout

## Project Layout

- `app/`: Android wrapper source
- `app/src/main/assets/www/`: bundled Chordynaut web app files

## Build

```sh
./gradlew assembleDebug
./gradlew bundleRelease
```

On the Android/Termux Debian build host, use the local `aapt2` override if needed:

```sh
./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```
