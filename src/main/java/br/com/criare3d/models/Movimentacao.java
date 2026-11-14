package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor
public class Movimentacao {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
 @Column(nullable=false,length=40) private String tipo;
 @Column(nullable=false,length=180) private String descricao;
 @Column(nullable=false,precision=18,scale=6) private BigDecimal quantidade;
 @Column(nullable=false,length=8) private String medida;
 private UUID referencia;
 @Column(nullable=false,length=30) private String origem;
 @Column(nullable=false) private LocalDateTime data;
 private UUID usuarioId;
}
