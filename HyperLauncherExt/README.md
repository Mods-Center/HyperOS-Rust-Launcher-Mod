# HyperLauncherExt (com.miui.home.kashi.ext)

Settings app for the patched HyperOS 4 launcher. Plain Android views (no AndroidX/Compose), built with
`./build.sh` (aapt2 + javac + d8 + jarsigner, see below) into `HyperLauncherExt.apk`.

## Layout

| Path | What |
|---|---|
| `res/xml/prefs_main.xml` | Main page. Every section, switch, slider, dropdown and link is declared here. |
| `res/xml/prefs_glass.xml` | Soft glass sub page (opened for home and dock with `${prefix}` / `${name}` args). |
| `src/.../prefs/PrefScreen.java` | Renders the XML (element and attribute reference in its javadoc). |
| `src/.../prefs/SettingsStore.java` | Settings.System read/write (direct as system app, else root), launcher restart, helper cache. |
| `src/.../prefs/Host.java` | Services the activity gives to modules (ui, store, pages, toasts, activity results). |
| `src/.../modules/` | Feature actions: `SearchModule`, `IconPackModule` (+ `IconPacks`), `GlassModule`, `AboutModule`. Registered in `Modules.java`. |
| `src/.../ui/Ui.java` | miuix-style widget kit (cards, switches, sliders, dropdowns, sheets, blurred header, animations). |
| `src/.../MainActivity.java` | Root screen, page stack and transitions. |
| `src/.../SearchActivity.java` | Trampoline the launcher starts for search bar taps (SEARCH_OPEN / VOICE_GO). |

## Adding a setting

1. Add a row to `res/xml/prefs_main.xml`, e.g.
   `<SwitchPreference key="kashi_new_key" title="…" summary="…" defaultValue="true" />`
2. If it needs code (pickers, files, …) use `<Preference action="my_action" …/>`, implement `modules/Action`
   and register `my_action` in `modules/Modules.java`.
3. Launcher-side keys must also exist in the helper (`libkashidock.so`); the start-up cache order is
   `SettingsStore.HELPER_KEYS` and must match the helper.

## Translations

All user-visible text is in `res/values/strings.xml` (English) with translations in `values-ar`, `de`, `es`, `fr`,
`hi`, `in` (Indonesian), `it`, `pt-rBR`, `ru`, `tr`, `vi` and `zh-rCN`. The preference XML pages reference them as
`@string/...`; Java code uses `L.s(context, "name", args...)`. A new string must be added to every `values*` folder.

## Building

```
ANDROID_JAR=/path/to/android-35/android.jar ./build.sh
```

See the header of `build.sh` for the signing options. The release APK in the Magisk module is signed with the
author's own key; a self-built APK must be signed with the same key to update it, otherwise uninstall it first.
