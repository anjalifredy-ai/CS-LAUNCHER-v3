# Current step

**Batch 2 DONE (next 5 micro-steps):**
1. `ApkNativeInventory` — list `lib/**/*.so` inside imported APK (Zip only, no System.load)
2. Save inventory JSON under `meta/native_inventory.json`
3. `NativeHostChecklist` — readiness flags (all false until real host)
4. Stub dialog shows ABI + .so counts
5. Auto-scan after successful APK import

**Still NOT playable multi-version.**

**Next:** extract selected .so files to isolation `natives/` (still no load) OR document ABI filter.

Say **"agla step"** or **"5 step"**.
