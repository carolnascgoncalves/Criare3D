
package br.com.criare3d.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    private String email;

    @NotBlank(message = "Informe a senha.")
    @Size(min = 12, message = "A senha deve possuir pelo menos 12 caracteres.")
    private String password;

    @NotBlank(message = "Confirme a senha.")
    private String confirmarPassword;
}
