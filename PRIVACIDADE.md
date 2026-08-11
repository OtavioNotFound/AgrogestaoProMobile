# Aviso de privacidade — versão de trabalho para piloto

**Aplicativo:** AgroGestão Pro  
**Atualizado em:** 4 de agosto de 2026

Este texto descreve o comportamento técnico atual do aplicativo. Antes de publicação comercial, o controlador responsável deve preencher seus dados de contato e obter revisão jurídica brasileira.

## Dados tratados

- perfil informado pelo usuário: nome, e-mail, propriedade, município/UF, área e CAF/DAP opcional;
- talhões/safras, tarefas, prazos e registros financeiros;
- identificadores técnicos, estado e horário de sincronização;
- consentimento, metadados e integridade de relatórios;
- município/UF somente se a consulta opcional de clima for autorizada.

Senhas não são armazenadas pelo aplicativo. Tokens de sessão ficam cifrados pelo Android Keystore. O aplicativo não pede GPS, contatos, microfone ou câmera.

## Finalidades e compartilhamentos

Os dados são usados para as funções solicitadas: gestão local, backup, sincronização opcional, lembretes, exportação e relatórios. Quando a nuvem está configurada, perfil e registros operacionais são enviados ao Supabase da instalação. Quando o clima é autorizado, o município/UF é enviado ao Open-Meteo para geocodificação e previsão. Um relatório ou CSV só sai do armazenamento do app por ação explícita do usuário no seletor de arquivos/compartilhamento do Android.

## Armazenamento e segurança

O banco principal fica no espaço privado do aplicativo, mas não possui cifragem própria. O backup `.agrobackup` é cifrado com senha. PDFs arquivados têm hash SHA-256 para detectar alteração. Políticas RLS isolam contas no esquema Supabase moderno. Nenhuma dessas medidas substitui auditoria independente ou proteção do próprio aparelho.

## Controles do usuário

- trabalhar somente no aparelho, sem criar conta remota;
- exportar backup, PDF e CSV;
- revogar consentimento de relatório;
- desativar o clima e apagar a previsão salva;
- sair da conta e pausar lembretes.

O fluxo operacional para acesso, correção, portabilidade e eliminação no backend precisa ser definido pelo controlador antes de disponibilização pública. Cópias já compartilhadas com terceiros não podem ser recolhidas pelo aplicativo.

## Retenção e incidentes

Não há prazo automático de retenção nesta beta. Dados locais permanecem até exclusão pelo usuário, limpeza do app ou desinstalação; tombstones remotos podem permanecer pelo período necessário à sincronização. O responsável pela operação deve definir canal ao titular, prazo de atendimento, rotina de exclusão no Supabase e plano de resposta a incidentes antes do piloto ampliado.

## Serviços externos

- Supabase: autenticação e sincronização opcionais;
- Open-Meteo/GeoNames: geocodificação e previsão opcional por município;
- Android/Google: sistema operacional, backup do dispositivo e seletor de compartilhamento, conforme a configuração do aparelho.

## Contato do controlador

**Pendente antes de distribuição:** nome/razão social, CPF/CNPJ, endereço, e-mail de privacidade e encarregado/canal LGPD.
