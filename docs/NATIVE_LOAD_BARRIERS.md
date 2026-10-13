# Why extracted .so ≠ playable Bedrock

Remom can import an **owned** official APK, list and extract `lib/<abi>/*.so`.
That is **not** enough to run Minecraft Bedrock inside another app.

## Barriers

1. **Process model** — Bedrock expects its own app process, package name, data dirs, Play licensing hooks.
2. **Dependency order** — dozens of `.so` files; load order and JNI_OnLoad matter; wrong order → crash.
3. **Assets / split APKs** — resources not only in `lib/`; base.apk + splits + assets needed.
4. **Linker / namespace** — Android linker, library search path, `java.library.path`, and app isolation.
5. **Graphics / input** — Surface, EGL, touch pipeline wired for `com.mojang.minecraftpe` Activity.
6. **Online services** — Xbox Live / Store may reject non-official package context.
7. **Legal** — redistribute or run cracked clients is out of scope; only user-owned official bits.

## What Levi-class host does

A real multi-version host is a **dedicated native project**: correct ABI extract, env setup, Activity/host surface, version isolation, continuous breakage fixes — not a single `System.load` call.

## What Remom probe does

`NativeLoadProbe` may call `System.load(absolutePath)` on one or more extracted libs and **record** the result. Success on load ≠ game runs.
