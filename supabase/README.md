# Configuração do Supabase

O aplicativo usa autenticação do Supabase e sincronização bidirecional de perfil,
safras, tarefas e lançamentos financeiros. Cada linha é isolada pelo `user_id`
do usuário autenticado e protegida por Row Level Security (RLS).

## Preparar o backend

1. Crie ou abra um projeto válido no Supabase.
2. No SQL Editor, execute, em ordem, os arquivos da pasta `migrations`.
3. Em **Project Settings > API**, copie a Project URL e a chave pública/anon.
4. Em **Authentication > URL Configuration**, adicione
   `com.agrogestao.pro://auth/callback` às URLs de redirecionamento permitidas.
   Esse endereço é usado para devolver a confirmação de e-mail diretamente ao
   aplicativo Android.
5. Acrescente ao `local.properties` da raiz do projeto:

```properties
SUPABASE_URL=https://SEU_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=SUA_CHAVE_PUBLICA
```

Também é possível fornecer `SUPABASE_URL` e `SUPABASE_ANON_KEY` como variáveis de
ambiente durante o build. Nunca use a chave `service_role` no aplicativo.

Para builds locais, o Gradle também reconhece um arquivo `supabase.md` ignorado
pelo Git, com os campos `API URL` e `publishable key`. O campo `secret key` não é
copiado para o aplicativo. A URL pode ser a raiz do projeto ou terminar em
`/rest/v1/`; o build normaliza ambos os formatos.

O aplicativo detecta automaticamente o esquema legado baseado em `user_email` e
mantém a sincronização básica compatível. Aplique as migrations para obter o
esquema recomendado, com tombstones e associações de safra sincronizadas entre
aparelhos.

Se a confirmação de e-mail estiver habilitada no Supabase, mantenha o redirect
exato na allow list. Sem essa configuração, o provedor pode enviar o usuário para
o `SITE_URL` padrão em vez de reabrir o AgroGestão Pro. Consulte a documentação
oficial do Supabase sobre [Redirect URLs](https://supabase.com/docs/guides/auth/redirect-urls).

## Estratégia de sincronização

- UUID permanente por registro, independente do ID local do Room;
- download antes do envio para detectar alterações feitas em outro aparelho;
- `updated_at` como critério de conflito: a alteração mais recente vence;
- exclusões são tombstones na nuvem, permitindo propagação para outros aparelhos;
- RLS restringe leitura e escrita ao proprietário autenticado;
- WorkManager tenta novamente quando a rede volta.
