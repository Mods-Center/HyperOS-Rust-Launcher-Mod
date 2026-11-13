<div align="center">

# HyperOS 4 Rust Launcher Mod

[![Release](https://img.shields.io/github/v/release/Mods-Center/HyperOS-Rust-Launcher-Mod?style=for-the-badge&label=release&color=0a84ff)](https://github.com/Mods-Center/HyperOS-Rust-Launcher-Mod/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Mods-Center/HyperOS-Rust-Launcher-Mod/total?style=for-the-badge&color=30d158)](https://github.com/Mods-Center/HyperOS-Rust-Launcher-Mod/releases)
[![Stars](https://img.shields.io/github/stars/Mods-Center/HyperOS-Rust-Launcher-Mod?style=for-the-badge&color=ffd60a)](https://github.com/Mods-Center/HyperOS-Rust-Launcher-Mod/stargazers)
[![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/kashis_cringey_stuffs)

</div>

A modded **HyperOS 4 system launcher** (the new Rust/Flutter `com.miui.home`, base **8.01.02.9248**) plus
**HyperLauncherExt**, a settings app that configures it live. It's delivered as a Magisk / KernelSU / APatch module.

> HyperOS 4's launcher has no dex code, so it can't be modded with smali patches. All launcher changes are binary
> patches of the compiled Dart and Rust code, plus a small native helper (`libkashidock.so`) that reads the settings
> written by HyperLauncherExt.

## Built with AI

Modding a Flutter launcher is very different from the usual smali work: the whole UI is compiled Dart (AOT machine
code with no class names in the code itself), so every change means finding the right function in about 30 MB of
ARM64 code and patching it by hand. Because of that difficulty, **this mod was made with heavy help from AI**
(Claude by Anthropic): it reverse engineered the launcher, wrote the binary patches, the native helper and most of
HyperLauncherExt, under the direction of the mod author, who tested every build on a real device. Bugs are
possible; please report them with a log (HyperLauncherExt has a diagnostics page).

## Screenshots
<p align="center">
  <img src="https://github.com/user-attachments/assets/ebc307e2-e53e-467a-a462-b919fb941720" width="23%" alt="Rust Launcher"/>
  <img src="https://github.com/user-attachments/assets/1667bea4-917d-4eb6-8e7e-e75e742afac1" width="23%" alt="Rust Launcher"/>
  <img src="https://github.com/user-attachments/assets/2bcffe04-7fce-4426-a0ef-98cbc5dc3938" width="23%" alt="Rust Launcher"/>
  <img src="https://github.com/user-attachments/assets/d1f05c59-1243-46d4-8648-5f309d3ebe4c" width="23%" alt="Rust Launcher"/>
  <br/>
  <img src="https://github.com/user-attachments/assets/78c71228-fdf2-4a72-8d9f-0c5ce7519bcf" width="23%" alt="Rust Launcher"/>
  <img src="https://github.com/user-attachments/assets/111b8de8-4310-4379-bb3c-38dca40daadb" width="23%" alt="Rust Launcher"/>
  <img src="https://github.com/user-attachments/assets/e3907855-97d8-481f-86f8-cd381ddba73f" width="23%" alt="Rust Launcher"/>
  <img src="https://github.com/user-attachments/assets/03b21a37-5c37-4444-bb30-ed7739499dd9" width="23%" alt="Rust Launcher"/>
  <br/>
  <img src="https://github.com/user-attachments/assets/e4ad1a00-62c3-412d-ac3a-a6efe21bf90c" width="23%" alt="Rust Launcher"/>
  <img src="https://github.com/user-attachments/assets/3d0a07d4-989d-463f-a7dd-9df58ae6a250" width="23%" alt="Rust Launcher"/>
</p>

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
- Works on CN ROMs too, **without the Xiaomi search app**; "Search" shows up in the swipe-up gesture options
- **Google** opens directly from the launcher, with no helper screen and no delay; a "Launcher default" entry keeps
  the stock behaviour
- The capsule search bar is the default on first boot on every ROM, and all search bar styles are available
- **Circle to Search** option on every region

### Home screen & widgets
- **-1 screen** choice on every ROM, CN included: ROM default, **App Vault** or **Google Discover** (needs the
  Google app)
- **Share** in the app icon long-press menu: sends the app's APK through the system share sheet
- **Hide any app** through the launcher's own Hide apps page; shortcut to HyperOS's own hidden apps (Security app, opened with a pinch-out)
- **Free widget resizing**: every widget can be resized in both directions, down to one cell
- Paid / theme-store widgets usable
- Choose the widget picker: MIUI App Vault or Global App Vault

### Icons
- Icons **larger than the stock maximum** (extra size up to +15%)
- HyperOS **icon highlight** (glass rim and light) on third-party icons too
- **Themed (monochrome) icons** on every ROM
- **Icon packs** from installable icon-pack apps, applied through the theme engine (needs root, or a ROM rule; see
  "For ROM builders"), with a refresh for newly installed apps
- Shortcut to the launcher's own icon size and style page

### Recents & performance
- **Stack** recents layout on every device
- Recents background blur strength (0-300%)
- **Three-layer blur** (recents, app open/close) on every device while "Full animations" is on
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
- No background service: it only runs while you open it (or for a moment when a search app is launched)

## Requirements

- **HyperOS 4** with the Rust/Flutter launcher, arm64. The module checks the version props and, because many ports
  report the base ROM's version, also the HyperOS 4 launcher runtime (`hyos_spawner`, `libhyper_os_*`, `libmisqlite3`,
  `librust_maml_sdk`). If neither
  is found it asks before installing (Vol+ install / Vol- cancel).
- **HyperOS 3 is not supported.** This launcher needs the HyperOS 4 Flutter engine and Rust runtime; on HyperOS 3
  the module stops and asks before installing. If you want to try it on HyperOS 3 anyway, the experimental
  [HyperOS 3 Rust Bridge](https://github.com/kakashi-1v1/HyperOS3-Rust-Bridge) adds the missing runtime pieces; read
  its status page first (some HyperOS 4 effects don't work there), and flash the bridge before this module.
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

On Android 16 (HyperOS 4) privileged apps run in a versioned domain, for example `priv_app_36`, and rules for
`priv_app` don't cover it. Write the rules for the exact domain the icon pack row shows:

```
(allow priv_app_36 theme_data_file (dir (read write open getattr search add_name remove_name)))
(allow priv_app_36 theme_data_file (file (create read write open getattr setattr unlink rename)))
```

Use `platform_app` (or `platform_app_36`) instead if you sign HyperLauncherExt with the ROM's platform key. If your ROM boots a
precompiled policy (`precompiled_sepolicy` + `*.sha256` in vendor/odm), the changed hash makes init recompile it from
the CIL files on the next boot. HyperLauncherExt detects the permission by itself, so the icon pack rows appear and
work without root; it then clears the launcher cache through `DELETE_CACHE_FILES` (already in its allowlist).

If the icon pack row still says root is needed, its text now shows the reason:

| Reason shown | Fix |
|---|---|
| `app runs as untrusted_app` (or another domain) | install HyperLauncherExt in `priv-app` as above; with the platform key, write the rules for `platform_app` |
| `/data/system/theme is mode 775` | the folder must be `0777` (`system_theme:system_theme`); SELinux rules can't override Unix permissions |
| `SELinux denies priv_app_36 ...` (any domain) | no rules for that exact domain are loaded: write them for the domain shown (on Android 16 `priv_app_36`, not `priv_app`), check the CIL edit and that the precompiled policy was rebuilt |

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
- **Claude** (Anthropic): reverse engineering, launcher patches, native helper and most of HyperLauncherExt (see
  "Built with AI")

See [NOTICE.md](NOTICE.md) for licensing and third-party notices.

## Disclaimer

Not affiliated with or endorsed by Xiaomi. HyperOS, MIUI and the launcher are Xiaomi's property. You use this mod at
your own risk; keep a backup and a way to remove modules from recovery or safe mode.

---

<div align="center">

Made by **[🅺🅰🆂🅷🅸](https://t.me/kakashi1v1)** · part of **[Mods Center](https://github.com/Mods-Center)**

[Website](https://modscenter.org) · [Channel](https://t.me/kashis_cringey_stuffs) · [Support group](https://t.me/bootloop_discussion) · [Report a bug](https://github.com/Mods-Center/HyperOS-Rust-Launcher-Mod/issues)

</div>
