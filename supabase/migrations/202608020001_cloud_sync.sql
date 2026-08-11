-- AgroGestão Pro: esquema offline-first e isolamento por usuário.
-- Execute no SQL Editor de um projeto Supabase novo ou aplique com o Supabase CLI.

begin;

create extension if not exists pgcrypto;

-- Preserva qualquer tabela do esquema legado. Algumas instalações antigas já
-- usavam UUID no id, mas ainda identificavam o dono por user_email; olhar apenas
-- o tipo do id deixava o banco num estado misto e interrompia a sincronização.
do $$
declare
  candidate_table_name text;
  legacy_table_name text;
  has_user_id boolean;
begin
  foreach candidate_table_name in array array['safras', 'tarefas', 'financeiro'] loop
    if to_regclass('public.' || candidate_table_name) is not null then
      select exists (
        select 1
          from information_schema.columns c
         where c.table_schema = 'public'
           and c.table_name = candidate_table_name
           and c.column_name = 'user_id'
           and c.udt_name = 'uuid'
      ) into has_user_id;
      if not has_user_id then
        legacy_table_name := candidate_table_name || '_legacy_20260802';
        if to_regclass('public.' || legacy_table_name) is not null then
          raise exception 'A tabela de backup public.% já existe; migração interrompida para proteger os dados.',
            legacy_table_name;
        end if;
        execute format(
          'alter table public.%I rename to %I',
          candidate_table_name,
          legacy_table_name
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
    if to_regclass('public.produtores_legacy_20260802') is not null then
      raise exception 'A tabela de backup public.produtores_legacy_20260802 já existe; migração interrompida para proteger os dados.';
    end if;
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

-- Copia o conteúdo legado para as novas tabelas. As tabelas com sufixo
-- _legacy_20260802 permanecem intactas como backup recuperável.
do $$
begin
  if to_regclass('public.produtores_legacy_20260802') is not null then
    execute $copy$
      insert into public.produtores (
        user_id, email, nome_produtor, nome_propriedade, municipio_uf,
        dap_caf, area_hectares, updated_at
      )
      select
        u.id,
        coalesce(l.user_email, u.email, ''),
        coalesce(l.nome_produtor, ''),
        coalesce(l.nome_propriedade, ''),
        coalesce(l.municipio_uf, ''),
        coalesce(l.dap_caf, ''),
        coalesce(l.area_hectares, 0),
        coalesce(nullif(l.atualizado_em::text, '')::timestamptz, now())
      from public.produtores_legacy_20260802 l
      join auth.users u on u.id = l.id
      on conflict (user_id) do update set
        email = excluded.email,
        nome_produtor = excluded.nome_produtor,
        nome_propriedade = excluded.nome_propriedade,
        municipio_uf = excluded.municipio_uf,
        dap_caf = excluded.dap_caf,
        area_hectares = excluded.area_hectares,
        updated_at = greatest(public.produtores.updated_at, excluded.updated_at)
    $copy$;
  end if;

  if to_regclass('public.safras_legacy_20260802') is not null then
    execute $copy$
      insert into public.safras (
        id, user_id, nome_cultura, area_hectares, data_inicio,
        previsao_colheita, progresso, status_manejo, updated_at, is_deleted
      )
      select
        l.id,
        u.id,
        coalesce(l.nome_cultura, ''),
        coalesce(l.area_hectares, 0),
        nullif(l.data_inicio::text, '')::date,
        nullif(l.previsao_colheita::text, '')::date,
        coalesce(l.progresso, 0)::integer,
        coalesce(l.status_manejo, ''),
        coalesce(nullif(l.atualizado_em::text, '')::timestamptz, now()),
        false
      from public.safras_legacy_20260802 l
      join auth.users u on lower(u.email) = lower(l.user_email)
      on conflict (id) do nothing
    $copy$;
  end if;

  if to_regclass('public.tarefas_legacy_20260802') is not null then
    execute $copy$
      insert into public.tarefas (
        id, user_id, titulo, descricao, categoria, data_limite,
        status, updated_at, is_deleted
      )
      select
        l.id,
        u.id,
        coalesce(l.titulo, ''),
        coalesce(l.descricao, ''),
        coalesce(l.categoria, ''),
        nullif(l.data_limite::text, '')::date,
        case
          when l.status in ('A_FAZER', 'EM_PROGRESSO', 'CONCLUIDO') then l.status
          else 'A_FAZER'
        end,
        coalesce(nullif(l.atualizado_em::text, '')::timestamptz, now()),
        false
      from public.tarefas_legacy_20260802 l
      join auth.users u on lower(u.email) = lower(l.user_email)
      on conflict (id) do nothing
    $copy$;
  end if;

  if to_regclass('public.financeiro_legacy_20260802') is not null then
    execute $copy$
      insert into public.financeiro (
        id, user_id, descricao, valor, tipo, data, categoria,
        updated_at, is_deleted
      )
      select
        l.id,
        u.id,
        coalesce(l.descricao, ''),
        greatest(coalesce(l.valor, 0), 0),
        case when l.tipo = 'ENTRADA' then 'ENTRADA' else 'SAIDA' end,
        nullif(l.data::text, '')::date,
        coalesce(l.categoria, ''),
        coalesce(nullif(l.atualizado_em::text, '')::timestamptz, now()),
        false
      from public.financeiro_legacy_20260802 l
      join auth.users u on lower(u.email) = lower(l.user_email)
      on conflict (id) do nothing
    $copy$;
  end if;
end
$$;

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

commit;
