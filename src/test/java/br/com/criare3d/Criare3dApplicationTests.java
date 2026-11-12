package br.com.criare3d;
import br.com.criare3d.service.ResultadoOperacao;

import br.com.criare3d.service.CalculoService;
import br.com.criare3d.service.IndicadoresService;
import br.com.criare3d.service.ProducaoService;
import br.com.criare3d.service.VendaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.Peca;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.Embalagem;
import br.com.criare3d.models.Configuracao;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.ItemCalculoHistorico;
import br.com.criare3d.models.ManutencaoImpressora;
import br.com.criare3d.models.CalculoFoto;
import br.com.criare3d.models.Tenant;
import br.com.criare3d.dto.CompraFilamentoRequest;
import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.dto.DashboardResumo;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.ConfiguracaoRepository;
import br.com.criare3d.repository.EmbalagemRepository;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.repository.ImpressoraRepository;
import br.com.criare3d.repository.PecaRepository;
import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.repository.TenantRepository;
import br.com.criare3d.repository.ItemCalculoHistoricoRepository;
import br.com.criare3d.repository.ManutencaoImpressoraRepository;
import br.com.criare3d.repository.CalculoFotoRepository;
import br.com.criare3d.security.UsuarioPrincipal;
import br.com.criare3d.service.FilamentoServiceImp;
import br.com.criare3d.service.ClienteAdminService;
import br.com.criare3d.service.interfaces.UsuarioService;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class Criare3dApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private TenantRepository tenantRepository;
	@Autowired
	private FilamentoRepository filamentoRepository;
	@Autowired
	private PecaRepository pecaRepository;
	@Autowired
	private ImpressoraRepository impressoraRepository;
	@Autowired
	private EmbalagemRepository embalagemRepository;
	@Autowired
	private ConfiguracaoRepository configuracaoRepository;
	@Autowired
	private CalculoHistoricoRepository calculoHistoricoRepository;
	@Autowired
	private ItemCalculoHistoricoRepository itemCalculoHistoricoRepository;
	@Autowired
	private ManutencaoImpressoraRepository manutencaoImpressoraRepository;
	@Autowired
	private CalculoFotoRepository calculoFotoRepository;
	@Autowired
	private FilamentoServiceImp filamentoService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired private CalculoService calculoService;
    @org.springframework.beans.factory.annotation.Autowired private IndicadoresService indicadoresService;
    @org.springframework.beans.factory.annotation.Autowired private ProducaoService producaoService;
    @org.springframework.beans.factory.annotation.Autowired private VendaService vendaService;
	@Autowired
	private ClienteAdminService clienteAdminService;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void removerTenantsDeTeste() {
		SecurityContextHolder.clearContext();
		tenantRepository.findAll().stream()
				.filter(tenant -> tenant.getSlug() != null && tenant.getSlug().startsWith("teste-"))
				.forEach(tenant -> {
					UUID tenantId = tenant.getId();
					for (String tabela : java.util.List.of("melhor_envio_conexao", "foto_item_pedido", "venda_calculo", "item_pedido", "pedido_orcamento", "foto_rascunho", "custo_personalizado", "planejamento_producao", "falha_producao", "venda_calculo", "calculo_foto", "item_calculo_historico",
							"manutencao_impressora", "calculo_historico", "configuracao", "embalagem",
							"filamento", "impressora", "peca", "usuario")) {
						jdbcTemplate.update("DELETE FROM " + tabela + " WHERE tenant_id = ?", tenantId);
					}
					jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", tenantId);
				});
	}

	@Test
	void contextoDaAplicacaoIniciaComBancoDeTeste() {
		assertThat(passwordEncoder).isNotNull();
	}

	@Test
	void migrationRemoveDefaultsDeTenantParaEvitarVinculosSilenciosos() {
		assertThat(jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.columns
				WHERE table_schema = current_schema()
				  AND column_name = 'tenant_id'
				  AND column_default IS NOT NULL
				""", String.class))
				.isEmpty();
	}

	@Test
	void codificadorDeSenhaGeraHashEValidaSenhaOriginal() {
		String senha = "SenhaForteDeTeste123!";
		String hash = passwordEncoder.encode(senha);

		assertThat(hash).isNotEqualTo(senha);
		assertThat(passwordEncoder.matches(senha, hash)).isTrue();
		assertThat(passwordEncoder.matches("senha-incorreta", hash)).isFalse();
	}

	@Test
	void usuarioNaoConsegueBuscarRegistrosDeOutraOrganizacaoNasNoveTabelas() {
		Usuario usuarioA = criarUsuario("a-" + UUID.randomUUID() + "@teste.local");
		Usuario usuarioB = criarUsuario("b-" + UUID.randomUUID() + "@teste.local");

		Filamento filamentoA;
		Peca pecaA;
		Impressora impressoraA;
		Embalagem embalagemA;
		Configuracao configuracaoA;
		CalculoHistorico calculoA;
		ManutencaoImpressora manutencaoA;
		ItemCalculoHistorico itemA;
		CalculoFoto fotoA;

		autenticarComo(usuarioA);
		filamentoA = criarFilamento(usuarioA);
		pecaA = pecaRepository.saveAndFlush(Peca.builder()
				.nome("Peça teste").quantidadeFilamentoGramas(new BigDecimal("10"))
				.tempoImpressaoMinutos(60L).usuario(usuarioA).build());
		impressoraA = impressoraRepository.saveAndFlush(Impressora.builder()
				.nome("Impressora teste").modelo("Modelo teste")
				.potenciaWatts(new BigDecimal("100")).usuario(usuarioA).build());
		embalagemA = embalagemRepository.saveAndFlush(Embalagem.builder()
				.nome("Caixa teste").custoUnitario(new BigDecimal("2.00")).usuario(usuarioA).build());
		configuracaoA = configuracaoRepository.saveAndFlush(Configuracao.builder()
				.markupPercentual(new BigDecimal("100")).tarifaEnergiaKwh(new BigDecimal("0.80"))
				.embalagemFixa(false).usuario(usuarioA).build());
		calculoA = criarCalculoHistorico(usuarioA);
		calculoA.setFilamento(filamentoA);
		calculoA.setImpressora(impressoraA);
		calculoA.setEmbalagem(embalagemA);
		calculoA.setPeca(pecaA);
		calculoHistoricoRepository.saveAndFlush(calculoA);

		manutencaoA = manutencaoImpressoraRepository.saveAndFlush(ManutencaoImpressora.builder()
				.impressora(impressoraA).descricao("Manutenção teste").valor(new BigDecimal("10.00"))
				.data(LocalDate.now()).build());
		itemA = itemCalculoHistoricoRepository.saveAndFlush(ItemCalculoHistorico.builder()
				.calculoHistorico(calculoA).filamento(filamentoA).nomeFilamento(filamentoA.getNome())
				.corFilamento(filamentoA.getCor()).precoRoloFilamento(filamentoA.getPrecoRolo())
				.pesoRoloGramas(filamentoA.getPesoRoloGramas()).quantidadeFilamentoGramas(new BigDecimal("10.00"))
				.custoFilamento(new BigDecimal("0.80")).build());
		fotoA = new CalculoFoto();
		fotoA.setCalculoId(calculoA.getId());
		fotoA.setTipo("image/jpeg");
		fotoA.setBytes(new byte[]{1, 2, 3});
		calculoFotoRepository.saveAndFlush(fotoA);

		try {
			autenticarComo(usuarioB);
			assertThat(pecaRepository.findAll()).noneMatch(peca -> peca.getId().equals(pecaA.getId()));
			assertThat(filamentoService.fetchById(filamentoA.getId())).isEmpty();
			assertThat(pecaRepository.findById(pecaA.getId())).isEmpty();
			assertThat(impressoraRepository.findById(impressoraA.getId())).isEmpty();
			assertThat(embalagemRepository.findById(embalagemA.getId())).isEmpty();
			assertThat(configuracaoRepository.findById(configuracaoA.getId())).isEmpty();
			assertThat(calculoHistoricoRepository.findById(calculoA.getId())).isEmpty();
			assertThat(manutencaoImpressoraRepository.findById(manutencaoA.getId())).isEmpty();
			assertThat(itemCalculoHistoricoRepository.findById(itemA.getId())).isEmpty();
			assertThat(calculoFotoRepository.findById(fotoA.getCalculoId())).isEmpty();
			pecaRepository.deleteById(pecaA.getId());

			autenticarComo(usuarioA);
			assertThat(filamentoService.fetchById(filamentoA.getId())).isPresent();
			assertThat(pecaRepository.findById(pecaA.getId())).isPresent();
			assertThat(impressoraRepository.findById(impressoraA.getId())).isPresent();
			assertThat(embalagemRepository.findById(embalagemA.getId())).isPresent();
			assertThat(configuracaoRepository.findById(configuracaoA.getId())).isPresent();
			assertThat(calculoHistoricoRepository.findById(calculoA.getId())).isPresent();
			assertThat(manutencaoImpressoraRepository.findById(manutencaoA.getId())).isPresent();
			assertThat(itemCalculoHistoricoRepository.findById(itemA.getId())).isPresent();
			assertThat(calculoFotoRepository.findById(fotoA.getCalculoId())).isPresent();
		} finally {
			autenticarComo(usuarioA);
			calculoFotoRepository.deleteById(fotoA.getCalculoId());
			itemCalculoHistoricoRepository.deleteById(itemA.getId());
			manutencaoImpressoraRepository.deleteById(manutencaoA.getId());
			calculoHistoricoRepository.deleteById(calculoA.getId());
			configuracaoRepository.deleteById(configuracaoA.getId());
			embalagemRepository.deleteById(embalagemA.getId());
			impressoraRepository.deleteById(impressoraA.getId());
			pecaRepository.deleteById(pecaA.getId());
			filamentoRepository.deleteById(filamentoA.getId());
			SecurityContextHolder.clearContext();
			usuarioRepository.deleteAllById(java.util.List.of(usuarioA.getId(), usuarioB.getId()));
		}
	}

	@Test
	void requisicaoSemTenantNaoConsegueListarDadosDeNegocio() {
		Usuario semTenant = Usuario.builder()
				.id(UUID.randomUUID())
				.email("sem-tenant@teste.local")
				.password("irrelevante")
				.perfil(Perfil.USER)
				.build();
		autenticarComo(semTenant);
		try {
			assertThat(pecaRepository.findAll()).isEmpty();
			assertThat(filamentoRepository.findAll()).isEmpty();
			assertThat(impressoraRepository.findAll()).isEmpty();
			assertThat(embalagemRepository.findAll()).isEmpty();
			assertThat(configuracaoRepository.findAll()).isEmpty();
			assertThat(calculoHistoricoRepository.findAll()).isEmpty();
			assertThat(manutencaoImpressoraRepository.findAll()).isEmpty();
			assertThat(itemCalculoHistoricoRepository.findAll()).isEmpty();
			assertThat(calculoFotoRepository.findAll()).isEmpty();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void senhaRedefinidaPeloAdminExigeTrocaNoProximoLogin() {
		Usuario usuario = criarUsuario("reset-" + UUID.randomUUID() + "@teste.local");
		try {
			usuarioService.redefinirSenhaPorAdmin(usuario.getId(), "TemporariaTeste123!");
			Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
			assertThat(passwordEncoder.matches("TemporariaTeste123!", atualizado.getPassword())).isTrue();
			assertThat(atualizado.isTrocaSenhaObrigatoria()).isTrue();

			autenticarComo(usuario);
			usuarioService.alterarSenhaAtual("TemporariaTeste123!", "NovaSenhaTeste123!");
			assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().isTrocaSenhaObrigatoria()).isFalse();
		} finally {
			SecurityContextHolder.clearContext();
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	@Test
	void excluirUmClienteMantemOsDadosDoOutroTenant() {
		Usuario usuarioA = criarUsuario("excluir-a-" + UUID.randomUUID() + "@teste.local");
		Usuario usuarioB = criarUsuario("excluir-b-" + UUID.randomUUID() + "@teste.local");
		autenticarComo(usuarioA);
		Peca pecaA = pecaRepository.saveAndFlush(Peca.builder()
				.nome("Peça do tenant A").quantidadeFilamentoGramas(new BigDecimal("10"))
				.tempoImpressaoMinutos(30L).usuario(usuarioA).build());
		autenticarComo(usuarioB);
		Peca pecaB = pecaRepository.saveAndFlush(Peca.builder()
				.nome("Peça do tenant B").quantidadeFilamentoGramas(new BigDecimal("20"))
				.tempoImpressaoMinutos(45L).usuario(usuarioB).build());
		Usuario admin = Usuario.builder().id(UUID.randomUUID()).email("admin-teste@teste.local")
				.password("hash").perfil(Perfil.ADMIN).build();

		try {
			autenticarComo(admin);
			clienteAdminService.excluir(usuarioA.getTenant().getId(), "Tenant de teste");

			autenticarComo(usuarioB);
			assertThat(pecaRepository.findById(pecaB.getId())).isPresent();
			assertThat(tenantRepository.findById(usuarioB.getTenant().getId())).isPresent();
			var excluido = tenantRepository.findById(usuarioA.getTenant().getId()).orElseThrow();
            assertThat(excluido.isAtivo()).isFalse();
            assertThat(excluido.getExcluidoEm()).isNotNull();
            autenticarComo(admin);
            clienteAdminService.restaurar(usuarioA.getTenant().getId());
            autenticarComo(usuarioA);
            assertThat(pecaRepository.findById(pecaA.getId())).isPresent();
			assertThat(pecaA.getTenantId()).isNotEqualTo(pecaB.getTenantId());
		} finally {
			SecurityContextHolder.clearContext();
			jdbcTemplate.update("DELETE FROM peca WHERE id = ? AND tenant_id = ?", pecaB.getId(), usuarioB.getTenant().getId());
			jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioB.getId());
			jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", usuarioB.getTenant().getId());
		}
	}

	@Test
	void dashboardAgrupaLucroPelaDataDaVendaEnaoPelaDataDoCalculo() {
		Usuario usuario = criarUsuario("venda-" + UUID.randomUUID() + "@teste.local");
		autenticarComo(usuario);
		LocalDateTime agora = LocalDateTime.now().withNano(0);
		LocalDateTime dataVenda = agora.minusDays(2);
		CalculoHistorico calculo = criarCalculoHistorico(usuario);
		calculo.setDataCalculo(dataVenda.minusMonths(6));
		calculo.setDataVenda(dataVenda);
		calculo.setVendido(true);
		calculo.setUnidades(3);
		calculo.setFornadas(3);
		calculo.setLucroLiquido(new BigDecimal("25.00"));
		calculoHistoricoRepository.saveAndFlush(calculo);
        var vendaHistorica=new br.com.criare3d.models.VendaCalculo();vendaHistorica.setCalculo(calculo);
        vendaHistorica.setUnidades(3);vendaHistorica.setDataVenda(dataVenda);vendaCalculoRepository.saveAndFlush(vendaHistorica);

		try {
		DashboardResumo resumoVenda = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
				usuario.getTenant().getId(), dataVenda.minusDays(1), agora.plusDays(1)));
			assertThat(resumoVenda.vendidas()).isEqualTo(3);
			assertThat(resumoVenda.lucroVendido()).isEqualByComparingTo("75.00");
			assertThat(resumoVenda.minutosVendidos()).isEqualTo(180L);
			DashboardResumo resumoDataCalculo = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
					usuario.getTenant().getId(), dataVenda.minusMonths(6).minusDays(1), dataVenda.minusMonths(6).plusDays(1)));
			assertThat(resumoDataCalculo.lucroVendido()).isEqualByComparingTo("0.00");
			assertThat(calculoHistoricoRepository.primeiraVendaComLucro())
					.isEqualTo(dataVenda);
			DashboardResumo semTenant = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
					new UUID(0L, 0L), dataVenda.minusDays(1), agora.plusDays(1)));
			assertThat(semTenant.vendidas()).isZero();
		} finally {
			calculoHistoricoRepository.deleteById(calculo.getId());
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	@Test
	void compraDeFilamentoSomaEstoqueEAtualizaCustoMedioPonderado() {
		Usuario usuario = criarUsuario("compra-" + UUID.randomUUID() + "@teste.local");
		autenticarComo(usuario);
		Filamento filamento = criarFilamento(usuario);
		try {
			autenticarComo(usuario);
			CompraFilamentoRequest compra = new CompraFilamentoRequest();
			compra.setGramasCompradas(new BigDecimal("1000"));
			compra.setPesoRoloGramas(new BigDecimal("1000"));
			compra.setPrecoRolo(new BigDecimal("95.50"));

			assertThat(filamentoService.registrarCompra(filamento.getId(), compra)).isTrue();
			Filamento atualizado = filamentoService.fetchById(filamento.getId()).orElseThrow();
			assertThat(atualizado.getEstoqueGramas()).isEqualByComparingTo("2000.00");
			// 1.000 g a R$ 80,00 + 1.000 g a R$ 95,50: custo médio de R$ 87,75 por 1.000 g.
			assertThat(atualizado.getPesoRoloGramas()).isEqualByComparingTo("1000");
			assertThat(atualizado.getPrecoRolo()).isEqualByComparingTo("87.75");
		} finally {
			autenticarComo(usuario);
			filamentoRepository.deleteById(filamento.getId());
			SecurityContextHolder.clearContext();
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	@Test
	void produzirLoteDescontaEDevolverLoteRestauraEstoqueMultiplicadoPelasUnidades() {
		Usuario usuario = criarUsuario("lote-" + UUID.randomUUID() + "@teste.local");
		autenticarComo(usuario);
		Filamento filamento = criarFilamento(usuario);
		CalculoHistorico calculo = criarCalculoHistorico(usuario);
		calculo.setUnidades(3);
		calculo.setFilamento(filamento);
		calculo.setQuantidadeFilamentoGramas(new BigDecimal("50.00"));
		ItemCalculoHistorico item = ItemCalculoHistorico.builder()
				.calculoHistorico(calculo).filamento(filamento)
				.nomeFilamento(filamento.getNome()).corFilamento(filamento.getCor())
				.codigoCor(filamento.getCodigoCor()).precoRoloFilamento(filamento.getPrecoRolo())
				.pesoRoloGramas(filamento.getPesoRoloGramas())
				.quantidadeFilamentoGramas(new BigDecimal("50.00"))
				.custoFilamento(new BigDecimal("4.00")).build();
		calculo.getItensFilamento().add(item);
		calculoHistoricoRepository.saveAndFlush(calculo);

		try {
			autenticarComo(usuario);
			assertThat(producaoService.atualizarProduzido(calculo.getId(), true)).isEqualTo(ResultadoOperacao.OK);
			assertThat(filamentoService.fetchById(filamento.getId()).orElseThrow().getEstoqueGramas())
					.isEqualByComparingTo("850.00");

			assertThat(producaoService.atualizarProduzido(calculo.getId(), false)).isEqualTo(ResultadoOperacao.OK);
			assertThat(filamentoService.fetchById(filamento.getId()).orElseThrow().getEstoqueGramas())
					.isEqualByComparingTo("1000.00");
		} finally {
			autenticarComo(usuario);
			calculoHistoricoRepository.deleteById(calculo.getId());
			filamentoRepository.deleteById(filamento.getId());
			SecurityContextHolder.clearContext();
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	@Test
	void calculaLoteComDuasMisturasPurgaEAquecimento() {
		Usuario usuario = criarUsuario("calculo-lote-" + UUID.randomUUID() + "@teste.local");
		autenticarComo(usuario);
		Filamento filamentoA = criarFilamento(usuario);
		Filamento filamentoB = criarFilamento(usuario);
		Peca peca = pecaRepository.saveAndFlush(Peca.builder()
				.nome("Peça para lote integrado")
				.quantidadeFilamentoGramas(new BigDecimal("100.00"))
				.tempoImpressaoMinutos(60L).usuario(usuario).build());
		Impressora impressora = impressoraRepository.saveAndFlush(Impressora.builder()
				.nome("Impressora lote integrada").modelo("Modelo teste")
				.potenciaWatts(new BigDecimal("100.00"))
				.tempoAquecimentoMinutos(5).usuario(usuario).build());
		Embalagem embalagem = embalagemRepository.saveAndFlush(Embalagem.builder()
				.nome("Caixa lote integrada").custoUnitario(new BigDecimal("2.00")).usuario(usuario).build());
		Configuracao configuracao = configuracaoRepository.saveAndFlush(Configuracao.builder()
				.markupPercentual(new BigDecimal("100.00"))
				.markupFixo(false).tarifaEnergiaKwh(new BigDecimal("0.80"))
				.embalagemFixa(false).custoEmbalagemFixa(BigDecimal.ZERO)
				.purgaGramasPorImpressao(new BigDecimal("0.100000"))
				.usuario(usuario).build());

		try {
			autenticarComo(usuario);
			CalculoRequest request = new CalculoRequest();
			request.setPecaId(peca.getId());
			request.setImpressoraId(impressora.getId());
			request.setEmbalagemId(embalagem.getId());
			request.setUnidades(10);
			request.setPecasPorImpressao(4);
			request.setTempoImpressaoMinutos(240L);
			request.setMarkupPercentual(new BigDecimal("100.00"));
			ItemCalculoRequest itemA = new ItemCalculoRequest();
			itemA.setFilamentoId(filamentoA.getId());
			itemA.setQuantidadeFilamentoGramas(new BigDecimal("30.00"));
			ItemCalculoRequest itemB = new ItemCalculoRequest();
			itemB.setFilamentoId(filamentoB.getId());
			itemB.setQuantidadeFilamentoGramas(new BigDecimal("70.00"));
			request.setFilamentos(java.util.List.of(itemA, itemB));

			CalculoHistorico resultado = calculoService.calcular(request);

			assertThat(resultado.getUnidades()).isEqualTo(10);
			assertThat(resultado.getPecasPorImpressao()).isEqualTo(4);
			assertThat(resultado.getFornadas()).isEqualTo(3);
			assertThat(resultado.getTempoAquecimentoMinutos()).isEqualTo(5);
			assertThat(resultado.getMinutosMaquinaLote()).isEqualTo(2415L);
			assertThat(resultado.getCustoEnergia()).isEqualByComparingTo("0.32200000");
			assertThat(resultado.getItensFilamento()).hasSize(2);
			assertThat(resultado.getItensFilamento().stream()
					.map(ItemCalculoHistorico::getPurgaGramas)
					.reduce(BigDecimal.ZERO, BigDecimal::add))
				.isEqualByComparingTo("0.03000000");
			assertThat(resultado.getCustoPurga()).isEqualByComparingTo("0.00240000");
		} finally {
			autenticarComo(usuario);
			configuracaoRepository.deleteById(configuracao.getId());
			embalagemRepository.deleteById(embalagem.getId());
			impressoraRepository.deleteById(impressora.getId());
			pecaRepository.deleteById(peca.getId());
			filamentoRepository.deleteAllById(java.util.List.of(filamentoA.getId(), filamentoB.getId()));
			SecurityContextHolder.clearContext();
			usuarioRepository.deleteById(usuario.getId());
		}
	}

    @Autowired private br.com.criare3d.repository.VendaCalculoRepository vendaCalculoRepository;
    @Autowired private br.com.criare3d.repository.FalhaProducaoRepository falhaProducaoRepository;
    @Autowired private br.com.criare3d.repository.PlanejamentoProducaoRepository planejamentoProducaoRepository;
    @Autowired private br.com.criare3d.service.FalhaProducaoService falhaProducaoService;
    @Autowired private br.com.criare3d.service.PlanejamentoProducaoService planejamentoProducaoService;
    @Autowired private br.com.criare3d.service.OrcamentoPedidoService orcamentos;
    @Autowired private br.com.criare3d.service.OperacaoPedidoService pedidos;
    @Autowired private br.com.criare3d.repository.ItemPedidoRepository reservasPedido;

    @Test
    void pedidoMistoReservaProduzSomenteFaltanteEntregaECancelaSemApagarOutraVenda() {
        var u=criarUsuario("pedido-"+UUID.randomUUID()+"@teste.local");autenticarComo(u);
        var lote=criarLoteOperacao(u);
        configuracaoRepository.saveAndFlush(Configuracao.builder().usuario(u).markupFixo(false)
                .markupPercentual(new BigDecimal("100")).tarifaEnergiaKwh(new BigDecimal("0.8"))
                .embalagemFixa(true).custoEmbalagemFixa(BigDecimal.ONE).build());
        assertThat(producaoService.atualizarProduzido(lote.getId(),true)).isEqualTo(ResultadoOperacao.OK);
        var antes=filamentoRepository.findById(lote.getFilamento().getId()).orElseThrow().getEstoqueGramas();
        long calculosAntes=calculoHistoricoRepository.count();
        var fotoOrigem=new CalculoFoto();fotoOrigem.setCalculoId(lote.getId());fotoOrigem.setTipo("image/png");
        fotoOrigem.setBytes(java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/Z1sAAAAASUVORK5CYII="));
        calculoFotoRepository.saveAndFlush(fotoOrigem);
        var p=orcamentos.iniciar(lote.getId(),4,2);
        assertThat(p.getItens().getFirst().getId()).isNotNull();
        assertThat(fotosPedidoRepository.existsById(p.getItens().getFirst().getId())).isTrue();
        var dados=new br.com.criare3d.dto.PedidoDadosRequest();dados.setTitulo("Pedido integrado");dados.setCliente("Cliente teste");
        orcamentos.salvarDados(p.getId(),dados);orcamentos.enviar(p.getId());
        assertThat(calculoHistoricoRepository.count()).isEqualTo(calculosAntes);
        assertThat(filamentoRepository.findById(lote.getFilamento().getId()).orElseThrow().getEstoqueGramas()).isEqualByComparingTo(antes);
        p=orcamentos.buscar(p.getId());pedidos.aprovar(p.getId(),p.getRevisao());p=orcamentos.buscar(p.getId());
        // Simula abrir o pedido em outra requisição, sem sessão Hibernate anterior.
        assertThat(orcamentos.materiais(p).materiais()).isNotEmpty();
        assertThat(orcamentos.materiais(orcamentos.buscar(p.getId())).materiais()).isNotEmpty();
        var item=p.getItens().getFirst();
        assertThat(item.getLoteProducao().getUnidades()).isEqualTo(2);
        assertThat(reservasPedido.reservadas(lote.getId())).isEqualTo(2);
        assertThat(vendaService.registrarVenda(lote.getId(),2)).isEqualTo(ResultadoOperacao.RESERVA_ATIVA);
        assertThat(vendaService.registrarVenda(lote.getId(),1)).isEqualTo(ResultadoOperacao.OK);
        assertThat(producaoService.atualizarProduzido(lote.getId(),false)).isEqualTo(ResultadoOperacao.RESERVA_ATIVA);
        pedidos.produzir(p.getId(),item.getId(),p.getRevisao());p=orcamentos.buscar(p.getId());
        var depois=filamentoRepository.findById(lote.getFilamento().getId()).orElseThrow().getEstoqueGramas();
        assertThat(depois).isLessThan(antes);
        pedidos.entregar(p.getId(),p.getRevisao());p=orcamentos.buscar(p.getId());
        assertThat(p.getStatus()).isEqualTo(br.com.criare3d.models.enums.StatusPedido.ENTREGUE);
        assertThat(reservasPedido.reservadas(lote.getId())).isZero();
        assertThat(calculoHistoricoRepository.findById(lote.getId()).orElseThrow().getUnidadesVendidas()).isEqualTo(3);
        pedidos.cancelar(p.getId(),p.getRevisao());
        assertThat(calculoHistoricoRepository.findById(lote.getId()).orElseThrow().getUnidadesVendidas()).isEqualTo(1);
        var produzido=calculoHistoricoRepository.findById(item.getLoteProducao().getId()).orElseThrow();
        assertThat(produzido.getProduzido()).isTrue();assertThat(produzido.getUnidadesVendidas()).isZero();
        assertThat(filamentoRepository.findById(lote.getFilamento().getId()).orElseThrow().getEstoqueGramas()).isEqualByComparingTo(depois);
    }

    @Test void filaDeImpressoraIsolaContasEIgnoraLoteProduzidoMesmoComPlanoPendente(){
        var a=criarUsuario("fila-a-"+UUID.randomUUID()+"@teste.local");autenticarComo(a);var loteA=criarLoteOperacao(a);
        var plano=new br.com.criare3d.models.PlanejamentoProducao();
        plano.setCalculo(loteA);plano.setImpressora(loteA.getImpressora());
        plano.setNomePeca(loteA.getNomePeca());plano.setNomeImpressora(loteA.getNomeImpressora());
        plano.setUnidades(loteA.getUnidades());plano.setPecasPorImpressao(1);
        plano.setIntervalo(10);plano.setFolga(10);
        plano.setInicio(LocalDateTime.now().plusDays(1));plano.setFim(plano.getInicio().plusDays(9));
        planejamentoProducaoRepository.saveAndFlush(plano);
        var impressoraA=loteA.getImpressora().getId();
        assertThat(planejamentoProducaoRepository.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(impressoraA)).isPresent();
        var b=criarUsuario("fila-b-"+UUID.randomUUID()+"@teste.local");autenticarComo(b);var loteB=criarLoteOperacao(b);
        assertThat(planejamentoProducaoRepository.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(impressoraA)).isEmpty();
        assertThat(planejamentoProducaoRepository.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(loteB.getImpressora().getId())).isEmpty();
        assertThat(planejamentoProducaoRepository.findByCanceladoFalseAndConcluidoFalseOrderByInicioAsc(org.springframework.data.domain.PageRequest.of(0,20))).isEmpty();
        autenticarComo(a);loteA.setProduzido(true);calculoHistoricoRepository.saveAndFlush(loteA);
        assertThat(planejamentoProducaoRepository.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(impressoraA)).isEmpty();
        // Um planejamento avulso continua visível até a venda, sem ocupar a impressora após produzir.
        assertThat(planejamentoProducaoRepository.findByCanceladoFalseAndConcluidoFalseOrderByInicioAsc(org.springframework.data.domain.PageRequest.of(0,20))).hasSize(1);
        assertThat(planejamentoProducaoRepository.findByCanceladoFalseAndConcluidoTrueOrderByInicioDesc(org.springframework.data.domain.PageRequest.of(0,20))).isEmpty();
        loteA.setVendido(true);loteA.setUnidadesVendidas(loteA.getUnidades());calculoHistoricoRepository.saveAndFlush(loteA);
        assertThat(planejamentoProducaoRepository.findByCanceladoFalseAndConcluidoFalseOrderByInicioAsc(org.springframework.data.domain.PageRequest.of(0,20))).isEmpty();
        assertThat(planejamentoProducaoRepository.findByCanceladoFalseAndConcluidoTrueOrderByInicioDesc(org.springframework.data.domain.PageRequest.of(0,20))).hasSize(1);
    }

    @Test
    void orcamentoItemEReservasNaoSaoAcessiveisPorOutroCliente() {
        var a=criarUsuario("pedido-a-"+UUID.randomUUID()+"@teste.local");autenticarComo(a);
        var p=new br.com.criare3d.models.PedidoOrcamento();p.setCriadoEm(LocalDateTime.now());
        var repository=orcamentoRepository;
        p=repository.saveAndFlush(p);
        var b=criarUsuario("pedido-b-"+UUID.randomUUID()+"@teste.local");autenticarComo(b);
        assertThat(repository.findById(p.getId())).isEmpty();
        final var id=p.getId();
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException().isThrownBy(()->pedidos.aprovar(id,0));
    }
    @Autowired private br.com.criare3d.repository.PedidoOrcamentoRepository orcamentoRepository;
    @Autowired private br.com.criare3d.repository.FotoItemPedidoRepository fotosPedidoRepository;

    private CalculoHistorico criarLoteOperacao(Usuario usuario) {
        var f = criarFilamento(usuario);
        var impressora = impressoraRepository.saveAndFlush(Impressora.builder().nome("Impressora operação")
                .modelo("Modelo teste").potenciaWatts(new BigDecimal("100")).usuario(usuario).build());
        var peca = pecaRepository.saveAndFlush(Peca.builder().nome("Peça operação").categoria("Decoração")
                .quantidadeFilamentoGramas(BigDecimal.TEN).tempoImpressaoMinutos(60L).usuario(usuario).build());
        var c = criarCalculoHistorico(usuario);c.setUnidades(3);c.setFornadas(3);c.setFilamento(f);c.setImpressora(impressora);c.setPeca(peca);
        c.setDataCalculo(LocalDateTime.now().minusMonths(6));c.setMinutosMaquinaLote(180L);c.setGramasTotaisLote(new BigDecimal("30"));
        return calculoHistoricoRepository.saveAndFlush(c);
    }
    private void limparOperacao(Usuario usuario) {
        UUID tenant=usuario.getTenant().getId();
        for (String tabela : java.util.List.of("melhor_envio_conexao", "foto_item_pedido", "venda_calculo", "item_pedido", "pedido_orcamento", "foto_rascunho", "custo_personalizado","planejamento_producao","falha_producao","venda_calculo","calculo_foto","item_calculo_historico","calculo_historico","peca","filamento","impressora","configuracao","embalagem"))
            jdbcTemplate.update("DELETE FROM " + tabela + " WHERE tenant_id = ?",tenant);
        usuarioRepository.deleteById(usuario.getId());SecurityContextHolder.clearContext();tenantRepository.deleteById(tenant);
    }
    @Test
    void producaoUsaDataRealENaoDataAntigaDoCalculo() {
        var u=criarUsuario("producao-data-"+UUID.randomUUID()+"@teste.local");autenticarComo(u);var c=criarLoteOperacao(u);
        try {
            assertThat(producaoService.atualizarProduzido(c.getId(),true)).isEqualTo(ResultadoOperacao.OK);
            var salvo=calculoHistoricoRepository.findById(c.getId()).orElseThrow();
            var agora=LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"));
            assertThat(salvo.getDataProducao()).isAfter(agora.minusMinutes(1));
            var resumo=calculoHistoricoRepository.resumirDashboard(u.getTenant().getId(),agora.minusDays(1),agora.plusDays(1));
            assertThat(resumo.getProduzidas().longValue()).isEqualTo(3L);
        } finally {limparOperacao(u);}
    }
    @Test
    void vendaParcialComPrecoRealECancelamentoPreservaAuditoria() {
        var u=criarUsuario("venda-parcial-"+UUID.randomUUID()+"@teste.local");autenticarComo(u);var c=criarLoteOperacao(u);
        try {
            assertThat(vendaService.registrarVenda(c.getId(),1)).isEqualTo(ResultadoOperacao.PRODUCAO_NECESSARIA);
            assertThat(producaoService.atualizarProduzido(c.getId(),true)).isEqualTo(ResultadoOperacao.OK);
            assertThat(vendaService.registrarVenda(c.getId(),2,new BigDecimal("18"),new BigDecimal("2"),new BigDecimal("3"))).isEqualTo(ResultadoOperacao.OK);
            var venda=vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(c.getId()).getFirst();
            assertThat(vendaService.cancelarVenda(c.getId(),venda.getId(),1)).isEqualTo(ResultadoOperacao.OK);
            var ativa=vendaCalculoRepository.findById(venda.getId()).orElseThrow();
            assertThat(ativa.getUnidadesAtivas()).isEqualTo(1);assertThat(ativa.getUnidadesCanceladas()).isEqualTo(1);
            assertThat(calculoHistoricoRepository.findById(c.getId()).orElseThrow().getUnidadesVendidas()).isEqualTo(1);
            assertThat(producaoService.atualizarProduzido(c.getId(),false)).isEqualTo(ResultadoOperacao.VENDA_ATIVA);
            var agora=LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"));
            assertThat(indicadoresService.somarLucroVendidoEntre(agora.minusDays(1),agora.plusDays(1))).isEqualByComparingTo("10.12");
        } finally {limparOperacao(u);}
    }
    @Test
    void duasVendasSimultaneasNaoExcedemSaldoDoLote() throws Exception {
        var u=criarUsuario("venda-concorrente-"+UUID.randomUUID()+"@teste.local");autenticarComo(u);var c=criarLoteOperacao(u);
        try {
            producaoService.atualizarProduzido(c.getId(),true);
            var iniciar=new java.util.concurrent.CountDownLatch(1);
            try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
                java.util.concurrent.Callable<ResultadoOperacao> vender=() -> {
                    autenticarComo(u);
                    try {iniciar.await();return vendaService.registrarVenda(c.getId(),2);}
                    finally {SecurityContextHolder.clearContext();}
                };
                var a=pool.submit(vender);var b=pool.submit(vender);iniciar.countDown();
                assertThat(java.util.List.of(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder(ResultadoOperacao.OK,ResultadoOperacao.QUANTIDADE_VENDA_ACIMA_DISPONIVEL);
            }
            assertThat(calculoHistoricoRepository.findById(c.getId()).orElseThrow().getUnidadesVendidas()).isEqualTo(2);
            assertThat(vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(c.getId())).hasSize(1);
        } finally {limparOperacao(u);}
    }
    @Test
    void perdaDeMaterialPodeSerCorrigidaSemAlterarUnidadesBoas() {
        var u=criarUsuario("falha-"+UUID.randomUUID()+"@teste.local");autenticarComo(u);var c=criarLoteOperacao(u);
        try {
            falhaProducaoService.registrar(c.getId(),c.getFilamento().getId(),1,new BigDecimal("5"),10,"Tentativa extra");
            assertThat(filamentoRepository.findById(c.getFilamento().getId()).orElseThrow().getEstoqueGramas()).isEqualByComparingTo("995");
            var falha=falhaProducaoRepository.findByCalculo_IdOrderByDataRegistroDesc(c.getId()).getFirst();
            falhaProducaoService.cancelar(c.getId(),falha.getId());falhaProducaoService.cancelar(c.getId(),falha.getId());
            assertThat(filamentoRepository.findById(c.getFilamento().getId()).orElseThrow().getEstoqueGramas()).isEqualByComparingTo("1000");
            assertThat(calculoHistoricoRepository.findById(c.getId()).orElseThrow().getUnidades()).isEqualTo(3);
        } finally {limparOperacao(u);}
    }
    @Test
    void planejamentoEnfileiraNaMesmaImpressoraSemMovimentarEstoque() {
        var u=criarUsuario("fila-"+UUID.randomUUID()+"@teste.local");autenticarComo(u);var c=criarLoteOperacao(u);
        try {
            var inicio=LocalDateTime.now().withNano(0).plusDays(1);
            var a=planejamentoProducaoService.salvar(c.getId(),3,2,inicio,10,10);
            var b=planejamentoProducaoService.salvar(c.getId(),3,2,inicio,10,10);
            assertThat(b.getInicio()).isEqualTo(a.getFim());
            assertThat(filamentoRepository.findById(c.getFilamento().getId()).orElseThrow().getEstoqueGramas()).isEqualByComparingTo("1000");
            planejamentoProducaoService.cancelar(a.getId());
            assertThat(planejamentoProducaoRepository.findById(b.getId()).orElseThrow().getInicio()).isEqualTo(b.getInicio());
            assertThat(producaoService.atualizarProduzido(c.getId(),true)).isEqualTo(ResultadoOperacao.OK);
            assertThat(vendaService.registrarVenda(c.getId(),3)).isEqualTo(ResultadoOperacao.OK);
            assertThat(planejamentoProducaoRepository.findByCanceladoFalseAndConcluidoFalseOrderByInicioAsc(org.springframework.data.domain.PageRequest.of(0,20))).isEmpty();
            assertThat(planejamentoProducaoRepository.findById(b.getId()).orElseThrow().isConcluido()).isTrue();
        } finally {limparOperacao(u);}
    }
    @Test
    void arquivarCadastroPreservaHistoricoESaiDasOpcoesAtivas() {
        var u=criarUsuario("arquivo-"+UUID.randomUUID()+"@teste.local");autenticarComo(u);var c=criarLoteOperacao(u);
        try {
            var p=pecaRepository.findById(c.getPeca().getId()).orElseThrow();p.setArquivado(true);pecaRepository.saveAndFlush(p);
            assertThat(pecaRepository.findByArquivadoFalse(org.springframework.data.domain.PageRequest.of(0,20))).isEmpty();
            assertThat(pecaRepository.findByArquivadoTrue(org.springframework.data.domain.PageRequest.of(0,20))).hasSize(1);
            assertThat(calculoHistoricoRepository.findById(c.getId())).isPresent();
        } finally {limparOperacao(u);}
    }

    @Autowired private br.com.criare3d.service.AcaoEmLoteService acoesEmLote;
    @Autowired private br.com.criare3d.service.RetencaoClienteService retencao;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;

    @Test void falhaNaSegundaVendaRevertePrimeiraVendaDoLote() {
        var u = criarUsuario("rollback-" + UUID.randomUUID() + "@teste.local");
        autenticarComo(u);
        var a = criarLoteOperacao(u); var b = criarLoteOperacao(u);
        var ordem = java.util.stream.Stream.of(a,b).sorted(java.util.Comparator.comparing(CalculoHistorico::getId)).toList();
        producaoService.atualizarProduzido(ordem.getFirst().getId(), true);
        assertThat(acoesEmLote.registrarVendasEmLote(ordem.stream().map(CalculoHistorico::getId).toList(), 1))
                .isEqualTo(ResultadoOperacao.PRODUCAO_NECESSARIA);
        assertThat(vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(ordem.getFirst().getId())).isEmpty();
        assertThat(calculoHistoricoRepository.findById(ordem.getFirst().getId()).orElseThrow().getUnidadesVendidas()).isZero();
    }

    @Test void expurgoDescobreTodasAsTabelasIncluindoTabelaFuturaSemAfetarOutroCliente() {
        var a = criarUsuario("purga-a-" + UUID.randomUUID() + "@teste.local");
        var b = criarUsuario("purga-b-" + UUID.randomUUID() + "@teste.local");
        autenticarComo(a); criarLoteOperacao(a);
        autenticarComo(b); criarLoteOperacao(b);
        jdbcTemplate.execute("CREATE TABLE teste_retencao_extra (id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id))");
        try {
            jdbcTemplate.update("INSERT INTO teste_retencao_extra VALUES (?, ?), (?, ?)", UUID.randomUUID(), a.getTenant().getId(), UUID.randomUUID(), b.getTenant().getId());
            var tabelas = jdbcTemplate.queryForList("SELECT table_name FROM information_schema.columns WHERE table_schema = current_schema() AND column_name = 'tenant_id'", String.class);
            var contagens = new java.util.HashMap<String, Long>();
            for (String tabela : tabelas) contagens.put(tabela, jdbcTemplate.queryForObject("SELECT count(*) FROM \"" + tabela + "\" WHERE tenant_id = ?", Long.class, b.getTenant().getId()));
            var limite = java.time.Instant.now().minusSeconds(30 * 86400L);
            jdbcTemplate.update("UPDATE tenant SET ativo = false, excluido_em = ? WHERE id = ?", java.sql.Timestamp.from(limite.minusSeconds(1)), a.getTenant().getId());
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status ->
                    org.springframework.test.util.ReflectionTestUtils.invokeMethod(retencao, "excluirExpirado", a.getTenant().getId(), limite));
            assertThat(tenantRepository.existsById(a.getTenant().getId())).isFalse();
            for (String tabela : tabelas) {
                assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"" + tabela + "\" WHERE tenant_id = ?", Long.class, a.getTenant().getId())).isZero();
                assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"" + tabela + "\" WHERE tenant_id = ?", Long.class, b.getTenant().getId())).isEqualTo(contagens.get(tabela));
            }
        } finally { jdbcTemplate.execute("DROP TABLE teste_retencao_extra"); }
    }

    @Autowired private br.com.criare3d.service.RecuperacaoSenhaService recuperacao;
    @Autowired private br.com.criare3d.service.FotoRascunhoService fotosRascunho;

    @Test void recuperacaoExpiraESoPodeSerUsadaUmaVez() throws Exception {
        var u = criarUsuario("recuperacao-" + UUID.randomUUID() + "@teste.local");
        String token = "a".repeat(43);
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        jdbcTemplate.update("INSERT INTO recuperacao_senha VALUES (?, ?, ?)", hash, u.getId(), java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(900)));
        recuperacao.redefinir(token, "NovaSenhaSegura123!");
        assertThat(passwordEncoder.matches("NovaSenhaSegura123!", usuarioRepository.findById(u.getId()).orElseThrow().getPassword())).isTrue();
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException().isThrownBy(() -> recuperacao.redefinir(token, "OutraSenhaSegura123!"));
        jdbcTemplate.update("INSERT INTO recuperacao_senha VALUES (?, ?, ?)", hash, u.getId(), java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)));
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException().isThrownBy(() -> recuperacao.redefinir(token, "OutraSenhaSegura123!"));
        assertThat(passwordEncoder.matches("NovaSenhaSegura123!", usuarioRepository.findById(u.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test void fotoTemporariaIsolaUsuarioExpiraESubstituiRascunhoAnterior() {
        var a = criarUsuario("foto-rascunho-a-" + UUID.randomUUID() + "@teste.local");
        var b = criarUsuario("foto-rascunho-b-" + UUID.randomUUID() + "@teste.local");
        autenticarComo(a);
        UUID primeira = fotosRascunho.salvar("image/png", new byte[]{1,2,3});
        UUID atual = fotosRascunho.salvar("image/png", new byte[]{4,5,6});
        assertThat(fotosRascunho.buscar(primeira)).isEmpty();
        assertThat(fotosRascunho.buscar(atual)).isPresent();
        autenticarComo(b);
        assertThat(fotosRascunho.buscar(atual)).isEmpty();
        fotosRascunho.excluir(atual);
        autenticarComo(a);
        assertThat(fotosRascunho.buscar(atual)).isPresent();
        jdbcTemplate.update("UPDATE foto_rascunho SET atualizado_em = ? WHERE id = ?", java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1801)), atual);
        assertThat(fotosRascunho.buscar(atual)).isEmpty();
    }

	private Usuario criarUsuario(String email) {
		Tenant tenant = criarTenant("teste-" + UUID.randomUUID());
		return usuarioRepository.save(Usuario.builder()
				.email(email).password(passwordEncoder.encode("SenhaTeste123!"))
				.perfil(Perfil.USER).tenant(tenant).build());
	}

	private Tenant criarTenant(String slug) {
		Tenant tenant = new Tenant();
		tenant.setNome("Tenant de teste");
		tenant.setSlug(slug);
		tenant.setAtivo(true);
		tenant.setCorPrimaria("#492e1e");
		tenant.setCorDestaque("#e9a695");
		tenant.setCorFundo("#f5e7da");
		return tenantRepository.saveAndFlush(tenant);
	}

	private void autenticarComo(Usuario usuario) {
		UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
	}

	private Filamento criarFilamento(Usuario usuario) {
		Filamento filamento = new Filamento();
		filamento.setNome("PLA teste");
		filamento.setCor("Preto");
		filamento.setMarca("Marca teste");
		filamento.setPesoRoloGramas(new BigDecimal("1000"));
		filamento.setEstoqueGramas(new BigDecimal("1000"));
		filamento.setPrecoRolo(new BigDecimal("80"));
		filamento.setUsuario(usuario);
		return filamentoRepository.saveAndFlush(filamento);
	}

	private CalculoHistorico criarCalculoHistorico(Usuario usuario) {
		return calculoHistoricoRepository.saveAndFlush(CalculoHistorico.builder()
				.nomePeca("Peça do histórico")
				.nomeFilamento("PLA teste")
				.precoRoloFilamento(new BigDecimal("80.00"))
				.pesoRoloGramas(new BigDecimal("1000.00"))
				.quantidadeFilamentoGramas(new BigDecimal("10.00"))
				.nomeImpressora("Impressora teste")
				.potenciaWatts(new BigDecimal("100.00"))
				.tempoImpressaoMinutos(60L)
				.tarifaEnergiaKwh(new BigDecimal("0.80"))
				.custoFilamento(new BigDecimal("0.80"))
				.custoEnergia(new BigDecimal("0.08"))
				.nomeEmbalagem("Caixa teste")
				.custoEmbalagem(new BigDecimal("2.00"))
				.custoTotal(new BigDecimal("2.88"))
				.markupPercentual(new BigDecimal("100.00"))
				.precoSugerido(new BigDecimal("5.76"))
				.dataCalculo(LocalDateTime.now())
				.usuario(usuario)
				.build());
	}
}
