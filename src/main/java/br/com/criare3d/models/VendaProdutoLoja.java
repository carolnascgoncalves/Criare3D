package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor
public class VendaProdutoLoja {
    public enum TipoEmbalagem { SEM, CADASTRADA, PERSONALIZADA }
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
    @Version private Long versao;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private ProdutoLoja produto;
    @Column(nullable=false,length=150) private String nomeProduto;
    @Column(nullable=false) private int quantidade;
    @Column(nullable=false) private int canceladas;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal precoUnitario;
    @Column(precision=12,scale=2) private BigDecimal custoUnitario;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TipoEmbalagem tipoEmbalagem=TipoEmbalagem.SEM;
    private UUID embalagemId;
    private Integer quantidadeEmbalagens;
    @Column(length=150) private String nomeEmbalagem;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal custoEmbalagem=BigDecimal.ZERO;
    @Column(nullable=false) private LocalDateTime dataVenda;
    private LocalDateTime dataCancelamento;
    public int getAtivas() { return quantidade-canceladas; }
    public BigDecimal getReceita() { return precoUnitario.multiply(BigDecimal.valueOf(getAtivas())); }
    public int getEmbalagensUsadas() { return quantidadeEmbalagens==null ? (tipoEmbalagem==TipoEmbalagem.SEM ? 0 : quantidade) : quantidadeEmbalagens; }
    public BigDecimal getCustoEmbalagemTotal() { return custoEmbalagem.multiply(BigDecimal.valueOf(getEmbalagensUsadas())); }
    public BigDecimal getCustoEmbalagemAtivas() { return getCustoEmbalagemTotal().multiply(BigDecimal.valueOf(getAtivas())).divide(BigDecimal.valueOf(quantidade),2,java.math.RoundingMode.HALF_UP); }
    public BigDecimal getLucro() { return custoUnitario==null ? null : precoUnitario.subtract(custoUnitario).multiply(BigDecimal.valueOf(getAtivas())).subtract(getCustoEmbalagemAtivas()); }
}
