# 📋 FUTURE_WORK.md — Decisions, Open Questions & Next Steps

A short, plain-language log of everything discussed during build sessions so you can
pick up where you left off without re-reading hours of chat history.

Last updated: 2026‑05‑20

---

## 1. What is built and working today

- ✅ Yellow + white branded UI (Compose + Material 3)
- ✅ Login / Signup screens (email/password)
- ✅ **DEV: Skip sign-in** button — instant local bypass (remove before release)
- ✅ Browse, listing detail, Sell, Chat, My Tickets, Profile screens
- ✅ Supabase backend (Postgres + Realtime + Auth)
- ✅ On-device ticket OCR (ML Kit) — extracts PNR, source, destination, date
- ✅ Perceptual image hash duplicate detection
- ✅ "Seed demo tickets" dev tool — drops 10 realistic listings into Browse
- ✅ Gmail SMTP wired into Supabase for confirmation emails
- ✅ Friendly error mapping — "quota full" / "wrong password" etc.
- ✅ AdMob test ID in manifest (so app doesn't crash on launch)

---

## 2. Open decisions (you need to choose)

### 🔑 Email confirmation — ON or OFF?

| Choice | Effect |
|---|---|
| ON (current) | Users get a verification email, must click link before sign-in works. More secure. Requires Gmail SMTP. |
| OFF | Users can sign up and instantly sign in. Faster for dev/testing. No emails sent. |

To toggle: Supabase → Auth → Sign In / Providers → "Confirm email" → Save.

### 📧 SMTP provider — Gmail vs Brevo vs AWS SES

| Provider | Daily quota | Setup difficulty | When to switch |
|---|---|---|---|
| **Gmail (current)** | 500 emails/day | Easy (App Password) | Up to ~150 daily signups |
| Brevo | 300/day forever, 9,000/month | Easy (account + SMTP key) | Public launch with growth |
| AWS SES | 62,000/month free for 1 year | Medium (AWS account + verification) | Scaling past 500 signups/day |

---

## 3. Things explicitly NOT built (intentional MVP cuts)

| Feature | Why skipped | Effort to add |
|---|---|---|
| In-app payment (Razorpay / Cashfree) | Marked as "Coming soon" until verification is battle-tested | ~2 days |
| Ratings UI (post-purchase) | Model exists, screen not built | ~half day |
| Push notifications (FCM) | Not critical for MVP | ~half day |
| Photo cropping before upload | Uploads as-is; works fine | ~2 hours |
| Admin / review queue | Pending listings live in DB, no UI yet | ~1 day |
| Real AdMob ads | Using test App ID; needs AdMob account | ~half day |
| Release-signed APK / AAB | Need to generate keystore | ~half day |
| Privacy policy page | Required for Play Store | ~2 hours |
| Google Sign-In | Removed during Supabase migration | ~half day to re-add via OAuth |
| Phone OTP login | Not free anywhere; skipped | ~1 day |

---

## 4. Going to Play Store — checklist

When ready to publish:

1. ☐ Pay the **one-time $25** Google Play developer fee
2. ☐ Generate a **release keystore** (`.jks`) and back it up SECURELY — losing it = losing the app
3. ☐ Replace AdMob **test App ID** with a real one (created at admob.google.com)
4. ☐ Write a **privacy policy** (host free on GitHub Pages or Notion)
5. ☐ Take **8 screenshots** of the app for the Play Store listing
6. ☐ Create a **1024 × 500 feature graphic** (Canva is fine)
7. ☐ Write a short description (80 chars) + long description (4,000 chars)
8. ☐ Fill out the **content rating questionnaire** (5 min)
9. ☐ Fill out the **target audience declaration** (5 min)
10. ☐ Run a **closed beta** for ~14 days (Google now requires this for new dev accounts since Nov 2023)
11. ☐ Build the release AAB: `./gradlew :app:bundleRelease`
12. ☐ Upload AAB to Play Console → submit for review
13. ☐ First review takes 2–7 days; updates after that approve in hours

---

## 5. Monetization plan (when there are real users)

Pick any mix:

| Stream | Setup effort | Income potential |
|---|---|---|
| **AdMob banners on Browse** | ~2 hours | Steady, low (~₹15–60 / 1K views in India) |
| **AdMob interstitial after Sell submit** | ~1 hour | Higher per impression |
| **Rewarded ad to "boost listing"** | ~half day | Highest paying ad type |
| **Commission per sale (5–10%)** | ~2 days (Razorpay integration) | Best long-term — Razorpay takes 2%, you take 5–8% |
| **Subscription for premium buyers** (early access to listings) | ~1 day | High margin, requires user critical mass |
| **Paid "Boost listing" feature** (₹10–20 per pin) | ~half day | Small but recurring |

---

## 6. Cost trajectory (what to expect)

| Daily Active Users | Supabase cost | Gmail SMTP status | Action needed |
|---|---|---|---|
| 0–500 (current) | ₹0 (free tier) | Comfortable | Nothing |
| 500–5,000 | ₹0 (still under 50K MAU) | Need to upgrade SMTP — Brevo or SES | Switch SMTP provider |
| 5,000–50,000 | ₹0 (just under cap) | Brevo Pro (₹1,500) | Pay Brevo |
| 50,000+ | Supabase Pro ₹2,100/mo | AWS SES (~₹500/mo) | Pay both |

Compare to Firebase: at 10K DAU you'd pay ~₹6,000/mo. Supabase is roughly 3× cheaper.

---

## 7. Migration options (when Supabase gets expensive)

If Supabase ever becomes too expensive (only at ~50K+ MAU):

| Option | Effort | Monthly cost | Trade-off |
|---|---|---|---|
| Self-host on Hetzner VPS | ~1 day | ₹410 flat | You manage uptime + backups |
| Switch to Cloudflare D1 + R2 + Workers | ~1 week | ₹0 forever (huge free tier) | Custom backend code |
| Stay on Supabase Pro | 0 | ₹2,100/mo | Best dev experience |

Since you own your data (Postgres = standard SQL), migration is straightforward at any point.

---

## 8. Credentials checklist (where they live)

| Credential | File | Status |
|---|---|---|
| Supabase URL + anon key | `SupabaseConfig.kt` + `CREDENTIALS.md` | ✅ Set |
| Postgres password | `CREDENTIALS.md` only | ⚠️ Add yours |
| Gmail account + App Password | `CREDENTIALS.md` + Supabase Dashboard SMTP | ⚠️ Add yours |
| AdMob real App ID | `CREDENTIALS.md` (later) | ❌ Not created |
| Play Store keystore | Secure backup + `CREDENTIALS.md` | ❌ Not created |

`CREDENTIALS.md` is in `.gitignore` — never gets pushed to GitHub.

---

## 9. Quick decisions log

| Date | Question | Decided |
|---|---|---|
| 2026-05-19 | Firebase vs Supabase? | **Supabase** — better free tier (50K MAU vs 150 DAU) |
| 2026-05-19 | Phone OTP login? | **No** — costs money, skip until later |
| 2026-05-19 | Cloud Storage for images? | **No** — base64 inside Postgres works for now |
| 2026-05-19 | Brand colour? | **Yellow + white** (RedBus-inspired but distinct) |
| 2026-05-19 | Real AdMob ads from day 1? | **No** — test ID for now, real ads after first 50 users |
| 2026-05-20 | SMTP provider? | **Gmail** (500/day, free) — switch to Brevo later |
| 2026-05-20 | Email confirmation? | **Keep ON** but with friendly error if quota exceeded |

---

## 10. Notes for next session

When you come back to this project:

1. **Read README.md first** — full project overview and setup
2. **Read CREDENTIALS.md** — every secret value in one place
3. **Read THIS file** — what was discussed and what's left
4. Run `./gradlew :app:installDebug` to install on a connected phone
5. Tap **DEV: Skip sign-in** to bypass auth and test the app instantly

---

If you want me to pick up any of the "things not built" items, just say the name. Each is independent — we can knock them out one at a time.
