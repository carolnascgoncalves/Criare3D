package br.com.criare3d.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.util.UUID;

/** Valores imutáveis do rateio aplicado no momento do cálculo. */
@Entity @Getter @Setter @NoArgsConstructor
public class RateioCustoHistorico {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @TenantId @Column(nullable=false, updatable=false) private UUID tenantId;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private CalculoHistorico calculo;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private CustoLoja custoLoja;
    @Column(nullable=false, length=150) private String descricao;
    @Column(nullable=false, precision=16, scale=8) private BigDecimal valorUnitario;
}
