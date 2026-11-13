package br.com.criare3d.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Getter @Setter @NoArgsConstructor
public class CustoLoja {
    public enum Tipo { VALOR_UNIDADE, PERCENTUAL, RATEIO_MENSAL }
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @TenantId @Column(nullable=false, updatable=false) private UUID tenantId;
    @Column(nullable=false, length=150) private String descricao;
    @Column(nullable=false) private LocalDate inicio;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal meta;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private Tipo tipo;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal valor;
    private Integer unidadesPrevistas;
    @Column(nullable=false) private boolean ativo = true;
}
