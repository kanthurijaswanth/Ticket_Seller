# Changelog

## v1.0 — first complete release

The first end-to-end version: the whole **buy → sell → pay → rate** loop works for both
roles, on a full glassmorphism redesign.

### Marketplace
- **UPI direct payment** — sellers add a UPI ID; buyers pay the seller directly through their
  UPI app. Real payment screen (replaced the old "coming soon" placeholder).
- **Order lifecycle fixed end-to-end** — reserve → pay → seller confirms → completed → rate;
  cancel restores the listing. Sellers now see incoming orders (Your Tickets → Sold).
- **Ratings** after completed orders, averaged onto profiles (new `ratings` table + repository).
- **Sell verification** — checks PNR, passenger, operator, route, date and price before posting.
- **Honest Verified / Pending badges** from real listing status; own-listing & sold/reserved
  states handled.
- Chat with **mutual-consent phone reveal** that persists; **Notifications** screen from real orders.

### Design
- **8 live themes** (Sunshine, Mint, Indigo, Rose, Ocean, Graphite, Classic, Monochrome) ×
  light/dark, switched live from Profile.
- **Glassmorphism** with **real backdrop blur** (Android 12+), clear borders + depth shadows,
  clean flat backgrounds, Plus Jakarta Sans + JetBrains Mono fonts.
- **Animated colour-mix splash** and an **adaptive gradient ticket logo**.
- Every screen redesigned; edge-to-edge, keyboard-safe forms, opaque flush bottom nav that keeps
  its tab on back-navigation.

### Platform
- **Play In-App Update** prompt + **In-App Review** ("Rate us").
- Build fixes (JDK/SDK config, Gradle), fonts bundled, `.gitignore` rewritten to UTF-8.
- Schema adds `users.upi_id`, `orders.payment_ref`, and the `ratings` table (see DATABASE.md).

### Reliability
- All database writes are guarded — the app no longer crashes on a backend error.
- Optimistic UI for "Share my number" and "I've paid" so they respond instantly.
- List screens skip the heavy base64 image column for faster loading.

### Known items for later
Escrow/commission, push notifications (FCM), photo cropping, an admin review queue, real AdMob
IDs, a release keystore, and production RLS policies. See the end of
[APP_OVERVIEW.md](APP_OVERVIEW.md).
