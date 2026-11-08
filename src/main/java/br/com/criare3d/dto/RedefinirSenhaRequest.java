package br.com.criare3d.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedefinirSenhaRequest {
    @NotBlank(message = "Informe uma senha temporária.")
    @Size(min = 12, message = "A senha temporária deve possuir pelo menos 12 caracteres.")
    private String senhaTemporaria;

    @NotBlank(message = "Confirme a senha temporária.")
    private String confirmarSenhaTemporaria;
}
