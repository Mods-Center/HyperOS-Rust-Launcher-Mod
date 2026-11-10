# HyperOS 4 Rust Launcher Mod

A modded **HyperOS 4 system launcher** (the new Rust/Flutter `com.miui.home`, base **8.01.02.9239**) plus
**HyperLauncherExt**, a settings app that configures it live. It's delivered as a Magisk / KernelSU / APatch module.

> HyperOS 4's launcher has no dex code, so it can't be modded with smali patches. All launcher changes are binary
> patches of the compiled Dart and Rust code, plus a small native helper (`libkashidock.so`) that reads the settings
> written by HyperLauncherExt.

## Features

### Dock
- Native **Dock** background on the home screen, with three materials: system default, soft glass, advanced material
- Corner radius, horizontal margin and height
- Vertical offset for the dock icons
- **Space below search capsule**: lifts the search capsule and page dots away from the dock icons
- **Space above search capsule**: ends the icon grid higher, leaving more room above the capsule

### Soft glass
- Full soft-glass tuning for the **home screen** and the **dock** separately: blur, luminance, saturation, brightness,
  opacity, refraction, thickness, background saturation and brightness, burn, edge, edge feather, reflection,
  highlight and darken
- Export and import presets (file, share, clipboard)

### Search
- The search capsule, its mic icon, **pull-down search** and **swipe-up search** open the search app you pick
  (Google, ChatGPT, Perplexity, ...), on its search or voice screen when it has one
- All search bar styles are available on every ROM
- **Circle to Search** option on every region

### Home screen & widgets
- **Hide any app** through the launcher's own Hide apps page
- **Free widget resizing**: every widget can be resized in both directions, down to one cell
- Paid / theme-store widgets usable
- Choose the widget picker: MIUI App Vault or Global App Vault

### Icons
- Icons **larger than the stock maximum** (extra size up to +15%)
- HyperOS **icon highlight** (glass rim and light) on third-party icons too
- **Themed (monochrome) icons** on every ROM
- **Icon packs** from installable icon-pack apps, applied through the theme engine (needs root), with a refresh
  for newly installed apps
- Shortcut to the launcher's own icon size and style page

### Recents & performance
- Recents background blur strength (0-300%)
- Hide the clear-all button
- **Full animations on every device**: lite and low-end device checks removed

### HyperLauncherExt
- miuix-style settings app with a collapsing blurred header, animations, and -/+ buttons, number entry and reset on
  every slider
- Works **without root** when installed as a system app (the module does that). With root it can also apply icon
  packs
- Available in 13 languages: English, Arabic, Chinese (Simplified), French, German, Hindi, Indonesian, Italian,
  Portuguese (Brazil), Russian, Spanish, Turkish, Vietnamese
- Diagnostics page showing the launcher helper's log

## Requirements

- **HyperOS 4** with the Rust/Flutter launcher, arm64. The module checks the version props and, because many ports
  report the base ROM's version, also the HyperOS 4 launcher runtime (`hyos_spawner`, `libhyper_os_*`). If neither
  is found it asks before installing (Vol+ install / Vol- cancel).
- Magisk, KernelSU or APatch.
- **Signature verification disabled** (core patch) on stock ROMs. The patched launcher can't carry Xiaomi's
  signature.

## Install

1. Flash `HyperOS4_RustLauncherMod_vX.Y.zip` from the [releases](../../releases) in Magisk / KernelSU / APatch.
2. Reboot.
3. Open **HyperLauncherExt**, change settings, and tap the restart button at the top when it lights up.

The module removes the older dex-launcher mod (`miui_launcher_mod`) if it's installed, because both replace the same
launcher. To uninstall, remove the module and reboot; the stock launcher comes back.

## For ROM builders

Both APKs can be built into a ROM instead of using the module.

**Install locations** (same layout the module uses):

| File | Location |
|---|---|
| patched launcher | replaces `/product/priv-app/MiuiHome/MiuiHome.apk` (or wherever `pm path com.miui.home` points) |
| `HyperLauncherExt.apk` | `/product/priv-app/HyperLauncherExt/HyperLauncherExt.apk` |
| permission allowlists | `/product/etc/permissions/` (both files from `magisk-module/system/product/etc/permissions/`) |

Files `0644`, folders `0755`, owner `root:root`, label `u:object_r:system_file:s0`. The launcher APK is not
signed with Xiaomi's key, so signature verification must be relaxed (core patch) as for the module.

**Root-free features.** Installed as a privileged app, HyperLauncherExt changes every launcher setting, restarts the
launcher and reads its log **without root**. Only **icon packs** need more: they write `/data/system/theme/icons`,
and on stock HyperOS SELinux lets only system daemons and Theme Manager write there (`theme_data_file`), not apps,
even platform-signed ones. To enable icon packs without root, add to `system_ext/etc/selinux/system_ext_sepolicy.cil`:

```
(allow priv_app theme_data_file (dir (read write open getattr search add_name remove_name)))
(allow priv_app theme_data_file (file (create read write open getattr setattr unlink rename)))
```

Use `platform_app` instead of `priv_app` if you sign HyperLauncherExt with the ROM's platform key. If your ROM boots a
precompiled policy (`precompiled_sepolicy` + `*.sha256` in vendor/odm), the changed hash makes init recompile it from
the CIL files on the next boot. HyperLauncherExt detects the permission by itself, so the icon pack rows appear and
work without root; it then clears the launcher cache through `DELETE_CACHE_FILES` (already in its allowlist).

## Repository layout

| Path | What |
|---|---|
| `HyperLauncherExt/` | Source of the settings app (plain Android views; see its README) |
| `magisk-module/` | Module scripts (Dynamic Installer by @BlassGO) and permission allowlists; the APKs go into `files/` |
| `launcher-patches/` | HyperChanger-derived parts (AGPL): native helper source and the Dock / soft glass patch description |

## Credits

- **🅺🅰🆂🅷🅸** ([@kakashi1v1](https://t.me/kakashi1v1)): mod author
- **[HyperChanger](https://github.com/ColdP/HyperChanger)** by ColdP (btm_m): reference and code implementation help
  for the native Dock, Dock materials, soft glass and launcher hooks (AGPL-3.0)
- **Dynamic Installer** by @BlassGO: module installer framework
- Claude (Anthropic): help with reverse engineering and code

See [NOTICE.md](NOTICE.md) for licensing and third-party notices.

## Disclaimer

Not affiliated with or endorsed by Xiaomi. HyperOS, MIUI and the launcher are Xiaomi's property. You use this mod at
your own risk; keep a backup and a way to remove modules from recovery or safe mode.
