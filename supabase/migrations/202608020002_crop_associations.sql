-- Associa tarefas e lançamentos financeiros a uma safra pelo UUID de nuvem.

begin;

alter table public.tarefas
  add column if not exists crop_id uuid;

alter table public.financeiro
  add column if not exists crop_id uuid;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'safras_id_user_unique'
  ) then
    alter table public.safras
      add constraint safras_id_user_unique unique (id, user_id);
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'tarefas_crop_owner_fk'
  ) then
    alter table public.tarefas
      add constraint tarefas_crop_owner_fk
      foreign key (crop_id, user_id)
      references public.safras (id, user_id)
      on delete set null (crop_id);
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'financeiro_crop_owner_fk'
  ) then
    alter table public.financeiro
      add constraint financeiro_crop_owner_fk
      foreign key (crop_id, user_id)
      references public.safras (id, user_id)
      on delete set null (crop_id);
  end if;
end
$$;

create index if not exists tarefas_user_crop_idx
  on public.tarefas (user_id, crop_id);

create index if not exists financeiro_user_crop_idx
  on public.financeiro (user_id, crop_id);

notify pgrst, 'reload schema';

commit;
