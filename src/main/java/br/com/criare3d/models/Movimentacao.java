package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import br.com.criare3d.models.enums.TipoMovimentacao;
import br.com.criare3d.util.FormatoQuantidade;
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
 private UUID filamentoId;
 @Column(precision=18,scale=6) private BigDecimal saldoApos;

 public TipoMovimentacao getTipoInfo() { return TipoMovimentacao.de(tipo); }
 /** "entrada", "saida" ou "neutro" (saldo de abertura): define a cor na tela. */
 public String getDirecao() {
  if (TipoMovimentacao.SALDO_INICIAL.name().equals(tipo) || quantidade == null) return "neutro";
  return quantidade.signum() >= 0 ? "entrada" : "saida";
 }
 public String getQuantidadeTexto() { return FormatoQuantidade.resumido(quantidade, medida, true); }
 public String getQuantidadeExata() { return FormatoQuantidade.exato(quantidade, medida); }
 public String getLinkOrigem() {
  if (referencia == null) return null;
  return switch (origem == null ? "" : origem) {
   case "filamento" -> "/filamento/detalhes/" + referencia;
   case "lote" -> "/vendas-estoque/lote/" + referencia;
   case "produto" -> "/outras-vendas";
   default -> null;
  };
 }
}
