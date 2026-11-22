package br.com.criare3d.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.criare3d.api.auth.*;
import br.com.criare3d.api.clientes.ApiClientesController;
import br.com.criare3d.api.extrato.ApiExtratoController;
import br.com.criare3d.config.*;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.*;
import br.com.criare3d.security.*;
import br.com.criare3d.service.*;
import br.com.criare3d.service.interfaces.UsuarioService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@SpringBootTest(
        classes = ApiContratoTest.App.class,
        properties = {
            "criare.api.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
            "springdoc.paths-to-match=/api/v1/**",
            "springdoc.swagger-ui.persist-authorization=false"
        })
@AutoConfigureMockMvc
class ApiContratoTest {
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.test.context.TestComponent
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({
        ApiSecurityConfig.class,
        SecurityConfig.class,
        ApiOpenApiConfig.class,
        ApiJwtService.class,
        ApiDatas.class,
        ApiProblemas.class,
        ApiAuthController.class,
        ApiWebOrigins.class,
        ApiWebAuthController.class,
        ApiClientesController.class,
        ApiExtratoController.class,
        ApiTenantController.class,
        ApiRotaAusenteController.class
    })
    static class App {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        LimiteLogin limite(Clock clock) {
            return new LimiteLogin(clock);
        }

        @Bean
        UserDetailsService users() {
            return mock(UserDetailsService.class);
        }
    }

    @Autowired MockMvc mvc;
    @MockitoBean ApiContas contas;
    @MockitoBean ApiAuthService auth;
    @Autowired ApiJwtService jwt;
    @MockitoBean CompradorService clientes;
    @MockitoBean OrcamentoPedidoService pedidos;
    @MockitoBean MovimentacaoService movimentos;
    @MockitoBean UsuarioService usuarios;
    @MockitoBean TenantTemaCache temas;
    @MockitoBean SenhaObrigatoriaLoginSuccessHandler success;
    private UsuarioPrincipal usuario;
    private UUID familia;
    private String token;

    private UsuarioPrincipal usuario(UUID id, UUID tenantId, boolean troca) {
        Tenant t = null;
        if (tenantId != null) {
            t = new Tenant();
            t.setId(tenantId);
        }
        return new UsuarioPrincipal(
                Usuario.builder()
                        .id(id)
                        .email("teste@example.test")
                        .password("hash-atual")
                        .perfil(Perfil.USER)
                        .tenant(t)
                        .trocaSenhaObrigatoria(troca)
                        .build());
    }

    @BeforeEach
    void preparar() {
        reset(contas, auth, clientes, movimentos);
        usuario = usuario(UUID.randomUUID(), UUID.randomUUID(), false);
        familia = UUID.randomUUID();
        when(contas.porId(usuario.getUsuarioId())).thenReturn(Optional.of(usuario));
        when(auth.familiaAtiva(familia, usuario.getUsuarioId())).thenReturn(true);
        token = jwt.emitir(usuario, familia);
    }

    @Test
    void semTokenNaoRedirecionaParaHtmlNemAceitaSessaoDoSite() throws Exception {
        var sessao = new MockHttpSession();
        sessao.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(
                        UsernamePasswordAuthenticationToken.authenticated(
                                usuario, null, usuario.getAuthorities())));
        mvc.perform(get("/api/v1/clientes").session(sessao))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("autenticacao_necessaria"));
        verifyNoInteractions(clientes);
        mvc.perform(get("/compradores")).andExpect(status().is3xxRedirection());
    }

    @Test
    void tokenValidoCriaUsuarioPrincipalSemCriarSessaoHttp() throws Exception {
        var resposta =
                mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(usuario.getUsuarioId().toString()))
                        .andExpect(jsonPath("$.tenantId").value(usuario.getTenantId().toString()))
                        .andExpect(jsonPath("$.password").doesNotExist())
                        .andReturn();
        org.assertj.core.api.Assertions.assertThat(resposta.getRequest().getSession(false))
                .isNull();
    }

    @Test
    void assinaturaInvalidaExpiracaoRevogacaoEContaSuspensaBloqueiam() throws Exception {
        String[] partes = token.split("\\.");
        partes[2] = (partes[2].startsWith("A") ? "B" : "A") + partes[2].substring(1);
        mvc.perform(
                        get("/api/v1/auth/me")
                                .header("Authorization", "Bearer " + String.join(".", partes)))
                .andExpect(status().isUnauthorized());
        var antigo =
                new ApiJwtService(
                        "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
                        Clock.fixed(Instant.parse("2026-11-17T11:00:00Z"), ZoneOffset.UTC));
        mvc.perform(
                        get("/api/v1/auth/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + antigo.emitir(usuario, familia)))
                .andExpect(status().isUnauthorized());
        when(auth.familiaAtiva(familia, usuario.getUsuarioId())).thenReturn(false);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        when(contas.porId(usuario.getUsuarioId())).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantDoTokenPrecisaCoincidirComCadastroAtual() throws Exception {
        var outroTenant = usuario(usuario.getUsuarioId(), UUID.randomUUID(), false);
        String indevido = jwt.emitir(outroTenant, familia);
        mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + indevido))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(clientes);
    }

    @Test
    void trocaObrigatoriaESemLojaRetornamJsonSemAcessarDados() throws Exception {
        var troca = usuario(usuario.getUsuarioId(), usuario.getTenantId(), true);
        when(contas.porId(usuario.getUsuarioId())).thenReturn(Optional.of(troca));
        mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("troca_senha_obrigatoria"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // ADMIN sem tenant é válido para login administrativo, mas não para dados de uma loja.
        var admin =
                new UsuarioPrincipal(
                        Usuario.builder()
                                .id(usuario.getUsuarioId())
                                .email("admin@example.test")
                                .password("hash-atual")
                                .perfil(Perfil.ADMIN)
                                .build());
        when(contas.porId(usuario.getUsuarioId())).thenReturn(Optional.of(admin));
        mvc.perform(
                        get("/api/v1/clientes")
                                .header("Authorization", "Bearer " + jwt.emitir(admin, familia)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_sem_tenant"));
        verifyNoInteractions(clientes);
    }

    @Test
    void loginERefreshNaoExigemCsrfELogoutRevogaFamilia() throws Exception {
        when(auth.login(anyString(), anyString(), anyString()))
                .thenReturn(
                        new ApiAuthService.Tokens(
                                token,
                                "r".repeat(43),
                                "Bearer",
                                900,
                                OffsetDateTime.parse("2026-12-17T12:00:00Z")));
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"teste@example.test\",\"senha\":\"SenhaCorreta123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900));
        mvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + "r".repeat(43) + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(auth).revogar(familia, usuario.getUsuarioId());
    }

    @Test
    void validacaoEConflitoDeVersaoSaoProblemDetail() throws Exception {
        mvc.perform(
                        post("/api/v1/clientes")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"\",\"email\":\"invalido\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos.nome").exists());
        var id = UUID.randomUUID();
        when(clientes.salvar(any()))
                .thenThrow(
                        new br.com.criare3d.exception.RegraNegocioException(
                                "cliente_alterado",
                                "O cliente foi alterado em outra tela.",
                                org.springframework.http.HttpStatus.CONFLICT));
        mvc.perform(
                        put("/api/v1/clientes/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"Cliente\",\"versao\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("cliente_alterado"));
    }

    @Test
    void clienteNaoEncontradoNaoVazaEntidades() throws Exception {
        when(clientes.buscar(any())).thenThrow(new NoSuchElementException());
        mvc.perform(
                        get("/api/v1/clientes/" + UUID.randomUUID())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("nao_encontrado"));
        mvc.perform(get("/api/v1/rota-inexistente").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void listaOrcamentosDoCompradorNaPaginaESituacaoSolicitadas() throws Exception {
        var id = UUID.randomUUID();
        var pedido = new PedidoOrcamento();
        pedido.setNumero(12L);
        pedido.setTitulo("Lote azul");
        pedido.setFrete(new BigDecimal("4.75"));
        when(pedidos.listarDoComprador(id, "", 1, true, null))
                .thenReturn(new PageImpl<>(List.of(pedido), PageRequest.of(1, 12), 13));
        mvc.perform(get("/api/v1/clientes/{id}/orcamentos", id)
                        .param("arquivados", "true").param("pagina", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.total").value(13))
                .andExpect(jsonPath("$.itens[0].codigo").value("0012"))
                .andExpect(jsonPath("$.itens[0].titulo").value("Lote azul"))
                .andExpect(jsonPath("$.itens[0].situacao").value("Rascunho"))
                .andExpect(jsonPath("$.itens[0].total").value("4.75"));
        when(clientes.buscar(id)).thenThrow(new NoSuchElementException());
        mvc.perform(get("/api/v1/clientes/{id}/orcamentos", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        verify(pedidos).listarDoComprador(id, "", 1, true, null);
        verifyNoMoreInteractions(pedidos);
    }

    @Test
    void listaClientesUsaPaginaEstavel() throws Exception {
        var c = new Comprador();
        c.setId(UUID.randomUUID());
        c.setNome("Cliente");
        c.setVersao(0L);
        var contadores = mock(CompradorRepository.Contadores.class);
        when(clientes.listar(anyString(), anyBoolean(), any()))
                .thenReturn(
                        new CompradorService.Listagem(
                                new PageImpl<>(
                                        List.of(
                                                new CompradorService.ResumoCliente(
                                                        c,
                                                        1,
                                                        1,
                                                        LocalDateTime.parse(
                                                                "2026-11-17T08:00:00"))),
                                        PageRequest.of(0, 20),
                                        1),
                                contadores));
        mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes.total").value(1))
                .andExpect(jsonPath("$.clientes.itens[0].cliente.nome").value("Cliente"))
                .andExpect(
                        jsonPath("$.clientes.itens[0].ultimoOrcamento")
                                .value("2026-11-17T08:00:00-03:00"));
    }

    @Test
    void extratoPreservaDecimaisComoStringsEDatasComFuso() throws Exception {
        var m = new Movimentacao();
        m.setId(UUID.randomUUID());
        m.setTipo("COMPRA");
        m.setMedida("g");
        m.setDescricao("PLA Amarelo");
        m.setOrigem("filamento");
        m.setQuantidade(new BigDecimal("123456789.123456"));
        m.setSaldoApos(new BigDecimal("987654321.000001"));
        m.setData(LocalDateTime.parse("2026-11-17T08:30:00"));
        when(movimentos.extrato(any(), any()))
                .thenReturn(
                        new MovimentacaoService.Extrato(
                                new PageImpl<>(List.of(m)), List.of(), null, null));
        mvc.perform(get("/api/v1/extrato").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.movimentacoes.itens[0].quantidade").value("123456789.123456"))
                .andExpect(jsonPath("$.movimentacoes.itens[0].saldoApos").value("987654321.000001"))
                .andExpect(
                        jsonPath("$.movimentacoes.itens[0].data")
                                .value("2026-11-17T08:30:00-03:00"));
        mvc.perform(get("/api/v1/extrato?tamanho=1000").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void criarEArquivarClienteUsamServiceComBearerSemCsrf() throws Exception {
        UUID id = UUID.randomUUID();
        when(clientes.salvar(any()))
                .thenAnswer(
                        chamada -> {
                            br.com.criare3d.dto.SalvarComprador comando = chamada.getArgument(0);
                            var c = new Comprador();
                            c.setId(id);
                            c.setVersao(0L);
                            c.setNome(comando.nome());
                            return c;
                        });
        mvc.perform(
                        post("/api/v1/clientes")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"Novo comprador\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/clientes/" + id))
                .andExpect(jsonPath("$.nome").value("Novo comprador"));
        mvc.perform(
                        patch("/api/v1/clientes/" + id + "/arquivo")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"arquivado\":true}"))
                .andExpect(status().isNoContent());
        verify(clientes).arquivar(id, true);
        // O site conserva sua proteção CSRF.
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
    }

    @Test
    void limiteDeLoginRetornaRetryAfter() throws Exception {
        when(auth.login(anyString(), anyString(), anyString()))
                .thenThrow(new ApiAuthService.LoginLimitado(30));
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"teste@example.test\",\"senha\":\"SenhaCorreta123!\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.codigo").value("login_limitado"));
    }

    @Test
    void senhaRedefinidaInvalidaAccessAnterior() throws Exception {
        var t = new Tenant();
        t.setId(usuario.getTenantId());
        var redefinido =
                new UsuarioPrincipal(
                        Usuario.builder()
                                .id(usuario.getUsuarioId())
                                .email(usuario.getUsername())
                                .password("hash-novo")
                                .perfil(Perfil.USER)
                                .tenant(t)
                                .build());
        when(contas.porId(usuario.getUsuarioId())).thenReturn(Optional.of(redefinido));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocumentaSomenteV1ComTiposERotasEPermiteSwagger() throws Exception {
        var resposta =
                mvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                        .andExpect(jsonPath("$.paths['/api/v1/clientes']").exists())
                        .andExpect(jsonPath("$.paths['/api/v1/extrato']").exists())
                        .andExpect(jsonPath("$.paths['/catalogo-opcoes']").doesNotExist())
                        .andExpect(
                                jsonPath("$.components.securitySchemes.bearerAuth.scheme")
                                        .value("bearer"))
                        .andExpect(
                                jsonPath(
                                                "$.components.schemas.PaginaResponseClienteResumo.properties.itens.items['$ref']")
                                        .value("#/components/schemas/ClienteResumo"))
                        .andExpect(
                                jsonPath(
                                                "$.components.schemas.ClienteResumo.properties.cliente['$ref']")
                                        .value("#/components/schemas/Cliente"))
                        .andExpect(
                                jsonPath(
                                                "$.components.schemas.PaginaResponseMovimentacaoResponse.properties.itens.items['$ref']")
                                        .value("#/components/schemas/MovimentacaoResponse"))
                        .andExpect(
                                jsonPath(
                                                "$.components.schemas.MovimentacaoResponse.properties.quantidade.type")
                                        .value("string"))
                        .andReturn();
        Files.createDirectories(Path.of("target/api"));
        Files.writeString(
                Path.of("target/api/openapi.json"), resposta.getResponse().getContentAsString());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
