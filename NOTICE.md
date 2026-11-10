# NOTICE

HyperOS 4 Rust Launcher Mod
Copyright 2026 🅺🅰🆂🅷🅸 (@kakashi1v1)

## License

This project is distributed under the **GNU Affero General Public License, version 3** (see [LICENSE](LICENSE)),
because parts of the launcher modifications are derived from HyperChanger, which is AGPL-3.0.

## HyperChanger

HyperChanger, https://github.com/ColdP/HyperChanger
Copyright 2026 btm_m (ColdP), GNU AGPL-3.0-or-later

These launcher-side features were ported from HyperChanger's HyperOS 4 launcher hooks and adapted to static
binary patches plus a native settings helper:

- native Dock gates, Dock material selection, Dock radius / padding / height
- soft glass parameter adjustment (HyperChanger "home full glass": field mapping, value ranges, dock/home records)
- Dock icon offset

Adaptation: HyperChanger patches the launcher at runtime through LSPosed and its own config file. This mod writes
equivalent code islands into the launcher's `libapp.so` ahead of time, and reads its settings from
`Settings.System` keys through `libkashidock.so`. The helper source and a description of these patches are in [launcher-patches/](launcher-patches/).

## Third-party components

- **Dynamic Installer** by @BlassGO (`magisk-module/META-INF`): module installer framework, including its bundled
  busybox and helper binaries, each under its own license.
- **HyperOS launcher** (`com.miui.home`): property of Xiaomi. The module contains a modified copy of it; no
  ownership is claimed.

## Trademarks

Xiaomi, HyperOS and MIUI are trademarks of Xiaomi Inc. This project is not affiliated with or endorsed by Xiaomi.
