package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor
public class PlanejamentoProducao {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private CalculoHistorico calculo;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Impressora impressora;
 @Column(length=150) private String nomePeca;
 @Column(length=150) private String nomeImpressora;
 private int unidades;
 private int pecasPorImpressao;
 private int intervalo;
 private int folga;
 private LocalDateTime inicio;
 private LocalDateTime fim;
 private boolean cancelado;
 private boolean concluido;
}
