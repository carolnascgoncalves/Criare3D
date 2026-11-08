
package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.dto.UsuarioResumoResponse;

import java.util.List;
import java.util.UUID;

public interface UsuarioService {

    Usuario cadastrarUsuario(String email, String password);

    List<UsuarioResumoResponse> listarUsuarios();

    void alterarSenhaAtual(String senhaAtual, String novaSenha);

    void redefinirSenhaPorAdmin(UUID usuarioId, String senhaTemporaria);
}
