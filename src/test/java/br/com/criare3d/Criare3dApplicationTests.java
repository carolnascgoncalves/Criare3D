package br.com.criare3d;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import br.com.criare3d.service.interfaces.UsuarioService;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;

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
	@Autowired
	private CalculoHistoricoService calculoHistoricoService;

	@AfterEach
	void removerTenantsDeTeste() {
		tenantRepository.findAll().stream()
				.filter(tenant -> tenant.getSlug() != null && tenant.getSlug().startsWith("teste-"))
				.forEach(tenantRepository::delete);
	}

	@Test
	void contextoDaAplicacaoIniciaComBancoDeTeste() {
		assertThat(passwordEncoder).isNotNull();
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

		try {
		DashboardResumo resumoVenda = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
				usuario.getId(), usuario.getTenant().getId(), dataVenda.minusDays(1), agora.plusDays(1)));
			assertThat(resumoVenda.vendidas()).isEqualTo(3);
			assertThat(resumoVenda.lucroVendido()).isEqualByComparingTo("75.00");
			assertThat(resumoVenda.minutosVendidos()).isEqualTo(180L);
			DashboardResumo resumoDataCalculo = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
					usuario.getId(), usuario.getTenant().getId(), dataVenda.minusMonths(6).minusDays(1), dataVenda.minusMonths(6).plusDays(1)));
			assertThat(resumoDataCalculo.lucroVendido()).isEqualByComparingTo("0.00");
			assertThat(calculoHistoricoRepository.primeiraVendaComLucro(usuario.getId()))
					.isEqualTo(dataVenda);
			DashboardResumo semTenant = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
					usuario.getId(), new UUID(0L, 0L), dataVenda.minusDays(1), agora.plusDays(1)));
			assertThat(semTenant.vendidas()).isZero();
		} finally {
			calculoHistoricoRepository.deleteById(calculo.getId());
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	@Test
	void compraDeFilamentoSomaEstoqueEAtualizaPrecoDeReferencia() {
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
			assertThat(atualizado.getPrecoRolo()).isEqualByComparingTo("95.50");
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
			assertThat(calculoHistoricoService.atualizarProduzido(calculo.getId(), true)).isEqualTo("ok");
			assertThat(filamentoService.fetchById(filamento.getId()).orElseThrow().getEstoqueGramas())
					.isEqualByComparingTo("850.00");

			assertThat(calculoHistoricoService.atualizarProduzido(calculo.getId(), false)).isEqualTo("ok");
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

			CalculoHistorico resultado = calculoHistoricoService.calcular(request);

			assertThat(resultado.getUnidades()).isEqualTo(10);
			assertThat(resultado.getPecasPorImpressao()).isEqualTo(4);
			assertThat(resultado.getFornadas()).isEqualTo(3);
			assertThat(resultado.getTempoAquecimentoMinutos()).isEqualTo(5);
			assertThat(resultado.getCustoEnergia()).isEqualByComparingTo("0.09800000");
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
		UsuarioPrincipal principal = new UsuarioPrincipal(usuario, true);
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
