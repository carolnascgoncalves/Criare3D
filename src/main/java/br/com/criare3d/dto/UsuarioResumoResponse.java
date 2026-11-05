package br.com.criare3d.dto;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioResumoResponse {

    private String email;
    private Perfil perfil;

    public static UsuarioResumoResponse de(Usuario usuario) {
        return new UsuarioResumoResponse(usuario.getEmail(), usuario.getPerfil());
    }
}
