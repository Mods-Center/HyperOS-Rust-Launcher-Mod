# Changelog

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
- HyperLauncherExt 3.8.2

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
