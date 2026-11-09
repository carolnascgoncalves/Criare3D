
package br.com.criare3d.service;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.service.interfaces.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
    public void alterarSenhaAtual(String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(usuarioLogadoService.buscarIdUsuarioAtual())
                .orElseThrow(() -> new IllegalStateException("Não foi possível localizar a conta autenticada."));
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
            throw new IllegalArgumentException("A senha da conta administradora não pode ser redefinida por esta operação.");
        }
        usuario.setPassword(passwordEncoder.encode(senhaTemporaria));
        usuario.setTrocaSenhaObrigatoria(true);
        usuarioRepository.save(usuario);
    }

}
