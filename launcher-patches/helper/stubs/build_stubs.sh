#!/usr/bin/env bash
# Link-time stand-ins for the HyperOS 4 system libraries the helper links against. Only their symbol names and
# sonames matter; at runtime the real /system_ext/lib64/libhyper_os_*.so are used.
set -e; cd "$(dirname "$0")"
CC=${CC:-clang}
$CC ${CFLAGS} -shared -fPIC -o libhyper_os_system_public.so sys.c -Wl,-soname,libhyper_os_system_public.so
for n in sensor core_data; do
    $CC ${CFLAGS} -shared -fPIC -o libhyper_os_${n}_public.so empty.c -Wl,-soname,libhyper_os_${n}_public.so
done
