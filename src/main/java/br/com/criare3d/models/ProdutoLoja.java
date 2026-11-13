package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor
public class ProdutoLoja {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
    @Version private Long versao;
    @Column(nullable=false,length=150) private String nome;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal preco;
    @Column(precision=12,scale=2) private BigDecimal custo;
    @Column(nullable=false) private boolean arquivado;
}
