# Plano das próximas alterações

**Estado atual:** AgroGestão Pro `1.2.0-beta14`<br>
**Atualizado em:** 08/08/2026

## Ciclo implementado em 08/08/2026 — sincronização Supabase

- Backend migrado do esquema legado por e-mail para isolamento moderno por `user_id`.
- Os 11 perfis e a safra existentes foram copiados; as quatro tabelas legadas continuam intactas como backup recuperável.
- Migração corrigida para instalações legadas que já usavam UUID e guardavam datas como texto.
- Tarefas e lançamentos financeiros agora podem manter a associação com a safra entre aparelhos.
- O app renova a sessão uma vez quando um token é recusado antes do vencimento previsto.
- O selo da nuvem considera perfil, safras, tarefas e financeiro, sem informar “atualizada” enquanto houver item pendente.
- Logs de envio identificam a categoria que falhou sem registrar tokens ou conteúdo do produtor.
- Validação: 55 testes unitários e 40 instrumentados sem falhas; 2 testes live ignorados sem credenciais temporárias; lint com 0 erros; atualização beta13 → beta14 preservou exatamente o banco local.

APK gerado: `AgroGestaoPro-1.2.0-beta14-sync-nuvem.apk`.

## Ciclo implementado em 08/08/2026 — registro completo sem complicação

- Cada opção do registro diário agora pergunta claramente o que foi plantado, colhido, comprado, vendido, pago, recebido ou usado.
- Sugestões grandes permitem selecionar respostas comuns; o campo de texto continua disponível para qualquer outra opção.
- Compras, vendas, pagamentos e recebimentos separam a pergunta sobre o item da pergunta sobre o valor.
- Histórico, caixa e confirmação final exibem a resposta do produtor, evitando registros genéricos.
- Respostas recentes do próprio usuário aparecem primeiro nas próximas atualizações, reduzindo memória e digitação.
- Depois de salvar, o produtor pode escolher “Registrar outra”, “Terminei” ou “Desfazer último registro”.
- A tela Mais ganhou “Como usar”, com três instruções curtas e funcionamento offline explicado.
- Fluxos de colheita e compra foram percorridos no emulador; as telas novas também foram verificadas com fonte do Android em 130%.
- Validação: 55 testes unitários e 40 instrumentados, sem falhas; 2 testes live ignorados sem credenciais temporárias; lint sem erros.

O APK `1.2.0-beta13` foi gerado ao final dessa rodada.

## Ciclo implementado em 07/08/2026 — rotina diária simples

- Modo Simples recomendado por padrão para novos usuários, preservando a escolha de cada conta.
- Tela Hoje reduzida à prioridade mais urgente, resumo do dia e três destinos explicados em linguagem direta.
- Botão principal renomeado para “Atualizar meu dia”, com alvo ampliado e estimativa visível de cerca de 30 segundos.
- Registro guiado de plantio, colheita, compra, venda, pagamento, recebimento, uso de insumo, problema e outra atividade.
- Data automática e associação opcional a um terreno; cada pergunta fica em uma etapa separada.
- Um único registro alimenta o histórico de tarefas e, quando há valor, também o caixa; plantio e colheita atualizam o terreno escolhido.
- Gravação local composta antes da nuvem, evitando registro pela metade e repetição quando não há internet.
- Confirmação clara, opção de desfazer e preservação do que foi digitado quando ocorre erro.
- Navegação e linguagem do modo simples atualizadas de “Talhões/Lançamentos” para “Terrenos/Dinheiro”.
- Validação: 51 testes unitários e 40 instrumentados, sem falhas; 2 testes live ignorados sem credenciais temporárias; lint sem erros.

Esse ciclo foi visualmente aprovado antes da continuação autônoma solicitada pelo usuário.

## Ciclo concluído em 04/08/2026 — Modo Simples

- Preferência persistente e isolada por conta/proprietário.
- Navegação reduzida para Início, Tarefas, Talhões e Mais.
- Tela Mais com acesso a custos, relatórios, perfil e backup.
- Alternância disponível no Perfil e em Mais, sem remover o modo completo.
- Correção do retorno ao Perfil ao desativar o modo e abertura direta do backup.
- Validação: 37 testes unitários e 32 instrumentados sem falhas, lint sem erros e APK debug gerado.

A ordem dos próximos ciclos segue a documentação técnica v3: recuperação de acesso e estabilização vêm antes do clima.

## Ciclo implementado em 04/08/2026 — recuperação de acesso e CI

- Solicitação de recuperação de senha pelo e-mail da conta.
- Deep link de recuperação separado da confirmação de cadastro.
- Validação do token e da conta local antes da alteração.
- Nova senha com oito caracteres, confirmação e entrada automática segura.
- Troca de senha disponível para usuário conectado.
- Mensagens para link expirado, limite de envio, modo local e sessão indisponível.
- Pipeline GitHub Actions com JDK 21, testes, lint, APK e checagem de segredos.
- Validação local: 41 testes unitários e 32 instrumentados sem falhas; dois testes live ignorados sem credenciais temporárias.

Pendente para considerar a beta9 pronta: executar entrega, abertura e expiração do e-mail de recuperação em aparelho real usando somente conta temporária protegida.

## Como cada ciclo será conduzido

Cada nova versão seguirá esta ordem:

1. Planejar uma integração ou melhoria pequena e bem definida.
2. Implementar sem remover recursos que já funcionam.
3. Testar atualização, uso sem internet, sincronização e segurança.
4. Corrigir os bugs encontrados.
5. Gerar um APK identificado com versão, data e hash para teste.

## Prioridade 1 — `1.1.0-beta9`: conta e recuperação de acesso

Objetivo: impedir que problemas de e-mail, senha ou recriação da conta deixem o usuário sem acesso aos dados.

Alterações planejadas:

- Adicionar “Esqueci minha senha” com envio pelo SMTP configurado no Supabase/Brevo.
- Abrir o link de recuperação diretamente no aplicativo e permitir a criação de uma nova senha.
- Adicionar troca de senha para quem já está conectado.
- Melhorar as mensagens de erro de cadastro, confirmação, login e limite de e-mails.
- Mostrar com clareza quando o e-mail ainda precisa ser confirmado e permitir novo envio.
- Preservar e reassociar com segurança os dados locais quando a mesma pessoa precisar recriar a conta.
- Criar proteção de teste para que rotinas automáticas só possam excluir contas temporárias identificadas explicitamente como teste.
- Registrar falhas de autenticação sem guardar senha, chave secreta ou conteúdo pessoal.

Critérios para considerar a beta9 pronta:

- Cadastro, confirmação, login, troca e recuperação de senha funcionam em um aparelho real.
- Os e-mails aparecem como entregues no Brevo e abrem o aplicativo corretamente.
- Atualizar da beta8 para a beta9 não apaga nem duplica dados locais.
- Uma falha de internet ou de envio de e-mail apresenta orientação clara e permite tentar novamente.
- Nenhuma conta real pode ser removida por testes automatizados.

## Prioridade 2 — `1.1.0-beta10`: clima por município

Objetivo: oferecer previsão simples e útil sem tornar o funcionamento principal dependente da internet.

Alterações planejadas:

- Pedir consentimento antes de consultar externamente a localização informada pelo usuário.
- Buscar previsão pelo município, sem exigir localização precisa do aparelho.
- Exibir fonte, horário da atualização e limitações da previsão.
- Manter a última previsão em cache para consulta sem internet.
- Disponibilizar atualização manual e estados claros de carregamento ou indisponibilidade.
- Criar alertas informativos simples para chuva forte, calor e vento.
- Manter os alertas separados de recomendações técnicas, financeiras ou de crédito.

Critérios para considerar a beta10 pronta:

- A função é opcional e o restante do aplicativo continua funcionando quando ela está desligada.
- A previsão em cache permanece visível no modo avião, identificada como desatualizada.
- Município inexistente, serviço indisponível e resposta incompleta não causam travamento.
- Nenhuma localização é enviada antes do consentimento.

## Prioridade 3 — `1.1.0-beta11`: estabilidade e desempenho

Objetivo: preparar o aplicativo para bases de dados maiores e para uso diário prolongado.

Alterações planejadas:

- Medir e reduzir o tempo de abertura das telas principais.
- Revisar consultas e índices do banco local para históricos maiores.
- Evitar sincronizações repetidas e consumo desnecessário de bateria.
- Melhorar o andamento visível de backup, restauração, PDF e sincronização.
- Revisar acessibilidade: tamanho de texto, contraste, foco, descrições e área de toque.
- Adicionar testes de regressão para edição, exclusão, conflitos e restauração de backup.

Critérios para considerar a beta11 pronta:

- Listas grandes continuam responsivas em aparelho intermediário.
- Trabalho em segundo plano não entra em ciclo de tentativas contínuas.
- Os testes de acessibilidade e as rotinas principais passam sem regressões.

## Prioridade 4 — `1.1.0-beta12`: exportação e auditoria

Objetivo: facilitar o uso dos dados fora do aplicativo e aumentar a confiança nos relatórios.

Alterações planejadas:

- Exportar registros selecionados em CSV além dos relatórios PDF.
- Permitir escolher período, propriedade e tipo de registro antes da exportação.
- Incluir identificação da versão do aplicativo e data de geração nos arquivos.
- Melhorar o histórico de relatórios e a verificação de integridade.
- Revisar textos de consentimento, privacidade e limitações dos relatórios.

## Ciclos beta10–beta12 implementados em 04/08/2026

- valores financeiros persistidos e somados em centavos inteiros, com backup v2 compatível com backup v1;
- migration `9 -> 10` e teste de arredondamento/atualização;
- migration assistida `1 -> 2`, sem fallback destrutivo silencioso;
- previsão Open-Meteo por município somente após consentimento, sem GPS, com cache offline, fonte, horário, revogação e alertas informativos;
- CSV financeiro do período selecionado com versão, geração, propriedade, centavos, estado de sincronização e IDs;
- trilha local e isolada por conta para conflitos de sincronização, migration `10 -> 11` e tela de consulta;
- assinatura de release configurável por segredo local/ambiente, AAB reproduzível e checklist de publicação;
- aviso de privacidade operacional para piloto e gates externos explicitados;
- painel “Hoje”, atalho central de registro e ocultação persistente dos valores financeiros por conta;
- validação: 46 testes unitários e 36 instrumentados sem falhas, dois testes live ignorados; lint com zero erros; APK e AAB gerados.

Os critérios que dependem de terceiros permanecem gates de release: e-mail real de recuperação, banco v1 real, revisão jurídica/agronômica, piloto com produtores e publicação/assinatura com a chave do responsável.

## Verificação obrigatória antes de cada APK

- Executar testes automatizados, análise de qualidade e compilação de produção.
- Testar instalação limpa e atualização a partir do APK anterior.
- Confirmar funcionamento com internet, sem internet e após reconexão.
- Verificar sincronização entre dois acessos da mesma conta sem duplicações.
- Usar somente contas temporárias claramente marcadas nos testes do Supabase.
- Procurar chaves, senhas e tokens no código e no APK antes da entrega.
- Confirmar versão, assinatura, tamanho e hash SHA-256 do arquivo final.
- Registrar bugs encontrados, correções realizadas e limitações conhecidas.

## Regras de segurança

- Chaves administrativas, SMTP e tokens pessoais nunca serão incluídos no código ou no APK.
- O arquivo local com credenciais não será versionado nem distribuído.
- Alterações destrutivas no banco remoto exigirão backup e conferência exata do alvo.
- Contas reais nunca serão usadas em rotinas de exclusão ou limpeza de testes.
- Credenciais temporárias deverão ser revogadas ou trocadas depois do uso.

## Fora do próximo ciclo

Ficam para uma etapa posterior, após estabilizar conta, clima e desempenho:

- pagamentos e assinaturas;
- trabalho compartilhado entre vários membros de uma propriedade;
- integração com máquinas ou sensores;
- recomendações agronômicas automáticas;
- análise ou oferta de crédito.

## Próxima ação

O próximo trabalho planejado é iniciar a `1.1.0-beta9` pela recuperação de senha e pelo link de retorno ao aplicativo, depois validar todo o ciclo de autenticação usando exclusivamente uma conta temporária protegida.
