package br.com.criare3d.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ImpressoraCadastroRapidoRequest(
    @NotBlank(message = "Informe o nome da impressora.") @Size(min = 2, max = 150) String nome,
    @NotBlank(message = "Selecione ou informe o modelo.") @Size(min = 2, max = 150) String modelo,
    @DecimalMin(value = "0.0", message = "A potência não pode ser negativa.") BigDecimal potenciaWatts,
    @DecimalMin(value = "0.0", message = "As horas estimadas não podem ser negativas.") BigDecimal vidaUtilHoras,
    @DecimalMin(value = "0.0", message = "As horas mensais não podem ser negativas.") BigDecimal horasMensaisUso,
    Boolean horasMensaisManual) {
}
