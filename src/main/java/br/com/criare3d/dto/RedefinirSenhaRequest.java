package br.com.criare3d.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedefinirSenhaRequest {
    @NotBlank(message = "Informe uma senha temporária.")
    @br.com.criare3d.security.SenhaBcrypt
    @Size(min = 12, max = 72, message = "Use uma senha de 12 a 72 caracteres.")
    private String senhaTemporaria;

    @NotBlank(message = "Confirme a senha temporária.")
    private String confirmarSenhaTemporaria;
}
