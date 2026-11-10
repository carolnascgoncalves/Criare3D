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

O `AdminBootstrap` cria o ADMIN na primeira inicialização. Se já existir um ADMIN com o mesmo e-mail, a conta é mantida. Se o e-mail pertencer a um usuário comum, a inicialização é interrompida para evitar alteração indevida de perfil.

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

Configure no ambiente do serviço as variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` e `SESSION_COOKIE_SECURE=true`.

Para construir a imagem:

```bash
docker build -t criare3d .
```

O schema do banco é controlado pelo Flyway. A migração `V1` cria a estrutura para bancos novos. Nos bancos existentes, o Flyway registra a versão 1 como baseline e aplica a migração `V2`, que alinha a nulabilidade de `filamento`, cria índices por usuário ausentes e remove uma chave estrangeira duplicada conhecida. A migração `V3` adiciona as opções dos custos adicionais; a `V4` registra o tempo de trabalho nas peças e os dados de compra e vida útil das impressoras; a `V5` adiciona a opção de usar um tempo de mão de obra fixo nas configurações; a `V6` registra gastos de manutenção por impressora; a `V7` adiciona estoque e status de produção/venda; a `V8` separa no histórico o custo de manutenção do custo de depreciação; a `V9` registra a data de cadastro do filamento; a `V10` adiciona a versão usada para impedir perda de atualizações concorrentes no estoque; a `V11` registra quando uma conta precisa trocar a senha temporária definida pelo administrador; a `V12` registra a data real em que um cálculo foi marcado como vendido; a `V13` registra unidades por cálculo; a `V14` separa as fotos do histórico; a `V15` registra as impressões por cálculo; e a `V16` adiciona a capacidade de peças por impressão, o aquecimento da impressora, a purga por impressão, precisão maior para custos e estoque, totais de gramas no histórico e índices para consultas frequentes. A `V16` mantém os valores financeiros e o estoque dos cálculos antigos; os novos campos históricos recebem os valores correspondentes ou zero. A coluna legada `impressora.manutencao_hora_imp` é mantida no banco, mas deixa de ser usada como estimativa; a média passa a vir dos gastos registrados e das horas efetivamente produzidas.

Faça backup do PostgreSQL local e do Neon antes de iniciar a aplicação com essa versão. O Flyway roda automaticamente na inicialização; confirme que `DB_URL` aponta para o banco esperado. Depois das migrações, o Hibernate usa `ddl-auto=validate` e apenas confere a estrutura. Os testes exercitam as migrations em um PostgreSQL descartável, sem substituir o backup do banco real.

## Checklist antes de publicar

- Executar `mvn clean verify`.
- Validar login e permissões dos perfis.
- Validar isolamento dos dados por usuário.
- Testar cadastros, cálculo, marketplace e histórico.
- Confirmar as variáveis de ambiente do Render e manter backup do PostgreSQL.
