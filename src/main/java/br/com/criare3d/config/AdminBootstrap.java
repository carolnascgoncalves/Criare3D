
package br.com.criare3d.config;

import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public AdminBootstrap(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {

        String email = environment.getProperty("ADMIN_EMAIL");
        String password = environment.getProperty("ADMIN_PASSWORD");

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "Configure ADMIN_EMAIL e ADMIN_PASSWORD nas variáveis de ambiente."
            );
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        if (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("ADMIN_EMAIL precisa ser um e-mail válido.");
        }

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "A senha do ADMIN deve possuir pelo menos 12 caracteres."
            );
        }

        var usuarioExistente = usuarioRepository.findByEmail(email);

        if (usuarioExistente.isPresent()) {
            Usuario adminExistente = usuarioExistente.get();
            if (adminExistente.getPerfil() != Perfil.ADMIN) {
                throw new IllegalStateException(
                        "O e-mail configurado em ADMIN_EMAIL já existe e não é ADMIN."
                );
            }

            boolean alterouAdmin = false;
            if (Boolean.parseBoolean(environment.getProperty("ADMIN_REDEFINIR_SENHA", "false"))) {
                adminExistente.setPassword(passwordEncoder.encode(password));
                adminExistente.setTrocaSenhaObrigatoria(false);
                alterouAdmin = true;
                LOG.warn("Senha do ADMIN redefinida por configuração explícita. Remova ADMIN_REDEFINIR_SENHA do ambiente após a recuperação.");
            }

            if (adminExistente.getTenant() != null) {
                adminExistente.setTenant(null);
                alterouAdmin = true;
            }

            if (alterouAdmin) usuarioRepository.save(adminExistente);
            return;
        }

        if (usuarioRepository.existsByPerfil(Perfil.ADMIN)) {
            throw new IllegalStateException(
                    "Já existe uma conta ADMIN cadastrada com outro e-mail."
            );
        }

        Usuario admin = Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .perfil(Perfil.ADMIN)
                .build();

        usuarioRepository.save(admin);
    }
}
