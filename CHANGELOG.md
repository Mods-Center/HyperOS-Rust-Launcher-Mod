# Changelog

## v1.2

2026-11-13

- App drawer search no longer closes the launcher when a number is typed (seen on some CN ROMs). A stock search
  result widget raised a GetX error for number queries and the launcher's crash guard restarted it after a few errors;
  the error is now ignored and the result is shown normally
- Swipe up on the home screen opens the picked search app only when the launcher's own swipe-up setting is "Search"
  (it opened search even with "None" selected)
- HyperLauncherExt works without root when it is built into a ROM as a system app with WRITE_SECURE_SETTINGS (it
  asked for root because it only checked the "modify system settings" permission). When saving still fails, the
  welcome screen shows the exact error so ROM builders can see what is missing
- Icon packs without root: Android 16 runs privileged apps as `priv_app_36` (versioned domain); HyperLauncherExt
  now accepts it and names the exact domain the ROM's SELinux rules must allow (rules for plain `priv_app` don't
  cover `priv_app_36`, see the README)
- HyperLauncherExt 3.8.5

## v1.1

- Dock background is now off by default; turn it on in HyperLauncherExt (Dock)
- Search app list has a "Launcher default" entry that keeps the launcher's own search bar behaviour
- With Google as the search app, the search bar, pull-down and swipe-up search open Google directly from the launcher
  (no helper screen, no delay); other apps open through HyperLauncherExt without the 5-6 second delay
- No black bars at the screen edges when the search bar opens an app
- "Share" is back in the app icon long-press menu and shares the app's APK through the system share sheet
- Swipe-down and swipe-up search open the picked search app, also on CN ROMs without the Xiaomi search app (the
  swipe-up gesture settings list "Search" when a search app is picked)
- New "-1 screen" choice in HyperLauncherExt: ROM default, App Vault or Google Discover (needs the Google app), on
  every ROM including CN (Google Discover works on CN ROMs too)
- The capsule search bar is the default on first boot on every ROM
- Root detection works with more root managers (su outside PATH, KernelSU / APatch locations)
- Icon packs: when the theme folder isn't writable without root, the row now says why (SELinux domain, folder
  permissions or missing rules)
- Recents: the Stack layout can be chosen on every device (it was limited to high-end devices)
- Three-layer blur (recents, app open/close) on every device while "Full animations" is on in HyperLauncherExt
- HyperLauncherExt is signed with v1 + v2 + v3 signatures: fixes "INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION ...
  ArrayList cannot be cast to Certificate[]" when installing it by hand on some ROMs / signature-bypass setups
- Module: removes a separately installed copy of HyperLauncherExt (installed by hand or signed with another key) that
  made the system copy fail with a signature mismatch; its data is kept
- Module: genuine HyperOS 3 no longer passes the HyperOS 4 runtime check (it now also needs libmisqlite3 and
  librust_maml_sdk, which only HyperOS 4 has)
- HyperLauncherExt: new "HyperOS hidden apps" row in Home screen: opens HyperOS's own app hiding (Security app; hidden
  apps open with a pinch-out on the home screen); hidden on ROMs without that screen
- HyperLauncherExt 3.8.3

## v1.0

First public release, for the HyperOS 4 launcher 8.01.02.9239.

- Native Dock with system / soft glass / advanced materials, radius, margin, height and icon offset
- Spacing controls for the search capsule (space below and above)
- Soft glass tuning for the home screen and the dock, with presets
- The search capsule, mic icon, pull-down search and swipe-up search open the chosen search app; Circle to Search on
  every region
- Hide any app, free widget resizing, paid widgets, App Vault choice
- Larger icons, icon highlight on all icons, themed icons on every ROM, icon packs
- Recents blur strength, hide clear-all, full animations on every device
- HyperLauncherExt settings app in 13 languages; works without root as a system app
- Icon packs without root on ROMs that allow writing the theme folder (see "For ROM builders")
- Module: HyperOS 4 check that also recognises ports, privapp allowlists, removes the old dex-launcher mod
