# Plano das próximas alterações

**Ponto de partida:** AgroGestão Pro `1.1.0-beta8`<br>
**Atualizado em:** 02/08/2026

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
