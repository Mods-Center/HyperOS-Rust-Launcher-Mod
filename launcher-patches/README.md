# Launcher patches derived from HyperChanger

This folder holds the parts of the mod that are derived from
[HyperChanger](https://github.com/ColdP/HyperChanger) (AGPL-3.0, Copyright 2026 btm_m / ColdP), as required by
its license:

- `helper/`: source of `libkashidock.so`, the native helper inside the patched launcher (AGPL-3.0-or-later)
- this file: the HyperChanger-derived code patches in the launcher's `libapp.so` (native Dock and soft glass)

The mod's other launcher changes are original work, not based on HyperChanger, and are not part of this folder:
search, capsule spacing, widgets, icons, recents and animations.

## How the launcher is patched

HyperOS 4's launcher (`com.miui.home` 8.01.02.9239) is Dart AOT code in `lib/arm64-v8a/libapp.so`. HyperChanger
changes it at runtime through LSPosed. This mod applies equivalent changes ahead of time:

1. The executable LOAD segment of `libapp.so` is extended over the zero padding after `.text`. That area holds the
   **code islands** (from `0x1c46800`) and a **data page** (`0x1c4c000`).
2. Each patch site's instruction becomes a branch to its island. The island reads a value from the data page, then
   either runs the stock instruction or applies the setting, and jumps back. Islands only use Dart's scratch
   registers (x16, x17, v31), never call into Dart, and fall back to stock behaviour when a value is unset.
3. `libapp_launcher.so` gets `libkashidock.so` as a dependency: one `DT_NEEDED` name is rewritten in place, and the
   helper loads the original library itself. The helper reads `Settings.System` keys (`kashi_dock_*`,
   `kashi_glass_*`) through the launcher's own HyperOS settings API and writes the data page (`struct page` in
   `helper/kashidock.c`). The glass records start at `0x200`, 80 bytes per parameter: home record, then dock record.

Offsets are for 8.01.02.9239. In other builds the same functions are found by their Dart symbol names (the build
ships them in `.gnu_debugdata`).

## Native Dock

Gates switch the launcher's own Dock background on (off by default since v1.1: `kashi_dock_enable` = 1 turns it on). The material is chosen by redirecting the background's
allocation stub to the system default, soft glass (`GlassOs4Background`) or advanced material
(`MiuiXOs3BlurBackground`). Radius, horizontal margin, height and dock icon offset come from the data page.

| Patch | Function | 9239 offset | Data page |
|---|---|---|---|
| DeviceConfig.supportShowDockBackground | `DeviceConfig.supportShowDockBackground` | `0xf53f08` | `enable` (gate) |
| _DockBackgroundViewState.build | `_DockBackgroundViewState.build` | `0x10d3a84` | `enable` (gate) |
| _HotseatWidgetState.build capability gate | `_HotseatWidgetState.build` closure | `0x10dc478` | `enable` (gate) |
| HotseatLayerGetxController.backgroundLayout no-label | `HotseatLayerGetxController.backgroundLayout` | `0x1772370` | `enable` (gate) |
| HotseatLayerGetxController.calculateDockOffsetY no-label | `HotseatLayerGetxController.calculateDockOffsetY` | `0xadd4e8` | `enable` (gate) |
| HotseatLayerGetxController.calculatePositionY no-label | `HotseatLayerGetxController.calculatePositionY` | `0xd1913c` | `enable` (gate) |
| _buildBackground style select | `_HotseatWidgetState._buildBackground` | `0x10e5f28` | `enable`, `style` |
| GlassOs4Background radius | `GlassOs4Background.build` | `0x147507c` | `radius_on`, `radius` |
| MiuiXOs3BlurBackground radius | `MiuiXOs3BlurBackground.build` | `0x1475278` | `radius_on`, `radius` |
| horizontalPadding | `HotseatLayerGetxController.horizontalPadding` | `0x9a8914` | `padding_on`, `padding` |
| GridController.hotSeatsHeight delta | `GridController.hotSeatsHeight` | `0x9a3390` | `height_on`, `height` |
| dock icon vertical offset | `HotseatLayerGetxController.calculateDockOffsetY` | `0xadd550` | `doff_on`, `doff` |

## Soft glass ("home full glass")

Fifteen parameter stores in the glass parameter constructor. Each island reads the constructor's and the factory's
return addresses to tell the dock's dark/light glass from all other home glass. It then applies that record's
enabled flag, factor and fallback value and clamps the result to HyperChanger's ranges (minimum
`0,0,0,-1,0,1,0,0,-1,0,0,0,0,0,0`, maximum `500,2,3,1,2,5,120,3,1,1,100,2,1000,2,1`). Settings keys:
`kashi_glass_{home,dock}_{on,blur,luminance,saturation,brightness,alpha,refraction,thickness,bg_saturation,
bg_brightness,burn,edge,edge_feather,reflection_offset,light,darker}`.

| Patch | Function | 9239 offset | Data page |
|---|---|---|---|
| soft glass param 0 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d57b8` | `0x200` glass records |
| soft glass param 1 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d5778` | `0x200` glass records |
| soft glass param 2 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d57a0` | `0x200` glass records |
| soft glass param 3 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d57ac` | `0x200` glass records |
| soft glass param 4 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d57c0` | `0x200` glass records |
| soft glass param 5 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d56ec` | `0x200` glass records |
| soft glass param 6 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d567c` | `0x200` glass records |
| soft glass param 7 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d5740` | `0x200` glass records |
| soft glass param 8 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d5748` | `0x200` glass records |
| soft glass param 9 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d57c4` | `0x200` glass records |
| soft glass param 10 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d5668` | `0x200` glass records |
| soft glass param 11 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d56f4` | `0x200` glass records |
| soft glass param 12 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d5694` | `0x200` glass records |
| soft glass param 13 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d56a0` | `0x200` glass records |
| soft glass param 14 | glass parameter constructor (after `_DockGlassWidgetState._buildDarkGlassFilter`) | `0x10d56e4` | `0x200` glass records |

## Building the helper

```
cd helper && ./build.sh        # Termux on arm64, or CC=<NDK aarch64 clang> ./build.sh
```
