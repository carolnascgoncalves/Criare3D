# Criare 3D

Aplicação web para organizar os cadastros usados na impressão 3D e calcular o custo e o preço de venda de cada peça. Desenvolvida com Java, Spring Boot, Thymeleaf e PostgreSQL.

## Funcionalidades

- Autenticação com Spring Security, senhas com BCrypt e perfis ADMIN e USER.
- Cadastro, edição, pesquisa, consulta e exclusão de peças, filamentos, impressoras e embalagens.
- Filamentos com cor, código hexadecimal, marca, peso do rolo e preço.
- Configuração da tarifa de energia e da opção de custo fixo de embalagem.
- Cálculo em etapas, com vários filamentos/cores, consumo por filamento, tempo de impressão, energia, embalagem e markup.
- Simulação de venda na Shopee, TikTok Shop e Mercado Livre, incluindo tipo de anúncio no Mercado Livre.
- Histórico dos cálculos com valores registrados no momento da operação.
- Rascunho temporário para reduzir a perda de dados durante a navegação e cadastro rápido de itens auxiliares.

## Tecnologias

Java 21 · Spring Boot 4.1.2 · Spring MVC/WebMVC · Spring Data JPA · Hibernate · Spring Security · Thymeleaf · PostgreSQL · H2 (testes) · Maven · Bootstrap 5.3.3 · Bootstrap Icons · Hotwire Turbo · JavaScript · Docker · GitHub Actions.

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

Os testes usam o perfil `test` e um banco H2 em memória, sem acessar o PostgreSQL de desenvolvimento ou produção.

```powershell
.\mvnw.cmd clean verify
```

A suíte verifica a inicialização do contexto e o funcionamento básico do BCrypt (geração de hash e validação da senha original).

## Integração contínua

O workflow `.github/workflows/ci.yml` roda em pushes e pull requests direcionados à branch `main`. Ele configura Java 21, usa cache do Maven e executa `mvn clean verify`. O workflow valida o projeto, mas não publica nem faz deploy automaticamente; o deploy continua sob controle da configuração do serviço no Render.

## Docker e Render

O `Dockerfile` possui duas etapas: a primeira compila e executa os testes com Maven/Java 21; a segunda copia somente o JAR para uma imagem JRE menor, executada com usuário sem privilégios. A aplicação respeita a variável `PORT` do Render e aceita opções pela variável `JAVA_OPTS`.

Configure no ambiente do serviço as variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL` e `ADMIN_PASSWORD`.

Para construir a imagem:

```bash
docker build -t criare3d .
```

O projeto usa atualmente `spring.jpa.hibernate.ddl-auto=update`. Mantenha backup do banco antes de alterações estruturais importantes. Os testes com H2 não substituem a validação manual do fluxo completo com PostgreSQL.

## Checklist antes de publicar

- Executar `mvn clean verify`.
- Validar login e permissões dos perfis.
- Validar isolamento dos dados por usuário.
- Testar cadastros, cálculo, marketplace e histórico.
- Confirmar as variáveis de ambiente do Render e manter backup do PostgreSQL.
