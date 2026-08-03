# TicketResell

**A two-sided marketplace for last-minute, non-cancellable bus & train tickets.**
_Someone else's seat, your trip._

Sell a ticket you can't use — the app reads the details from a photo, checks it isn't a
duplicate, and lists it. Buy a verified last-minute ticket, chat with the seller, and pay them
**directly over UPI**. One account does both.

Android · Jetpack Compose + Material 3 · Supabase (Postgres + Realtime + Auth) · on-device OCR ·
glassmorphism UI with 8 live themes.

---

## 📚 Documentation

| Doc | What's in it |
|---|---|
| **[docs/APP_OVERVIEW.md](docs/APP_OVERVIEW.md)** | What the app is, every feature built (v1.0), tech stack, project layout |
| **[docs/DATABASE.md](docs/DATABASE.md)** | Supabase setup, schema, **free-tier limits**, credentials, required migration |
| **[docs/SETUP.md](docs/SETUP.md)** | Get it building on a new machine |
| **[docs/CHANGELOG.md](docs/CHANGELOG.md)** | Release notes |

---

## Quick start

```bash
git clone https://github.com/kanthurijaswanth/Ticket_Seller.git
cd Ticket_Seller/android
./gradlew.bat :app:installDebug        # builds + installs on a connected device
```

Then in the app: **DEV: Skip sign-in** → **Profile → Seed demo tickets**.

> First build downloads ~1.5 GB of dependencies. You'll need a `local.properties` with your
> Android SDK path and a JDK 17+ — see [docs/SETUP.md](docs/SETUP.md).

> Data comes from Supabase. If Browse is empty, the project may be paused — see
> [docs/DATABASE.md](docs/DATABASE.md). To fully enable UPI-save & ratings, run
> [`supabase_schema.sql`](supabase_schema.sql) once in the Supabase SQL Editor.

---

## Highlights

- **UPI direct payment** — money goes straight buyer → seller, no gateway or fees
- **On-device OCR** auto-fills ticket details; **duplicate-image** detection
- **Realtime chat** with mutual-consent phone-number reveal
- **Ratings** after every completed order
- **8 themes** (light/dark) switchable live; frosted-glass UI with real backdrop blur
- **Play In-App Update + Review**

Built with Kotlin, Jetpack Compose, and Supabase. See the [docs](docs/) for everything.
