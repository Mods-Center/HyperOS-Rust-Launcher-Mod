#!/system/bin/sh
MODDIR=${0%/*}
# --- Mods Center: warn in the root manager if module files are not mounted ---
[ -f "$MODDIR/kashi_mount_check.sh" ] && { . "$MODDIR/kashi_mount_check.sh"; kashi_boot_check; } &

# HyperLauncherExt: reinstall when missing or older than the module's copy
P=com.miui.home.kashi.ext
until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 5; done
sleep 10
[ -f "$MODDIR/kashi_ext.apk" ] || exit 0
want=$(cat "$MODDIR/kashi_ext.ver" 2>/dev/null)
have=$(dumpsys package "$P" 2>/dev/null | grep -m1 'versionCode=' | sed 's/.*versionCode=\([0-9]*\).*/\1/')
if [ -z "$have" ] || { [ -n "$want" ] && [ "$have" -lt "$want" ]; }; then
    sz=$(stat -c %s "$MODDIR/kashi_ext.apk")
    cat "$MODDIR/kashi_ext.apk" | pm install -r -S "$sz" >/dev/null 2>&1
fi
