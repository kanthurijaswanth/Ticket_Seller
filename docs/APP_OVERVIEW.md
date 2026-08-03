# TicketResell — App Overview

**A two-sided marketplace for last-minute, non-cancellable bus & train tickets.**
Someone else's seat, your trip.

- **Sellers** upload a ticket they can't use. The app reads the details from the photo
  (on-device OCR), checks it isn't a duplicate, and puts it live.
- **Buyers** browse verified last-minute tickets, chat with the seller, buy, and pay the
  seller **directly over UPI**. Contact details are shared only when both sides agree.

One account does both — there is no separate "seller" sign-up.

---

## What's built (v1.0)

### Core marketplace
- **Sell a ticket** — pick/scan a photo → **on-device OCR** (ML Kit) auto-fills PNR, route,
  operator, date → **perceptual-hash duplicate check** → auto-verify → list. The form
  **verifies required fields** (PNR, passenger, operator, source/destination, date, price)
  before posting.
- **Browse / Home** — greeting, search, Buy/Sell tiles, and a live feed of verified
  "leaving soon" tickets. Filter by Bus/Train.
- **Ticket detail** — route, boarding timeline, ticket paperwork, seller rating, "why this
  is safe", honest **Verified / Pending** badge based on real status.
- **Buy → Pay (UPI)** — buying reserves the ticket and opens a payment screen. The buyer taps
  **Pay via UPI**, their UPI app (GPay/PhonePe/Paytm) opens pre-filled with the seller's UPI ID
  and amount. On confirmation the ticket is marked **sold**. No escrow, no gateway, no fees —
  money goes straight buyer → seller.
- **Order lifecycle** — reserve → paid → seller confirms handover → completed → both rate;
  cancelling puts the ticket back on the market. Sellers see incoming orders under
  **Your Tickets → Sold**.
- **Ratings** — after a completed order, each side rates the other; the average shows on profiles.
- **Chat** — realtime messaging with a **mutual-consent phone-number reveal** (numbers appear
  only after both tap "Share my number").
- **Notifications** — derived from your real orders (payment received, someone's buying, rate
  the seller, …).

### Design & UX
- **Glassmorphism UI** — frosted glass cards with **real backdrop blur** (Android 12+, graceful
  fallback below), clear borders + depth shadows, clean flat backgrounds.
- **8 live themes** — Sunshine, Mint, Indigo, Rose, Ocean, Graphite, Classic, Monochrome — each
  in **Light or Dark**, switched live from **Profile → Colours & theme**.
- **Fonts** — Plus Jakarta Sans (UI) + JetBrains Mono (numbers).
- **Animated splash** — six theme colours sweep in and blend into the logo.
- **Adaptive launcher icon** — gradient ticket badge (foreground + background + monochrome).
- Keyboard-safe forms, edge-to-edge insets, opaque flush bottom navigation.

### Platform
- **Play In-App Update** prompt on launch and **In-App Review** ("Rate us") — active when
  installed from the Play Store.
- **AdMob** test IDs wired (replace before release).

### Developer tools (remove before public release)
- **DEV: Skip sign-in** — logs in as a ready-made demo profile (Aditya Sharma, 4.8★).
- **Seed demo tickets** — from Profile, drops realistic sample listings into Browse.

---

## Tech stack

| Layer | Tech |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Navigation | Navigation-Compose |
| Async | Coroutines + Flow |
| Serialization | kotlinx.serialization |
| Images | Coil |
| OCR | Google ML Kit Text Recognition (on-device) |
| Blur | Haze |
| Play services | Play In-App Update + In-App Review, AdMob |
| Backend | Supabase (Postgres + Realtime + Auth) — see [DATABASE](DATABASE.md) |
| Min / Target SDK | 24 / 35 · AGP 8.4 · Gradle 8.12 |

---

## Project layout

```
Ticket_Seller/
├── docs/                     ← you are here
├── supabase_schema.sql       ← run once in Supabase SQL Editor
└── android/app/src/main/
    ├── AndroidManifest.xml
    ├── res/ (font/, drawable/, mipmap-anydpi*/ launcher icon)
    └── java/com/ticketseller/app/
        ├── data/
        │   ├── SupabaseConfig.kt        ← URL + anon key
        │   ├── model/                   ← @Serializable data classes
        │   ├── repo/                    ← Auth, User, Listing, Chat, Order, Rating
        │   ├── verify/                  ← OCR, image hash, ticket verifier
        │   └── seed/DemoSeed.kt
        ├── di/                          ← Hilt modules (SupabaseModule)
        └── ui/
            ├── MainActivity.kt · Nav.kt
            ├── theme/                   ← 8 themes, GlassTokens, fonts, ThemeController
            ├── common/                  ← Glass component kit (cards, background, buttons)
            └── screens/                 ← splash, auth, home, listing, sell, chat,
                                            orders (payment/detail/tickets), notifications, profile
```

---

## Build & run

See [SETUP.md](SETUP.md) for the full environment. Quick version:

```bash
cd android
./gradlew.bat :app:installDebug   # builds and installs on a connected device
```

Then in the app: tap **DEV: Skip sign-in**, and use **Profile → Seed demo tickets** to fill Browse.

---

## Not built yet / future

- Escrow / commission (would need a payment gateway + a Supabase Edge Function)
- Push notifications (FCM)
- Photo cropping before upload
- Admin / review-queue screen for `PENDING_REVIEW` listings
- Real AdMob IDs, release keystore, privacy policy, RLS policies for production
- Google Sign-In (removed in the Supabase migration)
