# Plano de expansão - AgroGestão Pro

## Diagnóstico atual

O aplicativo já possui uma base funcional robusta de validação em Android nativo:

- Kotlin, Jetpack Compose e Material 3;
- persistência local com Room;
- cadastro/login pelo Supabase;
- dashboard financeiro;
- Kanban de tarefas;
- cadastro de safras e movimentações financeiras;
- relatório informativo para crédito rural em PDF.

Ainda não é um produto pronto para produção. A sincronização offline-first, as exclusões, os conflitos, o backup e o PDF já foram implementados e validados. As lacunas principais agora são o histórico verificável dos relatórios, medição de desempenho em aparelho de entrada, revisão jurídica/agronômica do documento e piloto com produtores reais.

## Prioridade 0 - estabilização e proteção de dados

Objetivo: tornar o núcleo confiável antes de ampliar funcionalidades.

1. Concluir autenticação e sessão:
   - restaurar sessão ao abrir o app;
   - implementar logout visível;
   - tratar confirmação de e-mail e expiração/renovação do token;
   - nunca gravar token ou resposta de autenticação em logs;
   - separar perfil local por usuário.
2. Tornar a sincronização realmente offline-first:
   - adicionar UUID estável, `updatedAt`, estado de sincronização e marca de exclusão;
   - enviar uma fila de mudanças com WorkManager;
   - baixar alterações remotas;
   - definir estratégia de conflito;
   - atualizar o indicador visual somente após confirmação do servidor;
   - sincronizar edições, mudanças de status e exclusões.
3. Proteger o banco:
   - substituir `fallbackToDestructiveMigration()` por migrations versionadas;
   - exportar e testar schemas do Room;
   - adicionar backup/exportação controlada;
   - armazenar credenciais com segurança e retirar configuração do backend do código-fonte.
4. Criar testes mínimos:
   - validação de formulários;
   - cálculos financeiros;
   - DAOs e migrations;
   - autenticação e sincronização com cliente HTTP falso;
   - fluxo instrumentado de cadastro, tarefa, safra e lançamento.

Critério de saída: nenhuma perda de perfil/dados, build reproduzível, testes automatizados e modo avião validado.

## Prioridade 1 - MVP utilizável no campo

Objetivo: substituir valores de demonstração por operações reais.

1. Datas reais com seletor, armazenamento em formato ISO e exibição `dd/MM/yyyy`.
2. Edição e confirmação antes de excluir safras, tarefas e lançamentos.
3. Estados vazios, mensagens de sucesso/erro e possibilidade de tentar sincronizar novamente.
4. Associação entre safra, tarefas, custos e receitas.
5. Progresso da safra editável, área e status de manejo estruturados.
6. Filtros por período, safra, categoria e situação.
7. Acessibilidade:
   - alvos de toque de pelo menos 48 dp;
   - contraste e escalonamento de fonte;
   - descrições para leitores de tela;
   - teste em tela pequena, luz intensa e uso com uma mão.
8. Desempenho em aparelho de entrada:
   - Baseline Profile;
   - listas paginadas quando necessário;
   - imagens e dependências mínimas;
   - medição de inicialização, memória e uso de bateria.

Critério de saída: produtor consegue trabalhar vários dias offline, reencontrar dados e compreender qualquer falha.

## Prioridade 2 - relatório de crédito rural

Objetivo: entregar valor verificável para produtor e instituição financeira.

1. Definir com especialista quais dados sustentam um relatório PRONAF.
2. Remover afirmações automáticas como “elegível/risco baixo” baseadas apenas em margem positiva.
3. Gerar PDF com:
   - identificação e consentimento do produtor;
   - período analisado;
   - safras, área, produção, receitas, custos e margem;
   - origem e completude dos dados;
   - aviso de que o relatório não garante aprovação de crédito.
4. Compartilhar, imprimir e manter histórico de versões.
5. Registrar trilha de auditoria e integridade do documento.

Critério de saída: relatório reproduzível, conferível e juridicamente revisado.

## Prioridade 3 - expansão operacional

1. Gestão de insumos, estoque, fornecedores e aplicação por talhão.
2. Cadastro de propriedades, talhões e múltiplos usuários.
3. Agenda climática e alertas de manejo com fonte identificada.
4. Delegação de tarefas para equipe/família.
5. Exportação CSV e integração contábil.
6. Portal opcional para cooperativas e assistência técnica, sempre com consentimento.

## Prioridade 4 - SaaS e crescimento

1. Telemetria opt-in e compatível com LGPD.
2. Planos simples, teste gratuito e cobrança tolerante à sazonalidade.
3. Administração de organizações/cooperativas.
4. Métricas de ativação, retenção e sucesso antes de CAC/LTV.
5. Pilotos regionais com sindicatos, cooperativas e agentes de crédito.

## Sequência recomendada

- Ciclo 1 (agora): build, perda de dados em login/edição, validações e IDs locais.
- Ciclo 2: sessão persistente, migrations e testes de banco.
- Ciclo 3: fila de sincronização com WorkManager e estados confiáveis.
- Ciclo 4: datas, edição, exclusão segura e acessibilidade.
- Ciclo 5: PDF de crédito com revisão de domínio e LGPD.
- Ciclo 6: piloto de campo, métricas e correções orientadas por uso real.

Novas funcionalidades devem entrar somente quando o critério de saída do ciclo anterior estiver atendido.

## Progresso de implementação

Concluído nos ciclos de 29/07/2026:

- wrapper do Gradle restaurado e build reproduzível com JDK 21;
- proteção contra perda do perfil durante login e edição;
- sessão restaurada ao abrir e logout disponível;
- IDs reais do Room usados nos envios ao Supabase;
- estados de sincronização atualizados após resposta do servidor;
- reenvio automático de pendências com WorkManager;
- migrations `2 -> 3`, `3 -> 4` e `4 -> 5`, com exportação dos schemas do Room;
- exclusões offline com tombstones para safras, tarefas e lançamentos;
- confirmação do usuário antes de excluir dados;
- cálculos financeiros centralizados e cobertos por testes unitários;
- remoção de produtor, propriedade, CAF e datas fictícias;
- remoção da afirmação automática de elegibilidade ao PRONAF.
- datas novas armazenadas em ISO e exibidas no formato brasileiro;
- seletores de data para safras, tarefas e lançamentos;
- validação para impedir colheita anterior ao início da safra.
- testes instrumentados de DAO e migrations executados em emulador;
- renovação automática do token Supabase antes do vencimento;
- bloqueio preventivo contra uma segunda conta herdar os dados locais da primeira.

Concluído no ciclo de 02/08/2026:

- sincronização bidirecional real de perfil, safras, tarefas e financeiro;
- UUID estável por registro, independente dos IDs locais do Room;
- isolamento local e remoto por usuário, com políticas RLS prontas para o Supabase;
- resolução de conflitos pela alteração mais recente e tombstones remotos para exclusões;
- download antes do envio, reprocessamento pelo WorkManager e sincronização manual no painel;
- credenciais do projeto retiradas do código-fonte e injetadas no build;
- migração Room `5 -> 6`, preservando dados e criando identidades de nuvem;
- tokens de sessão criptografados com AES-GCM e chave não exportável no Android Keystore;
- migração automática dos tokens legados do Room para o armazenamento protegido;
- regras de backup que impedem restauração de uma sessão sem sua chave do Keystore;
- testes unitários de conflito/data, testes instrumentados de DAO/migrations e teste real do Keystore;
- associação local e no esquema novo entre safras, tarefas e lançamentos financeiros;
- migração Room `6 -> 7`, preservando dados e adicionando as associações;
- compatibilidade automática com o esquema Supabase legado baseado em `user_email`;
- propagação de exclusões entre aparelhos também no esquema legado;
- modo local de teste quando um build não possui configuração de nuvem;
- teste real de ida e volta no Supabase entre dois bancos locais, incluindo exclusões.

Concluído no ciclo de edição de 02/08/2026:

- edição completa de safras, incluindo área, datas, progresso e situação do manejo;
- edição completa de tarefas, preservando status e associação com a safra;
- edição completa de receitas e despesas, incluindo tipo, categoria, data e safra;
- UUID, proprietário e associação preservados durante todas as edições;
- formulários roláveis e validados para uso em telas menores;
- atualização validada no Supabase real e baixada em um segundo banco local.

Concluído no ciclo de backup de 02/08/2026:

- exportação local em arquivo `.agrobackup` pelo seletor seguro do Android;
- conteúdo protegido por senha com AES-GCM e derivação de chave PBKDF2;
- formato versionado, com limites e validação contra arquivos corrompidos ou adulterados;
- exclusão de tokens de acesso e renovação do conteúdo exportado;
- restauração atômica somente na mesma conta, sem apagar registros ausentes do backup;
- preservação dos UUIDs e das associações entre safras, tarefas e lançamentos;
- registros restaurados reenviados automaticamente para a nuvem;
- testes de senha errada, adulteração, conta divergente, mesclagem local e ida e volta real no Supabase.

Concluído no ciclo de filtros e acessibilidade de 02/08/2026:

- filtros combináveis de tarefas por prazo, safra, categoria e situação;
- filtros combináveis do caixa por período, safra, categoria e tipo de movimentação;
- seleção explícita de registros sem safra associada;
- contagem de resultados e saldo recalculado somente sobre os lançamentos filtrados;
- categorias personalizadas descobertas automaticamente, sem duplicação por maiúsculas/minúsculas;
- períodos inclusivos e bloqueio visual de intervalo com data final anterior à inicial;
- controles com altura adaptável para fontes ampliadas e alvos de toque mínimos preservados;
- correção sistêmica do contraste de textos em botões, abas e superfícies;
- validação visual dos diálogos com fonte do Android em 130%;
- testes unitários das combinações de filtros e testes instrumentados dos estados reativos.

Concluído no ciclo de relatório PDF de 02/08/2026:

- período financeiro selecionável, inclusivo e validado antes da geração;
- resumo recalculado somente com as movimentações do período escolhido;
- identificação da origem dos dados e separação entre registros sincronizados e pendentes;
- indicador de completude com lista objetiva dos campos e registros ausentes;
- remoção de linguagem que apresentava o resumo como documento oficial;
- aviso explícito de que o relatório não substitui documentos nem garante aprovação;
- PDF A4 multipágina com cabeçalho, rodapé, movimentações detalhadas e compartilhamento seguro;
- compartilhamento validado no seletor real do Android com Drive, impressão, mensagens e Bluetooth;
- renderização visual de três páginas validada sem cortes e tela conferida com fonte em 130%;
- regras, gerador, URI de compartilhamento e fluxo reativo cobertos por testes automatizados;
- regressão de sincronização concluída novamente no Supabase real.

Concluído no ciclo de histórico e integridade de 02/08/2026:

- histórico persistente de PDFs separado pela conta ativa neste celular;
- migração Room `7 -> 8`, preservando todos os dados existentes;
- registro de período, data e hora, completude, resumo financeiro, nome e tamanho do arquivo;
- arquivo persistente com nome único, fora do cache temporário do Android;
- hash SHA-256 calculado no momento da geração e conferido antes de cada novo compartilhamento;
- bloqueio de arquivo ausente, modificado ou com caminho fora da área protegida do app;
- compartilhamento repetido pelo histórico somente após a verificação de integridade;
- exclusão confirmada que remove somente o PDF local e seu histórico;
- histórico local explicitamente excluído da sincronização do Supabase;
- validação visual com fonte em 130%, PDF A4 de três páginas e seletor real de compartilhamento;
- testes de migração, isolamento por conta, adulteração, caminho malicioso, geração e remoção;
- regressão de sincronização aprovada novamente no Supabase real.

Concluído no ciclo de consentimento de 02/08/2026:

- consentimento explícito e versionado antes de gerar ou compartilhar relatórios;
- finalidade e categorias de dados explicadas em linguagem simples antes da autorização;
- estado de consentimento separado por conta e preservado somente neste celular;
- revogação confirmada que bloqueia novos PDFs e compartilhamentos sem apagar dados ou arquivos;
- versão do formato, versão e data do consentimento registradas no PDF e no histórico;
- bloqueio seguro de PDFs antigos que não possuem prova de consentimento;
- migração Room `8 -> 9`, preservando o histórico anterior e identificando seu formato legado;
- PDF A4 de três páginas renderizado e conferido integralmente, com prova legível;
- telas de autorização, estado e revogação validadas com fonte do Android em 130%;
- testes de migração, isolamento, geração, integridade, revogação e nova autorização;
- regressão completa da sincronização aprovada novamente no Supabase real.

Concluído no ciclo de lembretes de 02/08/2026:

- lembretes locais para tarefas agrícolas próximas do vencimento ou atrasadas;
- permissão de notificações explicada pelo app somente quando o recurso é ativado;
- antecedência entre zero e sete dias e horário configurável pelo produtor;
- opção clara para pausar os alertas sem alterar ou excluir tarefas;
- reagendamento automático após edição, mudança de situação, exclusão, sincronização ou login;
- trabalhos persistentes do Android que sobrevivem à reinicialização do celular;
- nova conferência de conta, prazo, situação e versão da tarefa no momento de cada aviso;
- bloqueio de duplicatas e IDs distintos para tarefas com o mesmo título e prazo;
- preferências e avisos isolados por conta, sem depender de internet ou enviar dados;
- toque na notificação abrindo diretamente a área de tarefas;
- indicação quando a permissão foi desativada nas configurações do Android;
- interface e permissão validadas com fonte do Android em 130%;
- testes de cálculo, atraso, persistência, cancelamento, deduplicação, conclusão e troca de conta.

Correção de autenticação concluída na beta8 em 02/08/2026:

- endereço padrão `localhost:3000` removido da configuração de autenticação do Supabase;
- confirmação de e-mail retornando diretamente ao AgroGestão Pro por link Android;
- sessão recebida no link validada novamente no projeto antes de ser salva no celular;
- cadastro sem sessão imediata tratado como etapa normal, e não como erro;
- dados do produtor preservados localmente enquanto a confirmação está pendente;
- botão para reenviar o e-mail e mensagens de cadastro/login em português;
- teste fim a fim com confirmação, abertura automática do app e entrada no painel;
- nova regressão real da sincronização entre dois bancos locais aprovada.

Modo Simples concluído no ciclo de 04/08/2026:

- preferência global de interface persistida e isolada por proprietário local ou remoto;
- modo completo preservado sem remoção de recursos;
- barra simplificada com quatro destinos: Início, Tarefas, Talhões e Mais;
- nova tela Mais agrupando custos/relatórios, perfil, backup e troca do modo;
- custos e perfil continuam acessíveis e destacam corretamente o destino Mais;
- atalho de backup abre diretamente o fluxo protegido no painel;
- desligar o modo na tela Mais retorna com segurança ao Perfil;
- painel com linguagem mais direta quando o modo está ativo;
- 37 testes unitários e 32 instrumentados executados, sem falhas; lint com zero erros e APK gerado.

Recuperação de acesso e CI implementadas no ciclo de 04/08/2026:

- “Esqueci a senha” agora solicita o e-mail pelo endpoint de recuperação do Supabase;
- resposta não revela se um endereço possui conta cadastrada;
- deep link `type=recovery` abre uma tela exclusiva para criar a nova senha;
- token do link e compatibilidade com o perfil local são validados antes de alterar a senha;
- senha mínima de oito caracteres e confirmação centralizadas em regra de domínio;
- troca de senha para usuário autenticado disponível no Perfil;
- CI adicionada para JDK 21, testes unitários, lint, APK e varredura conservadora de segredos;
- 41 testes unitários e 32 instrumentados executados, sem falhas; dois testes live ignorados sem credenciais temporárias;
- pendência de saída: validar entrega/abertura do e-mail e troca real usando exclusivamente conta temporária.

Concluído no ciclo integrado de 04/08/2026:

- valores monetários migrados para centavos `Long`, incluindo schema, backup e soma exata;
- upgrade v1 assistido sem deleção silenciosa e migrations até a versão 11;
- clima opcional por município com consentimento, cache offline, fonte e revogação;
- CSV auditável do período financeiro selecionado;
- trilha consultável de conflitos de sincronização;
- configuração privada de assinatura, AAB de release, checklist e aviso de privacidade para piloto;
- painel “Hoje”, registro central e privacidade visual dos valores financeiros;
- 46 testes unitários e 36 instrumentados sem falhas; lint com zero erros; APK e AAB gerados.

Concluído no ciclo de rotina simples de 07/08/2026:

- Modo Simples recomendado desde o primeiro acesso, sem sobrescrever escolhas persistidas.
- Tela Hoje reorganizada para mostrar apenas a ação principal, a prioridade mais urgente e o resumo do dia.
- Fluxo “Atualizar meu dia” com perguntas curtas para nove acontecimentos rurais e data automática.
- Reaproveitamento do mesmo registro no histórico, caixa e terreno, conforme o tipo escolhido.
- Persistência local composta, tolerante à falta de internet, com confirmação e desfazer seguro.
- Linguagem principal adaptada para “Terrenos” e “Dinheiro”, mantendo o modo completo disponível.
- Fluxo visual percorrido no emulador com compra, atualização do resumo e desfazer.
- 51 testes unitários e 40 instrumentados sem falhas; dois testes live ignorados; lint sem erros.

Concluído no ciclo de redução de esforço de 08/08/2026:

- pergunta específica sobre o produto, conta, pagamento, origem ou problema em todos os nove tipos de registro rápido;
- sugestões selecionáveis e campo livre, com respostas recentes do próprio produtor priorizadas automaticamente;
- resumo final com atividade, resposta, valor, terreno e data antes do salvamento;
- continuação direta por “Registrar outra”, preservando também “Terminei” e o desfazer seguro;
- ajuda contextual em três passos na tela Mais;
- validação visual com fonte do Android em 130%, mantendo conteúdo refluído e controles acessíveis;
- 55 testes unitários e 40 instrumentados sem falhas; dois testes live ignorados; lint sem erros.

Gates externos pendentes antes de produção:

- validar recuperação/troca de senha no Supabase real com conta temporária;
- testar atualização usando um banco extraído de uma instalação v1 real;
- obter revisão jurídica, agronômica e de crédito;
- executar piloto consentido em aparelhos reais e conexão instável;
- assinar com a chave definitiva e concluir Play Console/LGPD do controlador.
