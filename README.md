# Criare 3D

Aplicação web para organizar os cadastros usados na impressão 3D e calcular o custo e o preço de venda de cada peça. Desenvolvida com Java, Spring Boot, Thymeleaf e PostgreSQL.

## Funcionalidades

- Autenticação com Spring Security, senhas com BCrypt e perfis ADMIN e USER.
- Cadastro, edição, pesquisa, consulta e exclusão de peças, filamentos, impressoras e embalagens.
- Importação de projetos 3MF e leitura opcional de tempo e consumo de arquivos fatiados do Bambu Studio (`slice_info.config`) e do OrcaSlicer (estatísticas JSON).
- Filamentos com cor, código hexadecimal, marca, peso do rolo e preço.
- Configuração da tarifa de energia, embalagem fixa e custos opcionais de mão de obra, depreciação da impressora e falhas/desperdício.
- Cálculo em etapas, com vários filamentos/cores, consumo por filamento, tempo de impressão, energia, embalagem, custos adicionais e markup.
- Simulação de venda na Shopee, TikTok Shop e Mercado Livre, incluindo tipo de anúncio no Mercado Livre.
- Histórico dos cálculos com valores registrados no momento da operação.
- Rascunho temporário para reduzir a perda de dados durante a navegação e cadastro rápido de itens auxiliares.

## Produção, vendas e planejamento

O fluxo separa estimativa, produção e venda. A produção registra sua própria data e baixa filamento; a venda trabalha com unidades prontas e pode registrar preço, desconto e taxas por unidade. Cancelamentos parciais preservam os registros e devolvem apenas as unidades canceladas ao estoque de peças.

Cadastros podem ser arquivados e restaurados. A simulação permite salvar planejamentos em uma fila por impressora, sem movimentar estoque. Perdas de tentativas extras podem ser registradas e corrigidas separadamente da estimativa de falha do cálculo.

Consulte também [segurança, backup, recuperação de acesso e arquitetura](docs/seguranca-operacao.md) para configurar os novos recursos de operação.

Consulte [a revisão e o roteiro de conferência](docs/revisao-operacao.md) para regras, migrations e cobertura de testes. Além dos testes Java, o CI executa os testes JavaScript e possui um job de navegador em 320 px e desktop.

## Tecnologias

Java 21 · Spring Boot 4.1.2 · Spring MVC/WebMVC · Spring Data JPA · Hibernate · Spring Security · Thymeleaf · PostgreSQL · Testcontainers · Maven · Bootstrap 5.3.3 · Bootstrap Icons · Hotwire Turbo · JavaScript · Docker · GitHub Actions.

## Estrutura

```text
src/
├── main/java/br/com/criare3d/
│   ├── config/       # Segurança, inicialização e filtros
│   ├── controller/   # Rotas e páginas
│   ├── dto/          # Objetos de entrada e saída
│   ├── models/       # Entidades e enums
│   ├── repository/   # Acesso ao banco
│   └── service/      # Regras de negócio
└── main/resources/
    ├── static/       # CSS, JavaScript e imagens
    ├── templates/    # Páginas Thymeleaf
    └── application.properties
```

## Variáveis de ambiente

A aplicação usa variáveis de ambiente para conexão e criação inicial do administrador. Não coloque credenciais reais no código-fonte nem as envie ao Git.

| Variável | Finalidade |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL. O padrão local é `jdbc:postgresql://localhost:5432/criare3d`. |
| `DB_USERNAME` | Usuário do PostgreSQL (padrão: `postgres`). |
| `DB_PASSWORD` | Senha do PostgreSQL (obrigatória). |
| `ADMIN_EMAIL` | E-mail do administrador inicial. |
| `ADMIN_PASSWORD` | Senha inicial do administrador, com pelo menos 12 caracteres. |
| `SESSION_COOKIE_SECURE` | Use `true` em produção com HTTPS; o padrão local é `false`. |
| `CRIARE_ML_ACCESS_TOKEN` | Token da aplicação do Mercado Livre usado para consultar anúncios novos e usados. Opcional; sem ele, a taxa de depreciação pode ser preenchida manualmente. Configure como segredo no Render, nunca no Git. |

O `AdminBootstrap` cria o ADMIN na primeira inicialização. Se já existir um ADMIN com o mesmo e-mail, a conta é mantida. Se o e-mail pertencer a um usuário comum, a inicialização é interrompida para evitar alteração indevida de perfil. O token do Mercado Livre precisa vir de uma aplicação de desenvolvedor e pode expirar; a renovação OAuth automática ainda não está implementada, então atualize o segredo do ambiente quando o token vencer.

## Executar localmente no Windows

1. Instale o JDK 21 e disponibilize um PostgreSQL.
2. Crie o banco `criare3d`.
3. Configure as variáveis no PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/criare3d"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="sua-senha-do-postgres"
$env:ADMIN_EMAIL="admin@exemplo.com"
$env:ADMIN_PASSWORD="uma-senha-com-12-caracteres-ou-mais"
```

4. Execute:

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação estará em `http://localhost:8080` (ou na porta definida por `PORT`). O Maven Wrapper baixa a versão configurada em `.mvn/wrapper/maven-wrapper.properties` na primeira execução, se necessário.

## Testes automatizados

Os testes de integração usam Testcontainers para iniciar um PostgreSQL descartável. É necessário ter Docker funcionando. O perfil de teste executa Flyway e valida o schema real; a suíte também verifica o isolamento entre contas nas seis tabelas principais e a atribuição do lucro à data de venda.

```powershell
.\mvnw.cmd clean verify
```

O GitHub Actions executa a suíte completa em ambiente com Docker. A construção da imagem Docker compila o projeto, mas não tenta iniciar containers aninhados; os testes ficam a cargo do CI.

## Integração contínua

O workflow `.github/workflows/ci.yml` roda em pushes e pull requests direcionados à branch `main`. Ele configura Java 21, usa cache do Maven e executa `mvn clean verify`. O workflow valida o projeto, mas não publica nem faz deploy automaticamente; o deploy continua sob controle da configuração do serviço no Render.

## Docker e Render

O `Dockerfile` possui duas etapas: a primeira compila o projeto com Maven/Java 21; a segunda copia somente o JAR para uma imagem JRE menor, executada com usuário sem privilégios. A aplicação respeita a variável `PORT` do Render e aceita opções pela variável `JAVA_OPTS`.

Configure no ambiente do serviço as variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` e `SESSION_COOKIE_SECURE=true`. Para buscar referências de preço no Mercado Livre, configure também `CRIARE_ML_ACCESS_TOKEN`.

Para construir a imagem:

```bash
docker build -t criare3d .
```

O schema é controlado pelo Flyway. Consulte [as migrations](src/main/resources/db/migration) para a estrutura e sua evolução; arquivos já aplicados são imutáveis. Bancos novos são criados pelas migrations, e o Hibernate confere o resultado com `ddl-auto=validate`. O baseline automático fica desligado; bancos legados sem histórico exigem incorporação explícita conforme a documentação operacional.

Faça backup do PostgreSQL local e do Neon antes de iniciar a aplicação com essa versão. O Flyway roda automaticamente na inicialização; confirme que `DB_URL` aponta para o banco esperado. Depois das migrações, o Hibernate usa `ddl-auto=validate` e apenas confere a estrutura. Os testes exercitam as migrations em um PostgreSQL descartável, sem substituir o backup do banco real.

## Checklist antes de publicar

- Executar `mvn clean verify`.
- Validar login e permissões dos perfis.
- Validar isolamento dos dados por usuário.
- Testar cadastros, cálculo, marketplace e histórico.
- Confirmar as variáveis de ambiente do Render e manter backup do PostgreSQL.
