package br.com.criare3d.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecuperarSenhaRequest {
    @NotBlank
    @Email
    @Size(max = 150)
    private String email;
}
