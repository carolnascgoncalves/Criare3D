package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="item_pedido") @Getter @Setter @NoArgsConstructor
public class ItemPedido {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private PedidoOrcamento pedido;
 @Column(nullable=false) private int ordem;
 @Column(nullable=false,length=150) private String nome;
 @Column(length=300) private String materiais;
 private UUID pecaId;
 private UUID impressoraId;
 @Column(nullable=false,columnDefinition="text") private String entradaJson;
 @Column(nullable=false) private int quantidade;
 @Column(nullable=false) private int usarEstoque;
 @Column(nullable=false) private int reservaEstoque;
 @Column(nullable=false) private int reservaProducao;
 @ManyToOne(fetch=FetchType.LAZY) private CalculoHistorico loteEstoque;
 @ManyToOne(fetch=FetchType.LAZY) private CalculoHistorico loteProducao;
 @ManyToOne(fetch=FetchType.LAZY) private PlanejamentoProducao planejamento;
 @Column(nullable=false,precision=16,scale=8) private BigDecimal precoUnitario;
 @Column(nullable=false,precision=16,scale=8) private BigDecimal taxasUnitarias=BigDecimal.ZERO;
 @Column(nullable=false,precision=20,scale=8) private BigDecimal custo;
 @Column(nullable=false) private long minutosProducao;
 @Column(nullable=false) private int aquecimento;
 public int getProduzir() { return quantidade-usarEstoque; }
 public BigDecimal getTotal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
 public boolean isPronto() { return getProduzir()==0 || loteProducao!=null && Boolean.TRUE.equals(loteProducao.getProduzido()); }
}
