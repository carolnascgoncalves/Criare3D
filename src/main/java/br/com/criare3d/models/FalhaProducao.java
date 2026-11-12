package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor
public class FalhaProducao {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private CalculoHistorico calculo;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Filamento filamento;
 private int unidades;
 @Column(precision=12,scale=6) private BigDecimal gramas;
 private long minutos;
 @Column(precision=16,scale=8) private BigDecimal custoMaterial;
 @Column(length=500) private String observacao;
 private LocalDateTime dataRegistro;
 private boolean cancelada;
}
