# API v1 — etapa 5

A API convive com o site Thymeleaf. Nenhuma página foi removida. A primeira entrega cobre autenticação, tema da loja, Clientes (compradores) e Extrato. Os demais módulos recebem rotas durante a migração de suas telas.

## Configurar o login da API

1. Gere uma chave com `powershell -File scripts/gerar-chave-api.ps1`.
2. Copie o resultado para a variável de ambiente `API_JWT_SECRET` na configuração de execução do IntelliJ e, quando publicar, no Render. Use a mesma chave entre reinícios. Não coloque a chave em arquivos versionados.
3. Inicie o projeto normalmente. O Flyway aplica `V53__autenticacao_api.sql`, criando `api_sessao` e `api_refresh_token`.
4. Abra `http://localhost:8080/swagger-ui/index.html`.
5. Execute `POST /api/v1/auth/login` com `email` e `senha` de uma conta existente.
6. Copie `accessToken` e use **Authorize**. O campo recebe só o token, sem escrever `Bearer`.

Sem a variável, o site continua disponível. O login da API responde 503 com código `api_nao_configurada`. Uma chave inválida configurada impede a inicialização para evitar emissão com chave fraca.

## Autenticação e segurança

- Access token JWT HS256: 15 minutos, com emissor, público, expiração e assinatura verificados pelo Spring Security/Nimbus.
- Refresh token: 32 bytes aleatórios, guardado no banco somente como SHA-256. Expiração absoluta da sessão: 30 dias, sem prorrogação na renovação.
- `POST /auth/refresh` troca o refresh token. Substitua os **dois** tokens no cliente. Reutilizar um refresh já consumido revoga a família inteira; o app deve serializar renovações concorrentes e tentar somente uma vez.
- `POST /auth/logout`, com Bearer, revoga a sessão daquele dispositivo, incluindo seus access tokens. Outras sessões permanecem abertas.
- Uma linha de sessão é bloqueada na rotação; logout e rotação compartilham esse bloqueio para impedir reativar uma sessão encerrada.
- Cada chamada verifica a identidade atual, a conta ativa, o vínculo com tenant, a credencial e a sessão. Troca/redefinição de senha invalida tokens anteriores.
- Contas com troca obrigatória só podem consultar `/auth/me`, alterar `/auth/senha` e sair. Depois de alterar a senha, faça login novamente.
- ADMIN sem tenant pode consultar sua identidade, mas não dados de uma loja. `/api/v1/clientes` representa **compradores da loja**; não é o cadastro administrativo `/clientes` do site.
- A API v1 não usa cookie nem cria HttpSession. Cookie de login do site não autentica chamadas da API. Somente a API v1 dispensa CSRF; as rotas `/api/*` antigas do site mantêm sessão e CSRF.
- O limite de login por conta/IP é compartilhado com o site. Bloqueios retornam 429 e `Retry-After`.
- Swagger e OpenAPI são públicos e mostram apenas o contrato, sem dados privados. A autorização do Swagger não é persistida.
- Publicação exige HTTPS. CORS para a origem concreta do Expo Web será configurado junto ao projeto Expo; nenhuma origem curinga foi liberada.

## Rotas

Todas as rotas abaixo têm prefixo `/api/v1`.

| Método | Rota | Uso |
| --- | --- | --- |
| POST | `/auth/login` | `{ "email": "...", "senha": "..." }`, sem Bearer |
| POST | `/auth/refresh` | `{ "refreshToken": "..." }`, sem Bearer |
| GET | `/auth/me` | Identidade, tenant, perfil e troca obrigatória |
| POST | `/auth/logout` | Encerrar a sessão atual, 204 |
| POST | `/auth/senha` | `senhaAtual`, `novaSenha`, `confirmarNovaSenha`; 204 |
| GET | `/tenant` | Nome, cores, versão e URL autenticada da logo |
| GET | `/tenant/logo` | Imagem privada da loja |
| GET | `/clientes` | `busca`, `arquivados`, `pagina`, `tamanho` |
| GET | `/clientes/{id}` | Abrir comprador da conta atual |
| POST | `/clientes` | Criar: nome, telefone, email, observacoes; 201 + Location |
| PUT | `/clientes/{id}` | Editar; incluir a `versao` recebida ao consultar |
| PATCH | `/clientes/{id}/arquivo` | `{ "arquivado": true }` ou false; 204 |
| GET | `/extrato` | Filtros, movimentos, resumo e saldo de abertura |

Extrato aceita `inicio`/`fim` (YYYY-MM-DD), `tipo`, `direcao` (entrada/saida), `busca`, `filamento` (UUID), `saldo`, `tudo`, `pagina`, `tamanho`. Por padrão consulta os últimos 30 dias. `tudo=true` desliga o intervalo. A paginação começa em zero e aceita de 1 a 100 itens, com ordenação estável.

## Contrato

- Não são serializadas entidades JPA nem `Page` diretamente. Paginação: `{ "itens": [], "pagina": 0, "tamanho": 20, "total": 0 }`.
- Decimais são strings para preservar precisão. Exemplo: `"123456789.123456"`. O front deve usar os cálculos do servidor ao migrar o módulo de cálculo.
- Datas novas de autenticação são `timestamptz` no banco e ISO-8601 no JSON. Datas antigas sem fuso são interpretadas como America/Sao_Paulo na API (`criare.api.fuso-legado`); as colunas legadas não foram alteradas.
- Erros usam `application/problem+json`, com `type`, `title`, `status`, `detail` e `codigo`. Validações também devolvem `campos`.
- Principais respostas: 400 formato, 401 credencial/token, 403 permissão/troca obrigatória, 404 ausente, 409 duplicidade/concorrência, 422 validação, 429 limite e 503 indisponibilidade/configuração.
- A edição exige `versao`. Duas telas alterando o mesmo cliente geram 409; recarregue os dados antes de repetir.

O contrato gerado está em `/v3/api-docs`. O teste `ApiContratoTest` também exporta `target/api/openapi.json`. No projeto Expo, gere os tipos com `npx openapi-typescript http://localhost:8080/v3/api-docs -o src/api/schema.d.ts`; não mantenha DTOs TypeScript escritos manualmente em paralelo.

## Teste manual em ordem

Use o Swagger ou [api-v1.http](api-v1.http) no IntelliJ.

1. Confira login, navegação e um cálculo no site existente.
2. Sem token, chame `/api/v1/clientes`: deve retornar 401 JSON, sem redirecionar para login.
3. Faça login na API; confirme 900 segundos de validade e consulte `/auth/me` e `/tenant`.
4. Crie um cliente, liste, consulte pelo ID, edite com sua versão, arquive e restaure.
5. Reenvie a versão antiga: espere 409. Envie nome vazio: espere 422 com erro de campo.
6. Consulte Extrato com datas, tipo e paginação. Confira que quantidades são strings e datas incluem fuso.
7. Faça login em outra loja e tente abrir/editar/arquivar o ID criado na primeira: espere 404.
8. Renove o token e use o novo par. Reutilize o refresh antigo: espere 401; o novo access também deve parar de funcionar.
9. Faça outro login, execute logout e tente usar seu access: espere 401.
10. Confirme que trocar a senha ou suspender a conta bloqueia os tokens anteriores.

## Testes automatizados

`ApiContratoTest` sobe MVC, as duas cadeias de segurança e springdoc, com services de dados substituídos por mocks. Exercita tokens reais, contrato JSON, Swagger e coexistência com o site. `ApiAuthServiceTest` testa emissão, hashes, expiração, limites e rotação. A suíte geral preserva os testes do site.

`ConsolidacaoIntegrationTest` acrescenta rotação/reuso com transações reais e isolamento de clientes, ativada somente com `-Dcriare.integration=true` e PostgreSQL descartável configurado. Não aponte esse teste para banco de uso real.

## Revisão dos catches existentes

Os catches genéricos encontrados ficaram concentrados nas fronteiras de leitura de 3MF, consultas HTTP a provedores, envio assíncrono de e-mail, alerta operacional e cifragem. Não foram copiados para a API v1. Os controllers da API delegam regras aos services, e um advice específico converte falhas em JSON; respostas internas não incluem stack trace ou mensagens SQL. A revisão desses módulos legados continua junto de suas respectivas migrações.
