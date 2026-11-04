
package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Usuario;

import java.util.List;

public interface UsuarioService {

    Usuario cadastrarUsuario(String email, String password);

    List<Usuario> listarUsuarios();
}
