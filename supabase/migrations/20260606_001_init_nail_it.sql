create extension if not exists "pgcrypto";

create table if not exists public.sessions (
  id uuid primary key default gen_random_uuid(),
  install_id text not null,
  status text not null default 'draft',
  source_type text not null,
  source_url text,
  style_name text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.session_assets (
  id uuid primary key default gen_random_uuid(),
  session_id uuid not null references public.sessions(id) on delete cascade,
  asset_type text not null,
  storage_path text not null,
  mime_type text,
  sort_order integer not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.source_parses (
  session_id uuid primary key references public.sessions(id) on delete cascade,
  model text,
  version text,
  parse_json jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists public.try_on_results (
  session_id uuid primary key references public.sessions(id) on delete cascade,
  model text,
  version text,
  result_image_path text,
  result_json jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists public.bom_lists (
  session_id uuid primary key references public.sessions(id) on delete cascade,
  bom_json jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists public.sop_guides (
  session_id uuid primary key references public.sessions(id) on delete cascade,
  version text,
  sop_json jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists public.prompt_versions (
  id uuid primary key default gen_random_uuid(),
  prompt_type text not null,
  version text not null,
  description text,
  prompt_template text,
  created_at timestamptz not null default now()
);

create index if not exists idx_sessions_install_id on public.sessions(install_id);
create index if not exists idx_sessions_status on public.sessions(status);
create index if not exists idx_session_assets_session_id on public.session_assets(session_id);
create index if not exists idx_session_assets_asset_type on public.session_assets(asset_type);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists sessions_set_updated_at on public.sessions;
create trigger sessions_set_updated_at
before update on public.sessions
for each row
execute function public.set_updated_at();

alter publication supabase_realtime add table public.sessions;
alter publication supabase_realtime add table public.source_parses;
alter publication supabase_realtime add table public.try_on_results;
alter publication supabase_realtime add table public.bom_lists;
alter publication supabase_realtime add table public.sop_guides;

insert into storage.buckets (id, name, public)
values ('nail-it-assets', 'nail-it-assets', false)
on conflict (id) do nothing;
