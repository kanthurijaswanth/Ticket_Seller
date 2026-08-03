# Setup on a new machine

## Prerequisites

| Item | Version |
|---|---|
| JDK | 17+ (Android Studio's bundled JBR 21 works fine) |
| Android Studio | Hedgehog or later |
| Android SDK | Platform 35, build-tools 35+ |
| Device or emulator | Android 7.0+ (a **real phone** is needed to test UPI payment) |

## 1. Clone

```bash
git clone https://github.com/kanthurijaswanth/Ticket_Seller.git
cd Ticket_Seller
```

## 2. Point Gradle at your SDK and JDK

- Create **`android/local.properties`** (gitignored) with your SDK path:
  ```properties
  sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
  ```
- **`android/gradle.properties`** has `org.gradle.java.home` pointing at a JDK. If you build from
  Android Studio you can remove that line (the IDE sets its own). On another machine, change it
  to your local JDK 17+ path, e.g. the Android Studio JBR:
  ```properties
  org.gradle.java.home=C\:/Program Files/Android/Android Studio/jbr
  ```

> The project intentionally does **not** pin a Kotlin JDK-17 toolchain, so it compiles with
> JDK 21 (JBR). The Gradle wrapper's `networkTimeout` is raised because the first build downloads
> ~1.5 GB of dependencies.

## 3. Set up the backend (Supabase)

The committed `SupabaseConfig.kt` already has a project URL + anon key. If that project is gone
or you want your own, follow **"Setting up a fresh Supabase project"** in [DATABASE.md](DATABASE.md)
— create a project, paste URL + anon key into `SupabaseConfig.kt`, and run
[`../supabase_schema.sql`](../supabase_schema.sql) in the SQL Editor.

## 4. Build & install

```bash
cd android
./gradlew.bat :app:assembleDebug      # APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew.bat :app:installDebug       # build + install on a connected device
```

Connecting a device: enable **Developer options → USB debugging**, plug in, set USB mode to
**File transfer**, and accept the "Allow USB debugging?" prompt.

## 5. First run

- Tap **DEV: Skip sign-in** for instant access (demo profile).
- **Profile → Seed demo tickets** to populate Browse.
- **Profile → Colours & theme** to try the 8 themes.

---

## Common issues

- **Empty Browse / "can't reach server"** — the Supabase project is paused or deleted. Restore
  it (or create a new one) — see [DATABASE.md](DATABASE.md). DNS takes ~30 s after restoring.
- **UPI ID / ratings don't save** — run the migration SQL in [DATABASE.md](DATABASE.md).
- **`device offline` during install** — unlock the phone / re-plug, or run
  `adb kill-server && adb start-server`.
- **Build can't find Gradle** — set `JAVA_HOME` to a JDK 17+ before running `gradlew`.
