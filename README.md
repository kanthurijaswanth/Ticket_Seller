# TicketResell

A two-sided marketplace for last-minute, non-cancellable bus and train tickets.

- **Seller mode** — upload a ticket you can't use. The app auto-extracts details with on-device OCR, runs a duplicate-image check across all listings, and puts the listing live.
- **Buyer mode** — browse verified last-minute tickets nearby, chat with the seller in-app, exchange contact details only after both sides agree.

---

## 📚 Table of contents

1. [Architecture overview](#architecture-overview)
2. [Tech stack](#tech-stack)
3. [Project layout](#project-layout)
4. [Setup for a new developer](#setup-for-a-new-developer)
5. [Credentials (where they live)](#credentials-where-they-live)
6. [How features work](#how-features-work)
7. [Free tier limits & cost trajectory](#free-tier-limits--cost-trajectory)
8. [Build & run commands](#build--run-commands)
9. [Common issues](#common-issues)
10. [Roadmap / things not built yet](#roadmap--things-not-built-yet)

---

## Architecture overview

```
        ┌───────────────────┐                      ┌───────────────────┐
        │   Phone A (you)   │                      │  Phone B (other)  │
        │                   │                      │                   │
        │   TicketResell    │                      │   TicketResell    │
        └─────────┬─────────┘                      └─────────┬─────────┘
                  │            HTTPS (encrypted)             │
                  ▼                                          ▼
        ┌─────────────────────────────────────────────────────────────┐
        │                     Supabase (Postgres + Realtime)          │
        │   ┌─────────────┐  ┌──────────────────┐  ┌──────────────┐  │
        │   │   Auth      │  │   Postgres DB    │  │  Realtime    │  │
        │   │ (gotrue)    │  │   public.*       │  │  channels    │  │
        │   └─────────────┘  └──────────────────┘  └──────────────┘  │
        └─────────────────────────────────────────────────────────────┘
                  │
                  │  (confirmation emails)
                  ▼
        ┌─────────────────────────────────────────────────────────────┐
        │           Gmail SMTP   (smtp.gmail.com:465)                  │
        │           500 emails / day, free                             │
        └─────────────────────────────────────────────────────────────┘
```

- **No separate backend server.** The Android app talks directly to Supabase.
- **OCR runs on-device** (ML Kit) — free, offline, no API calls.
- **AdMob** is independent of the backend — works regardless of what we use.

---

## Tech stack

### Android client

| Layer | Tech |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM with Hilt DI |
| Navigation | Compose Navigation |
| Async | Kotlin Coroutines + Flow |
| Serialization | kotlinx.serialization |
| Image OCR | Google ML Kit Text Recognition (on-device) |
| Image hashing | Custom 16×16 pHash (`data/verify/ImageHasher.kt`) |
| Theme | Yellow + white brand palette |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 |
| Build tool | Gradle 8.12 + AGP 8.4.0 |
| JDK required | 17 |

### Backend (Supabase)

| Service | Used for |
|---|---|
| Postgres database | All persistent data (users, listings, chats, orders) |
| Supabase Auth | Email/password sign-up + sign-in (with confirmation emails) |
| Supabase Realtime | Live snapshots of listings, chat threads, messages |
| Supabase Storage | Not used yet — ticket images live as base64 inside Postgres for now |

### Third-party services

| Service | Used for | Cost |
|---|---|---|
| Gmail SMTP | Sending Supabase Auth confirmation emails | Free (500/day) |
| Google AdMob | Banner + interstitial ads (planned) | Pays you, ₹0 cost |
| Google Play Console | Store distribution | $25 one-time |

---

## Project layout

```
Ticket_Seller/
├── README.md                       ← you are here
├── CREDENTIALS.md                  ← REAL passwords/keys, gitignored
├── supabase_schema.sql             ← Run this once in Supabase SQL Editor
├── .gitignore                      ← Excludes secrets and build artifacts
│
├── server/                         ← Legacy Ktor backend (UNUSED — kept for reference)
│
└── android/
    └── app/
        └── src/main/
            ├── AndroidManifest.xml
            ├── res/                ← Colors, theme, launcher icon, strings
            └── java/com/ticketseller/app/
                ├── TicketSellerApp.kt          ← @HiltAndroidApp Application class
                │
                ├── data/
                │   ├── SupabaseConfig.kt       ← URL + anon key (committed; safe)
                │   ├── model/                  ← @Serializable data classes
                │   ├── repo/                   ← Supabase-backed repositories
                │   │   ├── AuthRepository.kt
                │   │   ├── UserRepository.kt
                │   │   ├── ListingRepository.kt
                │   │   ├── ChatRepository.kt
                │   │   └── OrderRepository.kt
                │   ├── verify/                 ← OCR + image hash + verifier
                │   │   ├── OcrService.kt
                │   │   ├── ImageHasher.kt
                │   │   ├── ImageUtils.kt
                │   │   └── TicketVerifier.kt
                │   └── seed/
                │       └── DemoSeed.kt         ← Writes 10 sample listings
                │
                ├── di/
                │   ├── SupabaseModule.kt       ← Provides SupabaseClient (Hilt)
                │   └── FirebaseModule.kt       ← Empty stub (legacy)
                │
                └── ui/
                    ├── MainActivity.kt
                    ├── Nav.kt                  ← Compose NavHost
                    ├── theme/                  ← Color.kt, Theme.kt, Type.kt
                    ├── common/Components.kt    ← Reusable composables
                    └── screens/
                        ├── auth/               ← LoginScreen, SignupScreen, AuthViewModel
                        ├── main/MainShell.kt   ← Bottom-nav scaffold
                        ├── browse/BrowsePane.kt
                        ├── listing/ListingDetailScreen.kt
                        ├── sell/               ← SellScreen + SellViewModel
                        ├── chat/               ← ChatListPane + ChatScreen
                        ├── orders/             ← MyTicketsPane + OrderDetail + Payment
                        └── profile/            ← ProfilePane + EditProfile
```

---

## Setup for a new developer

### Prerequisites

| Item | Version |
|---|---|
| JDK | 17 (Eclipse Adoptium recommended) |
| Android Studio | Hedgehog or later |
| Android SDK | Platform 35, build-tools 35 |
| ADB-recognised device or emulator | Android 7.0+ |

### Step 1 — Get the code

```bash
git clone <repo-url>
cd Ticket_Seller
```

### Step 2 — Get the credentials

Open **CREDENTIALS.md** (sent privately by the project owner — not in git).

You'll find:
- Supabase URL + anon key → already pasted into `android/app/src/main/java/com/ticketseller/app/data/SupabaseConfig.kt`
- Gmail SMTP App Password → already configured in Supabase Dashboard
- Other future secrets

If `SupabaseConfig.kt` shows the placeholder `"https://YOUR-PROJECT-ID..."`, paste the real values from CREDENTIALS.md.

### Step 3 — Verify Supabase schema exists

The 5 tables (`users`, `listings`, `chat_threads`, `chat_messages`, `orders`) should already exist. If a fresh project: open Supabase → SQL Editor → paste `supabase_schema.sql` → Run.

### Step 4 — Build & install

```bash
cd android
./gradlew :app:installDebug          # On Windows: gradlew.bat
```

First build downloads ~1.5 GB of dependencies. Subsequent builds are ~30 s.

### Step 5 — Run

- Open the app on a connected device.
- Tap **DEV: Skip sign-in** (yellow button at the bottom) for instant access.
- Go to **Profile → Seed demo tickets** to populate the Browse feed with 10 sample listings.

---

## Credentials (where they live)

| Credential | Stored in | Committed to git? |
|---|---|---|
| Supabase URL | `SupabaseConfig.kt` + CREDENTIALS.md | ✅ Yes (URL is not secret) |
| Supabase anon key | `SupabaseConfig.kt` + CREDENTIALS.md | ✅ Yes (anon key is safe to ship) |
| Supabase service_role key | CREDENTIALS.md only | ❌ Never in app code |
| Postgres password | CREDENTIALS.md only | ❌ Never |
| Gmail App Password | Supabase Dashboard SMTP settings + CREDENTIALS.md | ❌ Never |
| Gmail account | CREDENTIALS.md | ❌ Never |
| AdMob test App ID | `AndroidManifest.xml` | ✅ Yes (test ID is public) |
| AdMob real App ID | CREDENTIALS.md (once created) | ❌ Never |
| Play Store keystore | Encrypted backup + CREDENTIALS.md | ❌ NEVER. Losing it means losing the app on Play Store. |

### Why anon keys are committed but secret keys are not

The Supabase **anon key** is designed to be public. It's a JWT that says *"I'm an anonymous user."* What it can do is limited by your **Row Level Security policies**. If someone steals it from your APK, the worst they can do is what an anonymous browser visitor can already do.

The **service role key** bypasses all security. It's like your DB root password. Never put it in the app or in any committed file.

---

## How features work

### Verification (the "double-verification" pitch)

Layer 1 — **on-device OCR & sanity checks**:
- ML Kit reads the uploaded ticket image (`OcrService.kt`).
- Extracts PNR / booking ID, source, destination, journey date.
- Validates: future date, PNR format, required fields present.

Layer 2 — **cross-listing duplicate detection**:
- Computes a 16×16 perceptual hash of the image (`ImageHasher.kt`).
- Queries Postgres for any listing with the same hash.
- Duplicate match → listing held in `PENDING_REVIEW`.

A listing only auto-promotes to `ACTIVE` when both layers pass.

### Dev sign-in bypass

`AuthRepository.devBypassSignIn()` generates a random UUID like `dev_4f2a8e1b…`, stores it in SharedPreferences, and uses it as the user ID for the rest of the session. No Supabase Auth call is made. The user object is created in the `users` table via best-effort write.

The button is in `LoginScreen.kt` — clearly marked **DEV ONLY**. Remove before release.

### Real-time updates

Each repository observe* method:
1. Fetches the initial state via `supabase.from(...).select(...)`.
2. Subscribes to a Realtime channel for that table.
3. On any change event, re-fetches the query result.

Simpler than maintaining a delta-applied cache, and Realtime is free.

### Chat with consent-based contact reveal

When a buyer chats with a seller:
- They can message freely without sharing real-world contact info.
- Both can tap **"I agree to share my number"** in the chat.
- Once **both** have consented, the other side's phone number appears in the chat header.
- Phone numbers come from the user's saved profile (Edit Profile screen).

---

## Free tier limits & cost trajectory

### Supabase

| Resource | Free quota | App-level meaning |
|---|---|---|
| Postgres storage | 500 MB | ~3,000 listings with base64 images |
| Realtime concurrent connections | 200 | 200 phones with the app open at once |
| Monthly active users | 50,000 | Way more than Firebase's 150 DAU ceiling |
| Database bandwidth | 5 GB / month | Enough for an early-stage app |
| Edge function invocations | 500,000 / month | Not used today |

### Gmail SMTP

| Resource | Limit | When exceeded |
|---|---|---|
| Emails per day | 500 | Sign-up emails fail; app shows *"Today's email server quota is full. Try creating an account tomorrow."* |

### Migration path

| Active users | Cost | Plan |
|---|---|---|
| 0 – 50,000 MAU | ₹0 | Supabase Free |
| 50,000 – 100,000 MAU | ~₹2,100 / month | Supabase Pro |
| 100,000+ MAU | ₹50,000+ / month | Supabase Team OR self-host (₹400/month VPS) |

---

## Build & run commands

```bash
# Install on connected device
./gradlew :app:installDebug

# Build a debug APK (in app/build/outputs/apk/debug/)
./gradlew :app:assembleDebug

# Build a release APK (needs signing config)
./gradlew :app:assembleRelease

# Build an AAB for Play Store
./gradlew :app:bundleRelease

# Clean
./gradlew clean

# Stop the Gradle daemon (handy when files are locked on Windows)
./gradlew --stop
```

### Adb shortcuts during development

```bash
# Clear app data (back to fresh login)
adb shell pm clear com.ticketseller.app

# Tail app logs
adb logcat -s AndroidRuntime:E ticketseller.app:V

# Launch the app
adb shell am start -n com.ticketseller.app/.ui.MainActivity
```

---

## Common issues

### "PERMISSION_DENIED" from Supabase

**Cause:** Row-Level Security is enabled but no policy matches your user.

**Fix:** for development, the `supabase_schema.sql` script disables RLS on all tables. Verify by running:

```sql
select relname, relrowsecurity from pg_class where relname in
  ('users', 'listings', 'chat_threads', 'chat_messages', 'orders');
```

All five should show `f` (false).

### "Today's email server quota is full"

Gmail SMTP hit 500 emails for the day. Wait until tomorrow, or upgrade to Brevo / AWS SES.

### "Email not confirmed" on sign-in

The new user hasn't clicked the link in their inbox yet. Either:
- They click the link → can sign in
- Or disable email confirmation in Supabase Dashboard → Auth → Sign-in/up → toggle "Confirm email" off

### App icon / launch screen looks white

The launcher icon is a vector at `res/drawable/ic_launcher.xml`. If the device shows white, that means the manifest icon attribute isn't pointing at it. Check `AndroidManifest.xml` `android:icon="@drawable/ic_launcher"`.

### Build fails with "Could not read workspace metadata"

The Gradle cache was corrupted (usually after running out of disk).

```bash
./gradlew --stop
rm -rf ~/.gradle/caches/8.12
./gradlew :app:installDebug
```

### Lock release fails on Windows

```
Failed to release lock on Build Output Cleanup Cache
```

Two Gradle processes overlapped. Close Android Studio entirely, run `./gradlew --stop`, then retry.

---

## Roadmap / things not built yet

- ☑ **UPI payment** — buyers pay the seller's UPI ID directly via their UPI app (GPay/PhonePe/Paytm). See [UPI payment flow](#upi-payment-flow) below.
- ☑ **Ratings UI** — buyers and sellers rate each other after a completed order; the average shows on profiles.
- ☑ **Order lifecycle** — Buy → reserve → pay → seller confirms → completed → rate; sellers see incoming orders in the **Sold** tab.
- ☑ **Real verified badges** — listings show "Verified" or "Pending review" based on actual status.
- ☑ **Forgot password** + friendly email-confirmation handling.
- ☑ **"How it works"** in-app onboarding dialog (top-bar help icon).
- ☐ **Razorpay / Cashfree escrow** — optional upgrade if you want the platform to hold funds / take commission (UPI-direct has no escrow).
- ☐ **Push notifications** — would use Firebase Cloud Messaging or OneSignal (separate from backend).
- ☐ **Photo cropping** before upload — currently uploads as-is.
- ☐ **Admin / review queue UI** — pending listings sit in Postgres; no admin screen.
- ☐ **Real AdMob** — currently using Google's test App ID.
- ☐ **Release-signed APK / AAB** for Play Store — needs a keystore generated.
- ☐ **Privacy policy page** for Play Store listing.
- ☐ **Google Sign-In** — removed during Supabase migration; re-add via Supabase OAuth.

---

## UPI payment flow

Payments go **directly from the buyer to the seller** over UPI — the app never holds money (no gateway account or fees needed).

1. A seller adds their **UPI ID** in **Profile → Edit profile** (e.g. `yourname@okhdfcbank`).
2. A buyer taps **Buy** on a listing → an order is created and the ticket is **reserved**.
3. On the payment screen the buyer taps **Pay ₹X via UPI** → their UPI app opens pre-filled with the seller's UPI ID and amount.
4. After paying, the buyer confirms → the order is marked **paid** and the listing becomes **Sold**.
5. The seller sees the sale under **My Tickets → Sold**, shares the ticket, and taps **Mark as completed**.
6. Both sides can then **rate** each other.

> Because payment is peer-to-peer UPI, there's no escrow. If you later want the platform to hold funds or take a commission, integrate Razorpay/Cashfree and route payments through a Supabase Edge Function (kept as a future item above).

If a seller hasn't added a UPI ID, the buyer is prompted to ask for it in chat and can confirm a manual payment.

---

## Credits

Built collaboratively with Claude (Anthropic).
