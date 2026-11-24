# Ampy native core (Rust)

This workspace contains the Rust replacement for Ampy's previous C++ JNI libraries:

- `ampy_chart` → `libampy_chart.so`
- `ampy_logparse` → `libampy_logparse.so`
- `ampy_stats` → `libampy_stats.so`

All three export the exact JNI symbols expected by the Kotlin facades in
`app/src/main/java/com/zenin/ampy/nativelib/`.

## Pinned toolchain

`rust-toolchain.toml` pins Rust 1.100.0 and the `aarch64-linux-android` target.
The Gradle build expects `cargo-ndk` 4.1.2.

## Local checks

```bash
cargo test --workspace
```

## Android build

Gradle's `buildRustLibs` task runs automatically before `preBuild`:

```bash
./gradlew :app:assembleDebug
```

To build the libraries directly:

```bash
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs build --release \
  -p ampy_chart -p ampy_logparse -p ampy_stats
```

`.cargo/config.toml` adds the required 16 KB `LOAD` segment alignment flags.
