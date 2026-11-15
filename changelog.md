## v1.4 (6)

2026-11-15

- Based on Xiaomi's new launcher 8.01.02.9258
- Any widget can be placed on the App Vault -1 screen
- Recents: hide the status bar, hide the app titles above the cards, custom card corner radius (1-60 dp)
- App drawer blur has its own switch now
- HyperLauncherExt 3.9: settings for all of the above; Home screen backup (export / import the home screen setup,
  root); a warning with a download link when the Google app is not a privileged system app or not the default
  assistant
- HyperLauncherExt: the top bar blur fades in gradually instead of a blurred strip
- Module: HyperLauncherExt is now installed as a normal app (direct install) instead of a mounted system app. That
  fixes it not showing up, showing up as an icon-less stub, or closing right after opening on some root setups.
  It is reinstalled at boot if it goes missing, and removed together with the module. It needs root now (as a system
  app, for ROM builders, it still works without root)

