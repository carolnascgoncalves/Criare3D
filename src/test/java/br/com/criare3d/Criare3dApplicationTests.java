package br.com.criare3d;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class Criare3dApplicationTests {

	@Autowired
	private PasswordEncoder passwordEncoder;

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
}
