# Moonlight DS (dual display)

This fork talks to FanGoH Sunshine `sunshine-ds-linux`. It SETUPs `streamid=video/1/0` before ANNOUNCE when the host advertises `MaxVideoStreams=2`.

## Layouts

Settings → Dual display:

- **Auto** — two Android displays (AYN Thor) → TV on the default panel, GamePad on the other. One panel → stacked.
- **Dual panel** — force the second Android display (falls back to stacked).
- **Stacked** — both streams on one screen (phone).
- **Primary only** — stock single stream of the TV.
- **GamePad only (Odin)** — one stream of the virtual GamePad display, fullscreen. Does not encode the TV. Use this on a single-screen handheld.

## Fill (TV vs GamePad)

Settings → **TV / primary fill** and **GamePad / second screen fill** are independent.

- **Fit** keeps the stream aspect (letterbox).
- **Stretch to fill** scales that stream to the panel. Default for GamePad is stretch so a 1080p virtual GamePad fills Thor’s bottom screen instead of cropping to 1080×1240.

When GamePad fill is stretch and second-screen resolution is Auto, the GamePad bitstream stays on the TV resolution (1080p) so Sunshine does not letterbox the 16:9 capture into the Thor panel’s native mode.

Host `packet_to_unit` is linear `x/width`. Do **not** unletterbox a Stretch panel (`ref=1239x1079` from an old APK). Newer builds send stream-space 1920×1080 refs (`98700dcd`); that is the same linear map. Game Mode checkpoint **`checkpoint-2026-09-18-gamepad-xtest`**.

## Point at sunshine-ds, not Decky Sunshine

Plasma daily dual-screen: add the PC as `HOST:48100`. Production Decky Sunshine stays on `:47989`.

Game Mode on the Steam Machine is **`:48200`** (`sunshine-ds-kms`). **THE standard is `checkpoint-2026-09-18-gamepad-xtest`.** White De-FanGoH tile, not the grey `:48100` warning tile.

## Build (this machine)

Toolchain lives under `/home` (SteamOS root is too small):

```bash
export JAVA_HOME=$HOME/.local/jdk-17
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
cd ~/code/moonlight-android
./gradlew :app:assembleNonRootDebug
```

APK: `app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk`

Sideload:

```bash
adb install -r app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk
```

Thor: enable Wireless debugging, then `adb connect IP:PORT` and `adb install -r` this APK.
