# Clear cached package scans so the stock launcher is picked up again on the next boot.
for f in /data/system/package_cache/*/*; do
    case "$f" in *[Hh]ome*|*kashi*) rm -f "$f" ;; esac
done
