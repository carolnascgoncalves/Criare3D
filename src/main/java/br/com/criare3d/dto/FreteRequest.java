package br.com.criare3d.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FreteRequest {
    @NotBlank
    @Pattern(regexp = "[0-9]{5}-?[0-9]{3}")
    private String origem;
    @NotBlank
    @Pattern(regexp = "[0-9]{5}-?[0-9]{3}")
    private String destino;
    @NotNull
    @DecimalMin("0.001")
    @DecimalMax("1000")
    private BigDecimal peso;
    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("1000")
    private BigDecimal altura;
    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("1000")
    private BigDecimal largura;
    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("1000")
    private BigDecimal comprimento;
}
