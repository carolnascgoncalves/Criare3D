package br.com.criare3d.dto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class PedidoClienteRequest {
 @NotBlank @Size(max=150) private String titulo;
 @Size(max=150) private String cliente;
 @Size(max=150) private String contato;
 @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max=150) private String email;
 @Size(max=1000) private String observacoes;
}
