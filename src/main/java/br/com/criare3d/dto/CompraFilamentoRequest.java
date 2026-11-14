package br.com.criare3d.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CompraFilamentoRequest {
    @NotNull @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(10000)
    private Integer unidadesCompra = 1;
    public BigDecimal getQuantidadeTotalGramas() {
        return gramasCompradas!=null?gramasCompradas:pesoRoloGramas.multiply(BigDecimal.valueOf(unidadesCompra==null?1:unidadesCompra));
    }

    @DecimalMin(value = "0.01", message = "A quantidade comprada deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2, message = "Informe no máximo 8 números inteiros e 2 casas decimais.")
    private BigDecimal gramasCompradas;

    @NotNull(message = "Informe o peso do rolo comprado.")
    @DecimalMin(value = "0.01", message = "O peso do rolo deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2, message = "Informe no máximo 8 números inteiros e 2 casas decimais.")
    private BigDecimal pesoRoloGramas;

    @NotNull(message = "Informe o preço pago pelo rolo.")
    @DecimalMin(value = "0.01", message = "O preço do rolo deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2, message = "Informe no máximo 8 números inteiros e 2 casas decimais.")
    private BigDecimal precoRolo;
}
