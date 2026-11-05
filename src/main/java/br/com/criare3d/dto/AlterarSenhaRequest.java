package br.com.criare3d.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlterarSenhaRequest {

    @NotBlank(message = "Informe sua senha atual.")
    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 12, message = "A nova senha deve possuir pelo menos 12 caracteres.")
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmarNovaSenha;
}
