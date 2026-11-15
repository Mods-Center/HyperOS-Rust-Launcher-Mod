# Clear cached package scans so the stock launcher is picked up again on the next boot.
for f in /data/system/package_cache/*/*; do
    case "$f" in *[Hh]ome*|*kashi*) rm -f "$f" ;; esac
done
# HyperLauncherExt is a normal app installed by the module: remove it with the module
pm uninstall com.miui.home.kashi.ext >/dev/null 2>&1
