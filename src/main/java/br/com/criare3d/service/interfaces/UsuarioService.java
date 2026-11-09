
package br.com.criare3d.service.interfaces;

import java.util.UUID;

public interface UsuarioService {

    void alterarSenhaAtual(String senhaAtual, String novaSenha);

    void redefinirSenhaPorAdmin(UUID usuarioId, String senhaTemporaria);
}
