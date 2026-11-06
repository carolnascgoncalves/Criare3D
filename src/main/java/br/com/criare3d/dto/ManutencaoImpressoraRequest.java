package br.com.criare3d.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ManutencaoImpressoraRequest {

    @NotBlank(message = "Informe o que foi feito na manutenção.")
    @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres.")
    private String descricao;

    @NotNull(message = "Informe o valor gasto.")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
    private BigDecimal valor;

    @NotNull(message = "Informe a data da manutenção.")
    private LocalDate data = LocalDate.now();
}
