package br.com.criare3d.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteAdminRequest {

    @NotBlank(message = "Informe o nome do cliente.")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe um identificador para o endereço.")
    @Size(max = 80, message = "O identificador deve ter até 80 caracteres.")
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use letras minúsculas, números e hífens, sem espaços.")
    private String slug;

    @NotBlank(message = "Informe o e-mail de acesso.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    private String email;

    @NotBlank(message = "Escolha a cor principal.")
    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Informe uma cor hexadecimal válida.")
    private String corPrimaria = "#492e1e";

    @NotBlank(message = "Escolha a cor de destaque.")
    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Informe uma cor hexadecimal válida.")
    private String corDestaque = "#e9a695";

    @NotBlank(message = "Escolha a cor de fundo.")
    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Informe uma cor hexadecimal válida.")
    private String corFundo = "#f5e7da";

    private boolean ativo = true;
}
