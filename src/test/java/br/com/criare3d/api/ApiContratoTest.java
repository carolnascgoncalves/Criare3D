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
        ApiCatalogoEdicaoController.class, ApiPainelController.class, ApiLotesController.class, ApiCustosLojaController.class,
        ApiConfiguracaoController.class, ApiCalculosController.class,
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
    @MockitoBean br.com.criare3d.service.interfaces.ImpressoraService impressoras;
    @MockitoBean br.com.criare3d.service.interfaces.EmbalagemService embalagens;
    @MockitoBean CustoLojaRepository custosLoja;
    @MockitoBean RateioCustoHistoricoRepository rateios;
    @MockitoBean CatalogoModelosImpressora modelosImpressora;
    @MockitoBean MercadoLivreDepreciacaoService referenciasDepreciacao;
    @MockitoBean EstimativaUsoImpressoraService estimativasUso;
    @MockitoBean ConsultaLoteService consultaLote;
    @MockitoBean ProducaoService producao;
    @MockitoBean VendaService venda;
    @MockitoBean CustoPersonalizadoService custosLote;
    @MockitoBean FalhaProducaoService falhasLote;
    @MockitoBean IdempotenciaService operacoes;
    @MockitoBean ItemPedidoRepository reservas;
    @MockitoBean TarifaEnergiaReferenciaService tarifas;
    @MockitoBean DashboardDadosService dashboard;
    @MockitoBean CompradorRepository compradores;
    @MockitoBean Modelo3mfParser parser;
    @MockitoBean FotoCalculoService fotos;
    @MockitoBean CalculoService calculos;
    @MockitoBean CatalogoBuscaService catalogo;
    @MockitoBean br.com.criare3d.service.interfaces.PecaService pecas;
    @MockitoBean br.com.criare3d.service.interfaces.ManutencaoImpressoraService manutencao;
    @MockitoBean br.com.criare3d.service.PecaArquivoService arquivos;
    @MockitoBean HistoricoCalculoService historico;
    @MockitoBean IndicadoresService indicadores;
    @MockitoBean br.com.criare3d.service.interfaces.ConfiguracaoService configuracoes;
    @MockitoBean br.com.criare3d.service.interfaces.FilamentoService filamentos;
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
    void dashboardIncluiPrejuizoEVendasReaisDoPeriodo() throws Exception {
        when(dashboard.buscar("7")).thenReturn(Map.ofEntries(
                Map.entry("totalPecas", 3L), Map.entry("totalImpressoras", 1L), Map.entry("totalFilamentos", 2L),
                Map.entry("totalEmbalagens", 1L), Map.entry("composicaoCustos", List.of(Map.of("nome", "Energia",
                        "valor", new BigDecimal("1.20"), "percentual", 20))),
                Map.entry("rotulosLucro", List.of("22/11")), Map.entry("valoresLucro", List.of(new BigDecimal("-2.50"))),
                Map.entry("lucrosPorPlataforma", List.of(Map.of("nome", "Shopee", "vendas", 2L, "lucro", new BigDecimal("-2.50"), "cor", "--dashboard-shopee"))),
                Map.entry("vendasCategorias", List.of(Map.of("nome", "Decoração", "unidades", 2L))),
                Map.entry("lucroPlataformas", new BigDecimal("-2.50")),
                Map.entry("estoqueFilamentos", List.of(Map.of("nome", "Preto", "gramas", BigDecimal.TEN, "restante", 1, "cor", "#000000"))),
                Map.entry("lucroVendido", new BigDecimal("-2.50")), Map.entry("totalVendasLoja", 2L),
                Map.entry("horasProduzidas", BigDecimal.ONE), Map.entry("filamentoUsadoKg", BigDecimal.ONE),
                Map.entry("fluxoCalculados", 3L), Map.entry("fluxoProduzidos", 2L), Map.entry("fluxoVendidos", 1L)));
        mvc.perform(get("/api/v1/dashboard").param("periodo", "7").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serie[0].valor").value("-2.50"))
                .andExpect(jsonPath("$.plataformas[0].vendas").value(2))
                .andExpect(jsonPath("$.plataformas[0].lucro").value("-2.50"))
                .andExpect(jsonPath("$.lucroPlataformas").value("-2.50"))
                .andExpect(jsonPath("$.categorias[0].unidades").value(2))
                .andExpect(jsonPath("$.estoque[0].cor").value("#000000"))
                .andExpect(jsonPath("$.estoque[0].gramas").value("10"))
                .andExpect(jsonPath("$.cadastros.pecas").value(3))
                .andExpect(jsonPath("$.custos[0].valor").value("1.20"));
    }

    @Test
    void catalogoUsaPrecoDoHistoricoEExigeAutenticacao() throws Exception {
        var id = UUID.randomUUID();
        var peca = Peca.builder().id(id).nome("Vaso").quantidadeFilamentoGramas(new BigDecimal("12.40")).build();
        when(catalogo.buscarPecas(eq(""), any())).thenReturn(new PageImpl<>(List.of(peca)));
        when(historico.ultimosPrecosPecas(List.of(id))).thenReturn(Map.of(id, new BigDecimal("14.95")));
        var resumo = new br.com.criare3d.dto.PecaArquivoResumo(id, "vaso.3mf", "P1S", null, 1,
                List.of(new br.com.criare3d.dto.PecaMaterialResumo("PLA", "#ff0000", 12.4),
                        new br.com.criare3d.dto.PecaMaterialResumo("PLA", "#ffffff", 0)),
                List.of(new br.com.criare3d.dto.PecaArquivoResumo.PlacaResumo(1, "/placa", null, null, List.of())));
        when(arquivos.buscarResumos(List.of(peca))).thenReturn(Map.of(id, resumo));
        mvc.perform(get("/api/v1/catalogo/pecas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].nome").value("Vaso"))
                .andExpect(jsonPath("$.itens[0].materiais.length()").value(1))
                .andExpect(jsonPath("$.itens[0].materiais[0].corHex").value("#ff0000"))
                .andExpect(jsonPath("$.itens[0].placaMiniatura").value(1))
                .andExpect(jsonPath("$.itens[0].gramas").value("12.40"))
                .andExpect(jsonPath("$.itens[0].preco").value("14.95"));
        mvc.perform(get("/api/v1/catalogo/pecas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/vendas").param("grupo", "invalido").header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void fotoDaPlacaNoCatalogoUsaSessaoETrataAusencia() throws Exception {
        var id = UUID.randomUUID();
        var rota = "/api/v1/catalogo/pecas/" + id + "/placas/";
        when(arquivos.buscarImagem(id, 2)).thenReturn(Optional.of(
                new br.com.criare3d.service.PecaArquivoService.ImagemPeca("image/png", new byte[]{1, 2, 3})));
        mvc.perform(get(rota + "2/foto")).andExpect(status().isUnauthorized());
        mvc.perform(get(rota + "2/foto").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("image/png"))
                .andExpect(jsonPath("$.base64").value("AQID"));
        mvc.perform(get(rota + "1/foto").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalheDistinguePecaSemArquivoDePecaAusente() throws Exception {
        var id = UUID.randomUUID();
        var rota = "/api/v1/catalogo/pecas/" + id + "/arquivo";
        mvc.perform(get(rota).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
        var peca = Peca.builder().id(id).build();
        when(pecas.fetchById(id)).thenReturn(Optional.of(peca));
        mvc.perform(get(rota).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        var placa = new br.com.criare3d.dto.PecaArquivoResumo.PlacaResumo(2, "/placa", 1800d, 10d,
                List.of(new br.com.criare3d.dto.PecaMaterialResumo("PLA", "#ff0000", 10d),
                        new br.com.criare3d.dto.PecaMaterialResumo("PLA", "#ffffff", 0d)));
        when(arquivos.buscarResumo(peca)).thenReturn(Optional.of(new br.com.criare3d.dto.PecaArquivoResumo(
                id, "modelo.3mf", "P1S", "BambuStudio", 1, List.of(), List.of(placa))));
        mvc.perform(get(rota).header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("modelo.3mf"))
                .andExpect(jsonPath("$.placas[0].numero").value(2))
                .andExpect(jsonPath("$.placas[0].temImagem").value(true))
                .andExpect(jsonPath("$.placas[0].materiais.length()").value(1));
    }

    @Test
    void detalheDaImpressoraExibeManutencaoDoResumoExistente() throws Exception {
        var id = UUID.randomUUID();
        var impressora = new br.com.criare3d.models.Impressora();
        impressora.setVidaUtilHoras(new BigDecimal("8000"));
        impressora.setDepreciacaoPorHora(new BigDecimal("0.1250"));
        when(manutencao.resumo(id)).thenReturn(Optional.of(new br.com.criare3d.dto.ResumoManutencaoImpressora(
                impressora, List.of(), new BigDecimal("40.00"), 122, new BigDecimal("0.005"))));
        mvc.perform(get("/api/v1/catalogo/impressoras/" + id + "/custos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutosImpressos").value(122))
                .andExpect(jsonPath("$.gastoTotal").value("40.00"))
                .andExpect(jsonPath("$.manutencaoHora").value("0.005"))
                .andExpect(jsonPath("$.depreciacaoHora").value("0.1250"));
    }

    @Test
    void inicioMostraPrecoCustoESituacaoDaMaoDeObra() throws Exception {
        var config = new br.com.criare3d.models.Configuracao();
        when(configuracoes.buscarOuCriarConfiguracao()).thenReturn(config);
        when(historico.fetchRecentes(3)).thenReturn(List.of(new br.com.criare3d.dto.CalculoRecenteResponse(
                UUID.randomUUID(), "Calendário", new BigDecimal("67.31"), new BigDecimal("77.41"), null, true, true, 2)));
        mvc.perform(get("/api/v1/inicio").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorHoraConfigurado").value(false))
                .andExpect(jsonPath("$.recentes[0].custoUnitario").value("67.31"))
                .andExpect(jsonPath("$.recentes[0].precoSugerido").value("77.41"))
                .andExpect(jsonPath("$.recentes[0].situacao").value("Vendido"));
        config.setMaoObraAtivada(true);
        config.setValorMaoObraHora(BigDecimal.TEN);
        mvc.perform(get("/api/v1/inicio").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.valorHoraConfigurado").value(true));
    }

    @Test
    void pesquisaFilamentosComCorEPesoDoRolo() throws Exception {
        var pla = new br.com.criare3d.models.Filamento();
        pla.setNome("PLA");
        pla.setCodigoCor("#ff0000");
        pla.setPesoRoloGramas(new BigDecimal("1000"));
        pla.setEstoqueGramas(new BigDecimal("82.5"));
        when(catalogo.buscarFilamentos(eq("vermelho"), any())).thenReturn(new PageImpl<>(List.of(pla)));
        mvc.perform(get("/api/v1/catalogo/filamentos").param("busca", " vermelho ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].codigoCor").value("#ff0000"))
                .andExpect(jsonPath("$.itens[0].estoque").value("82.5"))
                .andExpect(jsonPath("$.itens[0].pesoRolo").value("1000"));
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
        m.setReferencia(UUID.randomUUID());
        when(movimentos.coresDosFilamentos(List.of(m))).thenReturn(Map.of(m.getReferencia(), "#f5da30"));
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
                .andExpect(jsonPath("$.movimentacoes.itens[0].codigoCor").value("#f5da30"))
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
    void compraNoCatalogoUsaChaveEQuantidadeDeRolos() throws Exception {
        var id = UUID.randomUUID();
        var chave = UUID.randomUUID();
        when(operacoes.executar(eq(chave), eq("COMPRA_FILAMENTO"), anyString(), any()))
                .thenAnswer(i -> i.<java.util.function.Supplier<String>>getArgument(3).get());
        when(filamentos.registrarCompra(eq(id), any())).thenReturn(true);
        mvc.perform(post("/api/v1/catalogo/filamentos/" + id + "/compras")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"chave":"%s","compra":{"unidadesCompra":2,"pesoRoloGramas":"750","precoRolo":"80.50"}}
                                """.formatted(chave)))
                .andExpect(status().isNoContent());
        verify(filamentos).registrarCompra(eq(id), argThat(c -> c.getQuantidadeTotalGramas().intValueExact() == 1500));
    }

    @Test
    void edicaoDeEmbalagemCarregaEAtualizaCusto() throws Exception {
        var id = UUID.randomUUID();
        var caixa = br.com.criare3d.models.Embalagem.builder().nome("Caixa").custoUnitario(BigDecimal.ONE).build();
        when(embalagens.fetchById(id)).thenReturn(Optional.of(caixa));
        when(embalagens.update(eq(id), any())).thenReturn(Optional.of(caixa));
        var rota = "/api/v1/catalogo/embalagens/" + id;
        mvc.perform(get(rota + "/edicao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Caixa"));
        mvc.perform(put(rota).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Caixa nova","custoUnitario":"2.50"}
                                """))
                .andExpect(status().isNoContent());
        verify(embalagens).update(eq(id), argThat(e -> new BigDecimal("2.50").compareTo(e.getCustoUnitario()) == 0));
    }

    @Test
    void configuracaoUsaValoresDaLojaEValidaPercentualDeFalhas() throws Exception {
        var config = br.com.criare3d.models.Configuracao.builder().tarifaEnergiaKwh(new BigDecimal("0.79"))
                .embalagemFixa(false).build();
        when(configuracoes.buscarOuCriarConfiguracao()).thenReturn(config);
        mvc.perform(get("/api/v1/configuracao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tarifaEnergiaKwh").value(0.79));
        var entrada = """
                {"markupOpcao1":15,"markupOpcao2":40,"markupOpcao3":50,"tarifaEnergiaKwh":0.85,
                 "embalagemFixa":false,"taxaFalhaAtivada":true,"percentualFalha":%d}
                """;
        mvc.perform(put("/api/v1/configuracao").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada.formatted(101)))
                .andExpect(status().isUnprocessableEntity());
        when(configuracoes.update(any(), any())).thenAnswer(i -> Optional.of(i.getArgument(1)));
        mvc.perform(put("/api/v1/configuracao").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada.formatted(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.percentualFalha").value(5))
                .andExpect(jsonPath("$.markupOpcao1").value(15));
    }

    @Test
    void referenciaDeEnergiaECidadeSaoConsultadasSemAlterarConfiguracao() throws Exception {
        when(tarifas.listar()).thenReturn(List.of(new br.com.criare3d.dto.TarifaEnergiaReferenciaResponse(
                "Distribuidora teste", new BigDecimal("0.798"), null, null, "MWh")));
        mvc.perform(get("/api/v1/configuracao/tarifas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].valorKwh").value(0.798));
        when(tarifas.localizarCidade(-23.5, -46.6)).thenReturn(Map.of("cidade", "São Paulo", "uf", "SP"));
        mvc.perform(post("/api/v1/configuracao/cidade").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"latitude":-23.5,"longitude":-46.6}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.uf").value("SP"));
        verify(configuracoes, never()).update(any(), any());
    }

    @Test
    void previaConsideraDoisFilamentosETaxasDoCanal() throws Exception {
        var id = UUID.randomUUID();
        var resultado = mock(br.com.criare3d.dto.calculo.ResultadoCalculo.class);
        when(resultado.taxasPlataforma()).thenReturn(new BigDecimal("3.25"));
        when(resultado.itensFilamento()).thenReturn(List.of(
                new br.com.criare3d.dto.calculo.ResultadoCalculo.Item(null, "PLA", "Vermelho", "#FF0000",
                        null, null, new BigDecimal("2.5"), BigDecimal.ZERO, new BigDecimal("1.26"), id),
                new br.com.criare3d.dto.calculo.ResultadoCalculo.Item(null, "PETG", "Laranja", "#FF6600",
                        null, null, new BigDecimal("7"), BigDecimal.ZERO, new BigDecimal("0.50"), null)));
        when(calculos.prever(any())).thenReturn(resultado);
        var entrada = """
                {"pecaId":"%1$s","impressoraId":"%1$s","embalagemId":"%1$s","filamentos":[
                 {"filamentoId":"%1$s","quantidadeFilamentoGramas":2.5},{"filamentoId":"%2$s","quantidadeFilamentoGramas":7}],
                 "minutos":10,"unidades":1,"venda":{"plataformaVenda":"TIKTOK","freteGratisTikTok":true}}
                """.formatted(id, UUID.randomUUID());
        mvc.perform(post("/api/v1/calculos/previa").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada))
                .andExpect(status().isOk()) .andExpect(jsonPath("$.custos.taxasPlataforma").value(3.25))
                .andExpect(jsonPath("$.filamentos.length()").value(2))
                .andExpect(jsonPath("$.filamentos[0].custo").value(1.26))
                .andExpect(jsonPath("$.filamentos[1].codigoCor").value("#FF6600"))
                .andExpect(jsonPath("$.filamentos[1].gramas").value(7));
        verify(calculos).prever(argThat(r -> r.getFilamentos().size() == 2 && r.isFreteGratisTikTok()
                && r.getPlataformaVenda() == br.com.criare3d.models.enums.PlataformaVenda.TIKTOK));
        verifyNoInteractions(historico);
    }

    @Test
    void custoExtraMensalUsaUnidadesPrevistasEPreservaEstadoAoPausar() throws Exception {
        var id=UUID.randomUUID();
        var chave=UUID.randomUUID();
        var custo = new CustoLoja();
        custo.setId(id);
        custo.setMeta(new BigDecimal("120"));
        when(operacoes.executar(eq(chave), eq("CRIAR_CUSTO_LOJA"), anyString(), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<String>>getArgument(3).get());
        when(custosLoja.save(any())).thenReturn(custo);
        String dados = """
                {"descricao":"Energia fixa","inicio":"2026-11-01","meta":120,"tipo":"RATEIO_MENSAL","unidadesPrevistas":60}
                """;
        mvc.perform(post("/api/v1/custos-loja?chave="+chave).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(dados)).andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(id.toString()));
        verify(custosLoja).save(argThat(c -> c.isAtivo() && c.getUnidadesPrevistas()==60 && c.getTipo()==CustoLoja.Tipo.RATEIO_MENSAL));
        when(custosLoja.findById(id)).thenReturn(Optional.of(custo));
        when(rateios.recuperado(id)).thenReturn(new BigDecimal("18"));
        mvc.perform(put("/api/v1/custos-loja/"+id+"/estado?ativo=false").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/custos-loja/"+id).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.custo.restante").value(102)).andExpect(jsonPath("$.custo.ativo").value(false));
    }

    @Test
    void exclusaoSelecionadaPreservaItensEmUso() throws Exception {
        var livre=UUID.randomUUID();
        var usado=UUID.randomUUID();
        when(pecas.deleteUnused(any())).thenReturn(List.of(usado));
        mvc.perform(post("/api/v1/catalogo/pecas/remover-lote").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[\""+livre+"\",\""+usado+"\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(usado.toString()));
        verify(pecas).deleteUnused(argThat(ids -> ids.size()==2 && ids.containsAll(List.of(livre,usado))));
    }

    @Test
    void cadastrosDoCatalogoGravamNasQuatroCategorias() throws Exception {
        when(operacoes.executar(any(), startsWith("CADASTRAR_"), anyString(), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<String>>getArgument(3).get());
        var cadastros = Map.of(
                "pecas", """
                    {"nome":"Suporte","categoria":"Decoração","quantidadeFilamentoGramas":"12.5","tempoImpressaoMinutos":"30"}
                    """,
                "filamentos", """
                    {"nome":"PLA","cor":"Branco","codigoCor":"#ffffff","pesoRoloGramas":"1000","precoRolo":"50","estoqueGramas":"500"}
                    """,
                "impressoras", """
                    {"nome":"Da loja","modelo":"Modelo A","potenciaWatts":"120"}
                    """,
                "embalagens", """
                    {"nome":"Caixa","custoUnitario":"1.25"}
                    """);
        for (var entrada : cadastros.entrySet()) {
            mvc.perform(post("/api/v1/catalogo/"+entrada.getKey()).param("chave",UUID.randomUUID().toString())
                    .header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(entrada.getValue()))
                    .andExpect(status().isNoContent());
        }
        verify(pecas).create(argThat(p -> "Suporte".equals(p.getNome())));
        verify(filamentos).create(argThat(f -> f.getEstoqueGramas().intValueExact() == 500));
        verify(impressoras).create(argThat(i -> "Modelo A".equals(i.getModelo())));
        verify(embalagens).create(argThat(e -> e.getCustoUnitario().compareTo(new BigDecimal("1.25")) == 0));
    }

    @Test
    void loteSeparaReservasDaQuantidadeProntaParaVenda() throws Exception {
        var id=UUID.randomUUID();
        var lote=new CalculoHistorico();
        lote.setNomePeca("Suporte");
        lote.setUnidades(5);
        lote.setUnidadesVendidas(1);
        lote.setProduzido(true);
        lote.setCustoTotal(BigDecimal.TEN);
        var op=new ConsultaLoteService.Operacao(lote,3,List.of(),List.of(),List.of(),List.of(),
                BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ZERO);
        when(consultaLote.buscar(id)).thenReturn(Optional.of(op));
        mvc.perform(get("/api/v1/vendas/"+id).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponiveis").value(1))
                .andExpect(jsonPath("$.reservadas").value(3)).andExpect(jsonPath("$.custoLote").value(50));
        lote.setProduzido(false);
        mvc.perform(get("/api/v1/vendas/"+id).header("Authorization","Bearer "+token))
                .andExpect(jsonPath("$.disponiveis").value(0)).andExpect(jsonPath("$.reservadas").value(3));
    }

    @Test
    void cancelamentoDoLoteUsaVendaEQuantidadeConfirmadas() throws Exception {
        var id = UUID.randomUUID();
        var vendaId = UUID.randomUUID();
        var chave = UUID.randomUUID();
        when(operacoes.executar(eq(chave), eq("CANCELAR_VENDA_LOTE"), anyString(), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<String>>getArgument(3).get());
        when(venda.cancelarVenda(id, vendaId, 2)).thenReturn(ResultadoOperacao.OK);
        mvc.perform(post("/api/v1/vendas/"+id+"/cancelamento").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"vendaId":"%s","unidades":2,"chave":"%s"}
                        """.formatted(vendaId,chave)))
                .andExpect(status().isNoContent());
        verify(venda).cancelarVenda(id, vendaId, 2);
    }

    @Test
    void vendaRejeitadaPorReservaDevolveConflito() throws Exception {
        var id=UUID.randomUUID();
        var chave=UUID.randomUUID();
        when(venda.registrarVenda(id,2,null,null,null,chave)).thenReturn(ResultadoOperacao.RESERVA_ATIVA);
        mvc.perform(post("/api/v1/vendas/"+id+"/venda").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"unidades\":2,\"chave\":\""+chave+"\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("reservaAtiva"));
    }

    @Test
    void importarConsultaRapidaDevolveConsumoPorCorSemSalvar() throws Exception {
        var modelo = mock(br.com.criare3d.dto.Modelo3mfResponse.class);
        when(modelo.numeroPlacas()).thenReturn(2);
        when(modelo.placas()).thenReturn(List.of(
                new br.com.criare3d.dto.Modelo3mfPlacaResponse(1, "", null, null, List.of()),
                new br.com.criare3d.dto.Modelo3mfPlacaResponse(2, "data:image/png;base64,AQ==", null, null, List.of())));
        when(modelo.filamentosUsados()).thenReturn(List.of(
                new br.com.criare3d.dto.Modelo3mfFilamentoResponse(1, "PLA", "#ff0000", 13.5),
                new br.com.criare3d.dto.Modelo3mfFilamentoResponse(2, "PLA", "#ffffff", 6.75)));
        when(parser.analisarConsulta(any())).thenReturn(modelo);
        mvc.perform(multipart("/api/v1/calculos/arquivo").file("arquivo3mf",new byte[]{1})
                        .header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.placas").value(2))
                .andExpect(jsonPath("$.imagemPreviewDataUri").value("data:image/png;base64,AQ=="))
                .andExpect(jsonPath("$.materiais[1].cor").value("#ffffff"))
                .andExpect(jsonPath("$.materiais[1].pesoGramas").value(6.75));
        verifyNoInteractions(historico, calculos);
    }

    @Test
    void consultaAvulsaNaoGravaEHistoricoUsaConfirmacaoDePreco() throws Exception {
        var id = UUID.randomUUID();
        var entrada = """
                {"impressoraId":"%1$s","embalagemId":"%1$s","filamentos":[{"filamentoId":"%1$s",
                "quantidadeFilamentoGramas":12}],"minutos":20,"unidades":1}
                """.formatted(id);
        var previa = mock(br.com.criare3d.dto.calculo.ResultadoCalculo.class);
        when(previa.precoSugerido()).thenReturn(BigDecimal.TEN);
        when(calculos.preverAvulsoTotais(any(), nullable(String.class), eq(1))).thenReturn(previa);
        mvc.perform(post("/api/v1/calculos/previa").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada))
                .andExpect(status().isOk()).andExpect(jsonPath("$.precoUnitario").value(10));
        verifyNoInteractions(historico);

        var h = new CalculoHistorico();
        h.setCustoTotal(BigDecimal.ONE);
        h.setPrecoSugerido(BigDecimal.TEN);
        h.setNomePeca("Suporte");
        when(calculos.calcular(any())).thenReturn(h);
        when(historico.create(h)).thenReturn(h);
        var salvar = "{\"entrada\":" + entrada.replace("{\"impressoraId", "{\"pecaId\":\"" + id + "\",\"impressoraId")
                + ",\"custoEsperado\":1,\"precoEsperado\":%s}";
        mvc.perform(post("/api/v1/calculos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(salvar.formatted(9)))
                .andExpect(status().isConflict());
        verifyNoInteractions(historico);
        mvc.perform(post("/api/v1/calculos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(salvar.formatted(10)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Suporte"));
        verify(historico).create(h);
        String comFoto = salvar.formatted(10).stripTrailing().replaceFirst("}$", ",\"foto\":\"%s\"}");
        mvc.perform(post("/api/v1/calculos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(comFoto.formatted("YWJj")))
                .andExpect(status().isUnprocessableEntity());
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+rWaQAAAAASUVORK5CYII=");
        when(historico.create(h,"image/png",png)).thenReturn(h);
        mvc.perform(post("/api/v1/calculos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(comFoto.formatted(Base64.getEncoder().encodeToString(png))))
                .andExpect(status().isOk());
        verify(historico).create(h,"image/png",png);

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
