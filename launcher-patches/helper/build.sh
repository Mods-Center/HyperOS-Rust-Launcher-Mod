#!/usr/bin/env bash
# Builds libkashidock.so (arm64-v8a, 16 KB page aligned).
#   Termux on an arm64 phone:   ./build.sh
#   Android NDK on a PC:         CC="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android35-clang" ./build.sh
set -e; cd "$(dirname "$0")"
CC=${CC:-clang}
CC="$CC" bash stubs/build_stubs.sh
$CC -shared -fPIC -O2 -Wall -Wextra -Werror -fvisibility=hidden -o libkashidock.so kashidock.c \
    -Wl,-soname,libkashidock.so -Wl,-z,max-page-size=16384 -Wl,--no-as-needed -Lstubs \
    -lhyper_os_sensor_public -lhyper_os_system_public -lhyper_os_core_data_public -Wl,--as-needed -llog
command -v patchelf >/dev/null && patchelf --remove-rpath libkashidock.so || true
command -v llvm-strip >/dev/null && llvm-strip --strip-unneeded libkashidock.so || true
echo "built $(pwd)/libkashidock.so"
