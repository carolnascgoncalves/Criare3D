package br.com.criare3d;

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
import br.com.criare3d.dto.CompraFilamentoRequest;
import br.com.criare3d.dto.DashboardResumo;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.ConfiguracaoRepository;
import br.com.criare3d.repository.EmbalagemRepository;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.repository.ImpressoraRepository;
import br.com.criare3d.repository.PecaRepository;
import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.service.FilamentoServiceImp;
import br.com.criare3d.service.interfaces.UsuarioService;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
	private FilamentoServiceImp filamentoService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private CalculoHistoricoService calculoHistoricoService;

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
	void usuarioNaoConsegueBuscarRegistrosDeOutraContaNasSeisTabelas() {
		Usuario usuarioA = criarUsuario("a-" + UUID.randomUUID() + "@teste.local");
		Usuario usuarioB = criarUsuario("b-" + UUID.randomUUID() + "@teste.local");
		Filamento filamentoA = criarFilamento(usuarioA);
		Peca pecaA = pecaRepository.saveAndFlush(Peca.builder()
				.nome("Peça teste").quantidadeFilamentoGramas(new BigDecimal("10"))
				.tempoImpressaoMinutos(60L).usuario(usuarioA).build());
		Impressora impressoraA = impressoraRepository.saveAndFlush(Impressora.builder()
				.nome("Impressora teste").modelo("Modelo teste")
				.potenciaWatts(new BigDecimal("100")).usuario(usuarioA).build());
		Embalagem embalagemA = embalagemRepository.saveAndFlush(Embalagem.builder()
				.nome("Caixa teste").custoUnitario(new BigDecimal("2.00")).usuario(usuarioA).build());
		Configuracao configuracaoA = configuracaoRepository.saveAndFlush(Configuracao.builder()
				.markupPercentual(new BigDecimal("100")).tarifaEnergiaKwh(new BigDecimal("0.80"))
				.embalagemFixa(false).usuario(usuarioA).build());
		CalculoHistorico calculoA = criarCalculoHistorico(usuarioA);

		try {
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(usuarioB.getEmail(), "", java.util.List.of()));
			assertThat(filamentoService.fetchById(filamentoA.getId())).isEmpty();
			assertThat(pecaRepository.findByIdAndUsuario_Id(pecaA.getId(), usuarioB.getId())).isEmpty();
			assertThat(impressoraRepository.findByIdAndUsuario_Id(impressoraA.getId(), usuarioB.getId())).isEmpty();
			assertThat(embalagemRepository.findByIdAndUsuario_Id(embalagemA.getId(), usuarioB.getId())).isEmpty();
			assertThat(configuracaoRepository.findByIdAndUsuario_Id(configuracaoA.getId(), usuarioB.getId())).isEmpty();
			assertThat(calculoHistoricoRepository.findByIdAndUsuario_Id(calculoA.getId(), usuarioB.getId())).isEmpty();

			assertThat(pecaRepository.findByIdAndUsuario_Id(pecaA.getId(), usuarioA.getId())).isPresent();
			assertThat(impressoraRepository.findByIdAndUsuario_Id(impressoraA.getId(), usuarioA.getId())).isPresent();
			assertThat(embalagemRepository.findByIdAndUsuario_Id(embalagemA.getId(), usuarioA.getId())).isPresent();
			assertThat(configuracaoRepository.findByIdAndUsuario_Id(configuracaoA.getId(), usuarioA.getId())).isPresent();
			assertThat(calculoHistoricoRepository.findByIdAndUsuario_Id(calculoA.getId(), usuarioA.getId())).isPresent();
		} finally {
			SecurityContextHolder.clearContext();
			calculoHistoricoRepository.deleteById(calculoA.getId());
			configuracaoRepository.deleteById(configuracaoA.getId());
			embalagemRepository.deleteById(embalagemA.getId());
			impressoraRepository.deleteById(impressoraA.getId());
			pecaRepository.deleteById(pecaA.getId());
			filamentoRepository.deleteById(filamentoA.getId());
			usuarioRepository.deleteAllById(java.util.List.of(usuarioA.getId(), usuarioB.getId()));
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

			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(usuario.getEmail(), "", java.util.List.of()));
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
		LocalDateTime agora = LocalDateTime.now().withNano(0);
		LocalDateTime dataVenda = agora.minusDays(2);
		CalculoHistorico calculo = criarCalculoHistorico(usuario);
		calculo.setDataCalculo(dataVenda.minusMonths(6));
		calculo.setDataVenda(dataVenda);
		calculo.setVendido(true);
		calculo.setUnidades(3);
		calculo.setLucroLiquido(new BigDecimal("25.00"));
		calculoHistoricoRepository.saveAndFlush(calculo);

		try {
		DashboardResumo resumoVenda = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
					usuario.getId(), dataVenda.minusDays(1), agora.plusDays(1)));
			assertThat(resumoVenda.vendidas()).isEqualTo(3);
			assertThat(resumoVenda.lucroVendido()).isEqualByComparingTo("75.00");
			assertThat(resumoVenda.minutosVendidos()).isEqualTo(180L);
			DashboardResumo resumoDataCalculo = DashboardResumo.from(calculoHistoricoRepository.resumirDashboard(
					usuario.getId(), dataVenda.minusMonths(6).minusDays(1), dataVenda.minusMonths(6).plusDays(1)));
			assertThat(resumoDataCalculo.lucroVendido()).isEqualByComparingTo("0.00");
			assertThat(calculoHistoricoRepository.primeiraVendaComLucro(usuario.getId()))
					.isEqualTo(dataVenda);
		} finally {
			calculoHistoricoRepository.deleteById(calculo.getId());
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	@Test
	void compraDeFilamentoSomaEstoqueEAtualizaPrecoDeReferencia() {
		Usuario usuario = criarUsuario("compra-" + UUID.randomUUID() + "@teste.local");
		Filamento filamento = criarFilamento(usuario);
		try {
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(usuario.getEmail(), "", java.util.List.of()));
			CompraFilamentoRequest compra = new CompraFilamentoRequest();
			compra.setGramasCompradas(new BigDecimal("1000"));
			compra.setPesoRoloGramas(new BigDecimal("1000"));
			compra.setPrecoRolo(new BigDecimal("95.50"));

			assertThat(filamentoService.registrarCompra(filamento.getId(), compra)).isTrue();
			Filamento atualizado = filamentoService.fetchById(filamento.getId()).orElseThrow();
			assertThat(atualizado.getEstoqueGramas()).isEqualByComparingTo("2000.00");
			assertThat(atualizado.getPrecoRolo()).isEqualByComparingTo("95.50");
		} finally {
			SecurityContextHolder.clearContext();
			filamentoRepository.deleteById(filamento.getId());
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	@Test
	void produzirLoteDescontaEDevolverLoteRestauraEstoqueMultiplicadoPelasUnidades() {
		Usuario usuario = criarUsuario("lote-" + UUID.randomUUID() + "@teste.local");
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
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(usuario.getEmail(), "", java.util.List.of()));
			assertThat(calculoHistoricoService.atualizarProduzido(calculo.getId(), true)).isEqualTo("ok");
			assertThat(filamentoService.fetchById(filamento.getId()).orElseThrow().getEstoqueGramas())
					.isEqualByComparingTo("850.00");

			assertThat(calculoHistoricoService.atualizarProduzido(calculo.getId(), false)).isEqualTo("ok");
			assertThat(filamentoService.fetchById(filamento.getId()).orElseThrow().getEstoqueGramas())
					.isEqualByComparingTo("1000.00");
		} finally {
			SecurityContextHolder.clearContext();
			calculoHistoricoRepository.deleteById(calculo.getId());
			filamentoRepository.deleteById(filamento.getId());
			usuarioRepository.deleteById(usuario.getId());
		}
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.save(Usuario.builder()
				.email(email).password(passwordEncoder.encode("SenhaTeste123!"))
				.perfil(Perfil.USER).build());
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
