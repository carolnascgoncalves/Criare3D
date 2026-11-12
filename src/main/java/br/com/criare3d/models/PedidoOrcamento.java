package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import br.com.criare3d.models.enums.StatusPedido;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
@Entity @Table(name="pedido_orcamento") @Getter @Setter @NoArgsConstructor
public class PedidoOrcamento {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
 @Version private long versao;
 @Column(nullable=false) private boolean arquivado;
 @Column(nullable=false) private long revisao;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StatusPedido status=StatusPedido.RASCUNHO;
 @Column(nullable=false,length=150) private String titulo="Novo orçamento";
 @Column(length=150) private String cliente;
 @Column(length=150) private String contato;
 @Column(length=1000) private String observacoes;
 @Column(nullable=false) private LocalDateTime criadoEm;
 private LocalDateTime enviadoEm;
 private LocalDateTime aprovadoEm;
 private LocalDateTime entregueEm;
 private LocalDateTime canceladoEm;
 private LocalDateTime pagoEm;
 private LocalDateTime inicioPretendido;
 private LocalDateTime previsao;
 @Column(nullable=false) private LocalTime turnoInicio=LocalTime.of(8,0);
 @Column(nullable=false) private LocalTime turnoFim=LocalTime.of(18,0);
 @Column(nullable=false,length=13) private String dias="1,2,3,4,5";
 @Column(nullable=false) private int intervalo=10;
 @Column(nullable=false) private int folga=10;
 @Column(precision=5,scale=2) private BigDecimal descontoPercentual;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal desconto=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal frete=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal custoFrete=BigDecimal.ZERO;
 @OneToMany(mappedBy="pedido",cascade=CascadeType.ALL,orphanRemoval=true)
 @OrderBy("ordem ASC") private List<ItemPedido> itens=new ArrayList<>();
 public String getCodigo() { return id==null?"Novo":id.toString().substring(0,8).toUpperCase(Locale.ROOT); }
 public BigDecimal getSubtotal() { return itens.stream().map(ItemPedido::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add); }
 public BigDecimal getDesconto(){return descontoPercentual==null?desconto:getSubtotal().multiply(descontoPercentual).divide(new BigDecimal("100"),2,java.math.RoundingMode.HALF_UP);}
 public BigDecimal getTotal() { return getSubtotal().subtract(getDesconto()).add(frete); }
 public BigDecimal getCusto() { return itens.stream().map(ItemPedido::getCusto).reduce(custoFrete,BigDecimal::add); }
 public BigDecimal getTaxas() { return itens.stream().map(i -> i.getTaxasUnitarias().multiply(BigDecimal.valueOf(i.getQuantidade()))).reduce(BigDecimal.ZERO,BigDecimal::add); }
 public BigDecimal getLucro() { return getTotal().subtract(getCusto()).subtract(getTaxas()); }
 public br.com.criare3d.dto.RentabilidadePedido getRentabilidade(){return br.com.criare3d.dto.RentabilidadePedido.avaliar(getTotal(),getCusto(),getTaxas());}
 public int getQuantidadeProduzir() { return itens.stream().mapToInt(ItemPedido::getProduzir).sum(); }
 public String getSituacao() { return status==StatusPedido.APROVADO && itens.stream().allMatch(ItemPedido::isPronto)?StatusPedido.PRONTO.getDescricao():status.getDescricao(); }
 public boolean isOperacional() { return status==StatusPedido.APROVADO || status==StatusPedido.PRONTO; }
}
