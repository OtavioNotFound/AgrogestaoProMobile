# Checklist de release e piloto

## Automático a cada versão

- `testDebugUnitTest`, testes instrumentados e lint sem erros;
- `assembleDebug` e `bundleRelease` reproduzíveis com JDK 21;
- schemas Room exportados e todas as migrations testadas;
- varredura de segredos;
- hash SHA-256 do APK/AAB e registro do tamanho;
- instalação limpa e atualização a partir da última versão de piloto.

## Assinatura

Use `keystore.properties` local baseado em `keystore.properties.example` ou as variáveis `AGRO_RELEASE_STORE_FILE`, `AGRO_RELEASE_STORE_PASSWORD`, `AGRO_RELEASE_KEY_ALIAS` e `AGRO_RELEASE_KEY_PASSWORD`. O keystore e as senhas nunca entram no Git. Guarde cópia offline da chave de upload e documente quem tem acesso.

## Gates externos — não podem ser simulados por teste automatizado

- entrega, abertura, uso e expiração do e-mail de recuperação em aparelho real e conta temporária;
- revisão jurídica do aviso de privacidade e processo de atendimento LGPD;
- revisão agronômica e de crédito dos textos do relatório;
- piloto consentido com produtores reais, aparelho de entrada e conexão instável;
- validação da licença/plano do provedor de clima para o modelo de distribuição escolhido;
- preenchimento da ficha de segurança de dados e política pública na loja;
- assinatura final e envio manual à Play Console.

## Roteiro mínimo do piloto

1. Atualizar uma instalação com dados da versão anterior e conferir valores/saldos.
2. Criar, editar e excluir talhão, tarefa e lançamento offline; reconectar e conferir em dois aparelhos.
3. Produzir/restaurar backup e validar senha incorreta/arquivo alterado.
4. Autorizar e revogar relatório; exportar PDF e CSV.
5. Autorizar clima, atualizar, entrar em modo avião e conferir cache identificado como desatualizado.
6. Usar fonte ampliada, tela pequena e leitor de tela.
7. Registrar somente métricas agregadas e voluntárias; nunca coletar conteúdo financeiro sem necessidade e consentimento.
