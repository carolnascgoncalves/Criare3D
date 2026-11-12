package br.com.criare3d.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SenhaRecuperadaRequest {
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") private String token;
    @NotBlank @Size(min = 12, max = 72) @br.com.criare3d.security.SenhaBcrypt private String senha;
    @NotBlank @Size(max = 72) private String confirmarSenha;
}
