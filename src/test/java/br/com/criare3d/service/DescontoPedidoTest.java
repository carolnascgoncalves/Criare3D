package br.com.criare3d.service;
import br.com.criare3d.models.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class DescontoPedidoTest {
 @Test void distingueReaisDePorcentagemEArredondaCentavos(){
  assertThat(DescontoPedido.interpretar("15",new BigDecimal("200")).valor()).isEqualByComparingTo("15.00");
  assertThat(DescontoPedido.interpretar("15%",new BigDecimal("200")).valor()).isEqualByComparingTo("30.00");
  assertThat(DescontoPedido.interpretar("12,5%",new BigDecimal("19.99")).valor()).isEqualByComparingTo("2.50");
 }
 @Test void impedeDescontosInvalidos(){for(String texto:new String[]{"-15","101%","abc","15%%","15.001","201"})assertThatIllegalArgumentException().isThrownBy(()->DescontoPedido.interpretar(texto,new BigDecimal("200")));}
 @Test void percentualAcompanhaQuantidadeENaoSeAplicaAoFrete(){
  var p=new PedidoOrcamento();var i=new ItemPedido();i.setQuantidade(2);i.setPrecoUnitario(new BigDecimal("100"));p.getItens().add(i);p.setDescontoPercentual(new BigDecimal("15"));p.setFrete(new BigDecimal("20"));
  assertThat(p.getTotal()).isEqualByComparingTo("190");i.setQuantidade(3);assertThat(p.getDesconto()).isEqualByComparingTo("45");assertThat(p.getTotal()).isEqualByComparingTo("275");
 }
}
