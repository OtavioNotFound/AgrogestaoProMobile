-- AgroGestão Pro: esquema offline-first e isolamento por usuário.
-- Execute no SQL Editor de um projeto Supabase novo ou aplique com o Supabase CLI.

create extension if not exists pgcrypto;

-- Preserva tabelas antigas cujo identificador não aceita os UUIDs usados pelo app.
do $$
declare
  table_name text;
  id_type text;
begin
  foreach table_name in array array['safras', 'tarefas', 'financeiro'] loop
    if to_regclass('public.' || table_name) is not null then
      select c.udt_name
        into id_type
        from information_schema.columns c
       where c.table_schema = 'public'
         and c.table_name = table_name
         and c.column_name = 'id';
      if coalesce(id_type, '') <> 'uuid' then
        execute format(
          'alter table public.%I rename to %I',
          table_name,
          table_name || '_legacy_20260802'
        );
      end if;
    end if;
  end loop;

  if to_regclass('public.produtores') is not null and not exists (
    select 1
      from information_schema.columns c
     where c.table_schema = 'public'
       and c.table_name = 'produtores'
       and c.column_name = 'user_id'
       and c.udt_name = 'uuid'
  ) then
    alter table public.produtores rename to produtores_legacy_20260802;
  end if;
end
$$;

create table if not exists public.produtores (
  user_id uuid primary key references auth.users(id) on delete cascade,
  email text not null default '',
  nome_produtor text not null default '',
  nome_propriedade text not null default '',
  municipio_uf text not null default '',
  dap_caf text not null default '',
  area_hectares double precision not null default 0,
  updated_at timestamptz not null default now()
);

create table if not exists public.safras (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  nome_cultura text not null default '',
  area_hectares double precision not null default 0,
  data_inicio date,
  previsao_colheita date,
  progresso integer not null default 0 check (progresso between 0 and 100),
  status_manejo text not null default '',
  updated_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

create table if not exists public.tarefas (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  titulo text not null default '',
  descricao text not null default '',
  categoria text not null default '',
  data_limite date,
  status text not null default 'A_FAZER'
    check (status in ('A_FAZER', 'EM_PROGRESSO', 'CONCLUIDO')),
  updated_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

create table if not exists public.financeiro (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  descricao text not null default '',
  valor double precision not null default 0 check (valor >= 0),
  tipo text not null default 'SAIDA' check (tipo in ('ENTRADA', 'SAIDA')),
  data date,
  categoria text not null default '',
  updated_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

create index if not exists produtores_user_updated_idx
  on public.produtores (user_id, updated_at);
create index if not exists safras_user_updated_idx
  on public.safras (user_id, updated_at);
create index if not exists tarefas_user_updated_idx
  on public.tarefas (user_id, updated_at);
create index if not exists financeiro_user_updated_idx
  on public.financeiro (user_id, updated_at);

-- Impede um aparelho com dados antigos de sobrescrever uma alteração mais recente.
create or replace function public.agro_keep_newest_update()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.updated_at < old.updated_at then
    return old;
  end if;
  return new;
end;
$$;

drop trigger if exists produtores_keep_newest on public.produtores;
create trigger produtores_keep_newest
before update on public.produtores
for each row execute function public.agro_keep_newest_update();

drop trigger if exists safras_keep_newest on public.safras;
create trigger safras_keep_newest
before update on public.safras
for each row execute function public.agro_keep_newest_update();

drop trigger if exists tarefas_keep_newest on public.tarefas;
create trigger tarefas_keep_newest
before update on public.tarefas
for each row execute function public.agro_keep_newest_update();

drop trigger if exists financeiro_keep_newest on public.financeiro;
create trigger financeiro_keep_newest
before update on public.financeiro
for each row execute function public.agro_keep_newest_update();

alter table public.produtores enable row level security;
alter table public.safras enable row level security;
alter table public.tarefas enable row level security;
alter table public.financeiro enable row level security;

drop policy if exists produtores_owner_all on public.produtores;
create policy produtores_owner_all on public.produtores
for all to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop policy if exists safras_owner_all on public.safras;
create policy safras_owner_all on public.safras
for all to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop policy if exists tarefas_owner_all on public.tarefas;
create policy tarefas_owner_all on public.tarefas
for all to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop policy if exists financeiro_owner_all on public.financeiro;
create policy financeiro_owner_all on public.financeiro
for all to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

revoke all on public.produtores, public.safras, public.tarefas, public.financeiro from anon;
grant select, insert, update, delete
  on public.produtores, public.safras, public.tarefas, public.financeiro
  to authenticated;

notify pgrst, 'reload schema';
