package br.com.criare3d.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Getter @Setter @NoArgsConstructor
public class CustoPersonalizado {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @TenantId @Column(nullable = false, updatable = false) private UUID tenantId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private CalculoHistorico calculo;
    @Column(nullable = false, length = 150) private String descricao;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal valor;
    @Column(nullable = false) private LocalDateTime dataRegistro;
    @Column(nullable = false) private boolean cancelado;
}
