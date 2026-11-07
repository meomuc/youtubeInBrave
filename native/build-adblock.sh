#!/usr/bin/env bash
# Biên dịch libadblock_jni.so cho arm64-v8a và x86_64, rồi đặt vào app/src/main/jniLibs.
# Chạy từ Git Bash: bash native/build-adblock.sh
# Cần: Rust (rustup) với target aarch64-linux-android và x86_64-linux-android, và Android NDK.
# NDK mặc định là bản 27.2.12479018 trong Android SDK; đặt ANDROID_NDK_HOME để đổi.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK="${ANDROID_NDK_HOME:-$HOME/AppData/Local/Android/Sdk/ndk/27.2.12479018}"
TOOLCHAIN_BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
API=26

if [ ! -d "$TOOLCHAIN_BIN" ]; then
  echo "Không tìm thấy NDK tại $NDK. Đặt ANDROID_NDK_HOME rồi chạy lại." >&2
  exit 1
fi

cd "$ROOT/native/adblock-jni"

build_abi() {
  local target="$1" abi="$2" linker="$3" env_name="$4"
  echo "== Build $target ($abi)"
  env "$env_name=$TOOLCHAIN_BIN/$linker" \
    cargo build --release --target "$target"
  local out="$ROOT/app/src/main/jniLibs/$abi"
  mkdir -p "$out"
  cp "target/$target/release/libadblock_jni.so" "$out/libadblock_jni.so"
  echo "   -> $out/libadblock_jni.so"
}

build_abi aarch64-linux-android arm64-v8a "aarch64-linux-android${API}-clang.cmd" \
  CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER
build_abi x86_64-linux-android x86_64 "x86_64-linux-android${API}-clang.cmd" \
  CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER

echo "Xong. Gradle sẽ đóng gói .so từ app/src/main/jniLibs."
