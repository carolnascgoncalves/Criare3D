package br.com.criare3d.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
@Getter @Setter
public class PedidoDadosRequest {
 @NotBlank @Size(max=150) private String titulo;
 @Size(max=150) private String cliente;
 @Size(max=150) private String contato;
 @Size(max=1000) private String observacoes;
 @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime inicioPretendido;
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) private LocalTime turnoInicio=LocalTime.of(8,0);
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) private LocalTime turnoFim=LocalTime.of(18,0);
 @NotBlank @Pattern(regexp="[1-7](,[1-7]){0,6}") private String dias="1,2,3,4,5";
 @Min(0) @Max(1440) private int intervalo=10;
 @Min(0) @Max(100) private int folga=10;
 @Size(max=30) @Pattern(regexp="[0-9]{1,8}([.,][0-9]{1,2})?\\s*%?",message="Informe reais (15) ou porcentagem (15%).") private String descontoInformado;
 @NotNull @DecimalMin("0") @DecimalMax("99999999") @Digits(integer=8,fraction=2) private BigDecimal desconto=BigDecimal.ZERO;
 @NotNull @DecimalMin("0") @DecimalMax("99999999") @Digits(integer=8,fraction=2) private BigDecimal frete=BigDecimal.ZERO;
 @NotNull @DecimalMin("0") @DecimalMax("99999999") @Digits(integer=8,fraction=2) private BigDecimal custoFrete=BigDecimal.ZERO;
}
