ui_print "
             __  ___        __
            /  |/  /__  ___/ /__
           / /|_/ / _ \/ _  (_-<
          /_/  /_/\___/\_,_/___/
            _____         __
           / ___/__ ___  / /____ ____
          / /__/ -_) _ \/ __/ -_) __/
          \___/\__/_//_/\__/\__/_/
"
ui_print "        HyperOS 4 Rust Launcher Mod by 🅺🅰🆂🅷🅸"
ui_print " "

LAUNCHER_PKG="com.miui.home"
OLD_MODULE_ID="miui_launcher_mod"          # dex-launcher mod; it replaces the same APK

# ------------------------------------------------------------------ HyperOS 4 detection
# Ports often keep the base ROM's version props (e.g. ro.mi.os.version.name=OS3.0 on a HyperOS 4 port), so the
# props are only one of two signals. The other is what this launcher really needs: the HyperOS 4 Rust runtime
# (hyos_spawner + the libhyper_os_* libraries it loads libapp_launcher.so with).
props_say_os4() {
    for p in ro.mi.os.version.name ro.mi.os.version.incremental ro.build.version.incremental ro.system.build.version.incremental; do
        case "$(getprop $p)" in OS4*|OS\ 4*|V4.*) return 0 ;; esac
    done
    code=$(getprop ro.mi.os.version.code)
    case "$code" in ''|*[!0-9]*) ;; *) [ "$code" -ge 4 ] && return 0 ;; esac
    return 1
}

# Genuine HyperOS 3 also has hyos_spawner and libhyper_os_* (an older runtime), but not libmisqlite3 /
# librust_maml_sdk, which this launcher links against: requiring them keeps HyperOS 3 from passing as HyperOS 4.
rust_runtime_present() {
    spawner=""
    for d in /system_ext/bin /system/bin /system/system_ext/bin; do
        [ -e "$d/hyos_spawner" ] && spawner="$d/hyos_spawner"
    done
    [ -n "$spawner" ] || return 1
    for lib in libhyper_os_flutter.so libmisqlite3.so librust_maml_sdk.so libhyper_os_app_public.so \
               libhyper_os_ui_public.so libhyper_os_system_public.so; do
        found=0
        for d in /system_ext/lib64 /system/lib64 /system/system_ext/lib64; do
            [ -e "$d/$lib" ] && found=1
        done
        [ $found = 1 ] || return 1
    done
    return 0
}

# 0 = Vol+, 1 = Vol-, 2 = no key within $1 seconds
volume_key() {
    timeout_s=${1:-15}
    start=$(date +%s)
    while :; do
        ev=$(timeout 1 getevent -lqc 1 2>/dev/null)
        case "$ev" in
            *KEY_VOLUMEUP*DOWN*) return 0 ;;
            *KEY_VOLUMEDOWN*DOWN*) return 1 ;;
        esac
        [ $(( $(date +%s) - start )) -ge "$timeout_s" ] && return 2
    done
}

check_hyperos4() {
    sdk=$(getprop ro.build.version.sdk)
    case "$sdk" in ''|*[!0-9]*) sdk=0 ;; esac
    if [ "$sdk" -lt 35 ]; then
        abort "! Android $(getprop ro.build.version.release) (SDK $sdk) is too old: HyperOS 4 is Android 15 or newer."
    fi
    if props_say_os4; then
        ui_print "- HyperOS 4 detected ($(getprop ro.mi.os.version.name))"
        return
    fi
    if rust_runtime_present; then
        ui_print "- HyperOS 4 launcher runtime found (props say: $(getprop ro.mi.os.version.name), common on ports)"
        return
    fi
    ui_print " "
    ui_print "! Could not confirm HyperOS 4:"
    ui_print "  version props: $(getprop ro.mi.os.version.name) / $(getprop ro.build.version.incremental)"
    ui_print "  HyperOS 4 launcher runtime (hyos_spawner, libhyper_os_*, libmisqlite3, librust_maml_sdk): not found"
    ui_print "  This launcher only runs on HyperOS 4. On other systems it will not start."
    ui_print " "
    ui_print "  Vol+ = install anyway      Vol- = cancel      (cancels in 20 s)"
    volume_key 20
    case $? in
        0) ui_print "- Installing anyway (your choice)" ;;
        *) abort "! Cancelled: HyperOS 4 not confirmed" ;;
    esac
}

# ------------------------------------------------------------------ install
remove_old_module() {
    if [ -d "/data/adb/modules/$OLD_MODULE_ID" ] || [ -d "/data/adb/modules_update/$OLD_MODULE_ID" ]; then
        ui_print "- Old launcher mod ($OLD_MODULE_ID) found: it is removed on reboot (both replace the same launcher)"
        touch "/data/adb/modules/$OLD_MODULE_ID/remove" 2>/dev/null
    fi
}

# Path inside $MODPATH for a file of the system image (/product and /system_ext are mounted through /system).
module_path() {
    case "$1" in
        /system/*) echo "$MODPATH$1" ;;
        *) echo "$MODPATH/system$1" ;;
    esac
}

install_launcher() {
    ui_print "- Locating the system launcher..."
    pm uninstall-system-updates "$LAUNCHER_PKG" >/dev/null 2>&1
    app_path=$(pm path "$LAUNCHER_PKG" 2>/dev/null | head -n1 | sed 's/^package://')
    case "$app_path" in
        /data/*|"")
            ui_print "- System copy not reported, using the HyperOS 4 location"
            app_path="/product/priv-app/MiuiHome/MiuiHome.apk" ;;
    esac
    ui_print "  $app_path"
    target=$(module_path "$app_path")
    rm -rf "$MODPATH/system/product/priv-app/MiuiHome"
    mkdir -p "$(dirname "$target")"
    ui_print "- Copying the patched launcher..."
    cp -f "$MODPATH/files/MiuiHome.apk" "$target"
    is_valid "$target" || abort "! Copying the launcher failed"
    # Also try a direct update (works when signature verification is disabled); stay silent when it fails,
    # the module copy is applied on reboot either way.
    if pm install -r "$MODPATH/files/MiuiHome.apk" >/dev/null 2>&1; then
        ui_print "- Launcher also updated directly"
    fi
}

install_ext() {
    target="$MODPATH/system/product/priv-app/HyperLauncherExt/HyperLauncherExt.apk"
    mkdir -p "$(dirname "$target")"
    ui_print "- Installing HyperLauncherExt (settings app, system app: works without root)..."
    cp -f "$MODPATH/files/HyperLauncherExt.apk" "$target"
    is_valid "$target" || abort "! Copying HyperLauncherExt failed"
}

clean_environment() {
    ui_print "- Cleaning launcher package cache..."
    for f in /data/system/package_cache/*/*; do
        case "$f" in *[Hh]ome*|*kashi*) rm -f "$f" ;; esac
    done
    ui_print "- Fixing contexts..."
    set_context /system "$MODPATH/system"
    rm -rf "$MODPATH/files"
    rm -f "$MODPATH/customize.sh"
}

package_extract_dir files "$MODPATH/files"
package_extract_dir system "$MODPATH/system"

check_hyperos4
ui_print "
    ------------------------------------------
                       READ!
      Signature verification must be disabled
      on stock ROMs, otherwise
      the launcher will not load.
    ------------------------------------------
"
remove_old_module
install_launcher
install_ext
clean_environment
ui_print " "
ui_print "- Done. Reboot, then open HyperLauncherExt app to configure the launcher."
