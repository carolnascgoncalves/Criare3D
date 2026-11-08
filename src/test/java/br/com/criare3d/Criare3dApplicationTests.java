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
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.service.FilamentoServiceImp;
import br.com.criare3d.service.interfaces.UsuarioService;

import java.math.BigDecimal;
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
	private FilamentoServiceImp filamentoService;
	@Autowired
	private UsuarioService usuarioService;

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
	void usuarioNaoConsegueBuscarFilamentoDeOutraConta() {
		Usuario usuarioA = criarUsuario("a-" + UUID.randomUUID() + "@teste.local");
		Usuario usuarioB = criarUsuario("b-" + UUID.randomUUID() + "@teste.local");
		Filamento filamentoA = criarFilamento(usuarioA);

		try {
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(usuarioB.getEmail(), "", java.util.List.of()));
			assertThat(filamentoService.fetchById(filamentoA.getId())).isEmpty();
		} finally {
			SecurityContextHolder.clearContext();
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
}
