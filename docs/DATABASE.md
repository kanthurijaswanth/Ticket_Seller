# Database & Backend

TicketResell has **no custom backend server**. The Android app talks **directly** to
[Supabase](https://supabase.com), which provides the database, realtime updates, and auth.

```
Phone A ─┐                        ┌─ Phone B
         ├──── HTTPS / WSS ──────►│
         │                        │
         ▼                        ▼
   ┌───────────────────────────────────┐
   │            Supabase               │
   │  Postgres · Realtime · Auth       │
   └───────────────────────────────────┘
```

- **Postgres** — all persistent data (users, listings, chats, orders, ratings)
- **Realtime** — live updates for listings, chat threads/messages, orders
- **Auth (GoTrue)** — email/password sign-up & sign-in (optional; the app also has a dev bypass)
- On-device **OCR** (Google ML Kit) reads ticket photos — no server, no API calls
- **Payments** are peer-to-peer over **UPI** — money never touches a server (see [APP_OVERVIEW](APP_OVERVIEW.md))

---

## Where the credentials live

| Value | Location | Committed to git? |
|---|---|---|
| Supabase Project URL | `android/app/src/main/java/com/ticketseller/app/data/SupabaseConfig.kt` | ✅ Yes (URL is public) |
| Supabase anon/publishable key | same file | ✅ Yes (anon key is safe to ship) |
| `service_role` key / DB password | **never in the repo** | ❌ Never |

The **anon key is designed to be public** — what it can do is limited by the database's
Row-Level-Security policies. The **service_role key bypasses all security**; never put it in
the app or any committed file.

---

## Tables

All primary keys are `text` (client-generated UUIDs). All timestamps are `bigint` epoch-millis.
The full, runnable schema is in [`../supabase_schema.sql`](../supabase_schema.sql).

### `users`
| column | type | notes |
|---|---|---|
| uid | text | primary key |
| email | text | |
| name | text | |
| phone | text | shared only after mutual consent in chat |
| upi_id | text | seller's UPI ID, to receive payments |
| photo_url | text | |
| rating_avg | double precision | recomputed when a rating is left |
| rating_count | integer | |
| created_at | bigint | |

### `listings`
| column | type | notes |
|---|---|---|
| id | text | primary key |
| seller_id / seller_name / seller_rating | text / text / double | denormalised seller info |
| ticket | jsonb | `TicketDetails` (type, pnr, operator, source, destination, journey_date_millis, …) |
| address | jsonb | `PickupDropAddress` (pickup/drop line, city, pincode, …) |
| asking_price | bigint | in paise (₹1 = 100) |
| ticket_image_base64 | text | the ticket photo, base64 (list screens skip this column for speed) |
| ticket_image_hash | text | 16×16 perceptual hash, for duplicate detection |
| verification_notes / verification_flags | text / jsonb | output of the verifier |
| status | text | `PENDING_REVIEW`, `ACTIVE`, `RESERVED`, `SOLD`, `REJECTED`, `EXPIRED` |
| created_at / updated_at | bigint | |

### `chat_threads`
`id` (text PK) · `listing_id` · `listing_title` · `buyer_id` · `seller_id` · `buyer_name` ·
`seller_name` · `last_message` · `last_sender_id` · `updated_at` ·
`buyer_consent_reveal` (bool) · `seller_consent_reveal` (bool) · `buyer_phone` · `seller_phone`

### `chat_messages`
`id` (text PK) · `thread_id` · `sender_id` · `text` · `type` (`TEXT` / `SYSTEM` / …) · `sent_at`

### `orders`
`id` (text PK) · `listing_id` · `buyer_id` · `seller_id` · `amount` (paise) ·
`status` (`AWAITING_PAYMENT`, `PAID_OFFLINE`, `COMPLETED`, `CANCELLED`, …) ·
`payment_ref` · `created_at` · `updated_at`

### `ratings`
`id` (text PK) · `order_id` · `rater_id` · `ratee_id` · `stars` (int) · `comment` · `created_at`

> **Row-Level Security is disabled** on all tables for development so the anon key can
> read/write. **Tighten this before a public launch** by adding RLS policies keyed on `auth.uid()`.

---

## Setting up a fresh Supabase project

1. Go to **supabase.com → New project** (name it e.g. `ticketresell`, region **ap-south-1 / Mumbai**, **Free** plan). Save the database password somewhere safe.
2. When it's ready: **Project Settings → API** → copy the **Project URL** and the **anon public** key.
3. Paste both into `SupabaseConfig.kt`.
4. Open **SQL Editor → New query**, paste all of [`../supabase_schema.sql`](../supabase_schema.sql), and **Run**. This creates every table and enables realtime.
5. Rebuild and install the app.

> Free Supabase projects **pause after ~1 week of inactivity** and are deleted later. If the
> app suddenly can't load data, open the dashboard and **Restore** the project (DNS takes ~30 s
> to propagate afterwards).

### ⚠️ Required migration (if your project predates these features)

If the project was created before UPI/ratings existed, three things are missing:
`users.upi_id`, `orders.payment_ref`, and the `ratings` table. **The app still runs** (browsing,
chat, buying, paying all work), but **saving a UPI ID and leaving ratings won't persist** until
you run this once in **SQL Editor** (safe to re-run):

```sql
alter table public.users  add column if not exists upi_id      text not null default '';
alter table public.orders add column if not exists payment_ref text not null default '';

create table if not exists public.ratings (
    id          text primary key,
    order_id    text not null,
    rater_id    text not null,
    ratee_id    text not null,
    stars       integer not null default 5,
    comment     text not null default '',
    created_at  bigint  not null default (extract(epoch from now()) * 1000)::bigint
);
create index if not exists idx_ratings_ratee on public.ratings (ratee_id);
create index if not exists idx_ratings_order on public.ratings (order_id);
alter table public.ratings disable row level security;
```

---

## Free-tier limits (Supabase)

| Resource | Free quota | What it means for this app |
|---|---|---|
| Postgres storage | 500 MB | ~3,000 listings (images are base64 in rows) |
| Realtime concurrent connections | 200 | 200 phones with the app open at once |
| Monthly active users | 50,000 | plenty for an early-stage app |
| Database bandwidth | 5 GB / month | fine for early stage |
| Edge function invocations | 500,000 / month | not used today |
| Auth users | unlimited on free | |

**Email (Gmail SMTP, if you enable email confirmation):** 500 emails/day. When exceeded, sign-up
shows a friendly "email quota is full, try tomorrow" message.

### Cost trajectory

| Monthly active users | Supabase cost | Notes |
|---|---|---|
| 0 – 50,000 | ₹0 (Free) | current |
| 50,000 – 100,000 | ~₹2,100/mo (Pro) | |
| 100,000+ | ₹50,000+/mo (Team) or self-host (~₹400/mo VPS) | data is standard SQL, so migration is easy |

---

## Scaling notes / known simplifications

- **Images are base64 inside Postgres rows.** Simple, but bloats storage/bandwidth. For scale,
  upload to **Supabase Storage** and store the URL instead (the Storage SDK is already wired up).
  List screens already avoid fetching the image column to stay fast.
- **Realtime re-fetches the whole result set** on any table change (no server-side row filtering).
  Fine at small scale; add filtering if it gets chatty.
- **No foreign keys** — relations are loose `text` id references.
