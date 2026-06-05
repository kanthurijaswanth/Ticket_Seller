-- ─────────────────────────────────────────────────────────────────────────
-- TicketResell — Supabase / Postgres schema
--
-- HOW TO USE:
--   1. Go to https://supabase.com → sign in (free, no card needed)
--   2. Click "New project"
--      • Name: ticketresell
--      • Database password: pick anything strong, write it down
--      • Region: pick closest (e.g. ap-south-1 Mumbai)
--      • Plan: Free
--   3. Once the project is provisioned (~2 min), open it
--   4. Top-left menu → SQL Editor → New query
--   5. Paste THIS ENTIRE FILE and click Run
--   6. Top-left menu → Project Settings → API
--        • Copy "Project URL"     → SUPABASE_URL  in SupabaseConfig.kt
--        • Copy "anon public" key → SUPABASE_ANON_KEY in SupabaseConfig.kt
--   7. Rebuild + install the app. Done.
-- ─────────────────────────────────────────────────────────────────────────

-- ─── users ────────────────────────────────────────────────────────────────
create table if not exists public.users (
    uid           text primary key,
    email         text not null default '',
    name          text not null default '',
    phone         text not null default '',
    photo_url     text not null default '',
    rating_avg    double precision not null default 0,
    rating_count  integer not null default 0,
    created_at    bigint not null default (extract(epoch from now()) * 1000)::bigint
);

-- ─── listings ─────────────────────────────────────────────────────────────
create table if not exists public.listings (
    id                    text primary key,
    seller_id             text not null,
    seller_name           text not null default '',
    seller_rating         double precision not null default 0,
    ticket                jsonb not null default '{}'::jsonb,
    address               jsonb not null default '{}'::jsonb,
    asking_price          bigint not null default 0,
    ticket_image_base64   text not null default '',
    ticket_image_hash     text not null default '',
    verification_notes    text not null default '',
    verification_flags    jsonb not null default '[]'::jsonb,
    status                text not null default 'PENDING_REVIEW',
    created_at            bigint not null default (extract(epoch from now()) * 1000)::bigint,
    updated_at            bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create index if not exists idx_listings_status            on public.listings (status);
create index if not exists idx_listings_seller            on public.listings (seller_id);
create index if not exists idx_listings_ticket_image_hash on public.listings (ticket_image_hash);
create index if not exists idx_listings_journey
    on public.listings ( ((ticket->>'journey_date_millis')::bigint) );

-- ─── chat_threads ─────────────────────────────────────────────────────────
create table if not exists public.chat_threads (
    id                      text primary key,
    listing_id              text not null,
    listing_title           text not null default '',
    buyer_id                text not null,
    seller_id               text not null,
    buyer_name              text not null default '',
    seller_name             text not null default '',
    last_message            text not null default '',
    last_sender_id          text not null default '',
    updated_at              bigint not null default (extract(epoch from now()) * 1000)::bigint,
    buyer_consent_reveal    boolean not null default false,
    seller_consent_reveal   boolean not null default false,
    buyer_phone             text not null default '',
    seller_phone            text not null default ''
);
create index if not exists idx_chat_threads_buyer  on public.chat_threads (buyer_id);
create index if not exists idx_chat_threads_seller on public.chat_threads (seller_id);

-- ─── chat_messages ────────────────────────────────────────────────────────
create table if not exists public.chat_messages (
    id          text primary key,
    thread_id   text not null,
    sender_id   text not null,
    text        text not null default '',
    type        text not null default 'TEXT',
    sent_at     bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index if not exists idx_chat_messages_thread on public.chat_messages (thread_id);

-- ─── orders ───────────────────────────────────────────────────────────────
create table if not exists public.orders (
    id          text primary key,
    listing_id  text not null,
    buyer_id    text not null,
    seller_id   text not null,
    amount      bigint not null default 0,
    status      text not null default 'INITIATED',
    created_at  bigint not null default (extract(epoch from now()) * 1000)::bigint,
    updated_at  bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index if not exists idx_orders_buyer  on public.orders (buyer_id);
create index if not exists idx_orders_seller on public.orders (seller_id);

-- ─── Row Level Security (open for dev — TIGHTEN BEFORE PRODUCTION) ────────
-- During development we let the "anon" key read/write everything so the
-- dev-bypass flow works without authenticating against Supabase. Before
-- you ship to real users, add RLS policies that require auth.uid().
alter table public.users          disable row level security;
alter table public.listings       disable row level security;
alter table public.chat_threads   disable row level security;
alter table public.chat_messages  disable row level security;
alter table public.orders         disable row level security;

-- ─── Enable realtime for live updates ─────────────────────────────────────
-- Make sure these tables are in the supabase_realtime publication so the
-- app's snapshot listeners receive events.
alter publication supabase_realtime add table public.users;
alter publication supabase_realtime add table public.listings;
alter publication supabase_realtime add table public.chat_threads;
alter publication supabase_realtime add table public.chat_messages;
alter publication supabase_realtime add table public.orders;
