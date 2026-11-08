
package br.com.criare3d.service;

import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.dto.UsuarioResumoResponse;
import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.service.UsuarioLogadoService;
import br.com.criare3d.service.interfaces.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UsuarioServiceImp implements UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogadoService usuarioLogadoService;

    public UsuarioServiceImp(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                             UsuarioLogadoService usuarioLogadoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    @Override
    public Usuario cadastrarUsuario(String email, String password) {

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        Usuario usuario = Usuario.builder()
                .email(emailNormalizado)
                .password(passwordEncoder.encode(password))
                .perfil(Perfil.USER)
                .trocaSenhaObrigatoria(true)
                .build();

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResumoResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResumoResponse::de)
                .toList();
    }

    @Override
    public void alterarSenhaAtual(String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioLogadoService.buscarUsuarioAtual();
        if (!passwordEncoder.matches(senhaAtual, usuario.getPassword())) {
            throw new IllegalArgumentException("A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(novaSenha, usuario.getPassword())) {
            throw new IllegalArgumentException("Escolha uma senha diferente da atual.");
        }
        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuario.setTrocaSenhaObrigatoria(false);
        usuarioRepository.save(usuario);
    }

    @Override
    public void redefinirSenhaPorAdmin(java.util.UUID usuarioId, String senhaTemporaria) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (usuario.getPerfil() != Perfil.USER) {
            throw new IllegalArgumentException("A senha da conta administradora não pode ser redefinida por esta tela.");
        }
        usuario.setPassword(passwordEncoder.encode(senhaTemporaria));
        usuario.setTrocaSenhaObrigatoria(true);
        usuarioRepository.save(usuario);
    }
}
