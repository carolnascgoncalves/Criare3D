package br.com.criare3d.service;
import br.com.criare3d.dto.RentabilidadePedido;
import br.com.criare3d.models.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RentabilidadePedidoTest {
 @Test void custoETaxasPodemGerarPrejuizoMesmoComPrecoMaiorQueMaterial(){var r=RentabilidadePedido.avaliar(new BigDecimal("214"),new BigDecimal("224"),BigDecimal.TEN);assertThat(r.estado()).isEqualTo("prejuizo");assertThat(r.lucro()).isEqualByComparingTo("-20");}
 @Test void usaMargemLiquidaEComparaLimiteSemArredondarAntes(){assertThat(RentabilidadePedido.avaliar(new BigDecimal("100"),new BigDecimal("85.01"),BigDecimal.ZERO).estado()).isEqualTo("baixa");assertThat(RentabilidadePedido.avaliar(new BigDecimal("100"),new BigDecimal("85"),BigDecimal.ZERO).estado()).isEqualTo("boa");assertThat(RentabilidadePedido.avaliar(new BigDecimal("100"),new BigDecimal("100"),BigDecimal.ZERO).estado()).isEqualTo("prejuizo");}
 @Test void incluiDescontoFreteCustoDeEnvioETaxasDoPedido(){var p=new PedidoOrcamento();var i=new ItemPedido();i.setQuantidade(2);i.setPrecoUnitario(new BigDecimal("100"));i.setCusto(new BigDecimal("150"));i.setTaxasUnitarias(new BigDecimal("10"));p.getItens().add(i);p.setDesconto(new BigDecimal("20"));p.setFrete(BigDecimal.TEN);p.setCustoFrete(new BigDecimal("30"));assertThat(p.getRentabilidade().lucro()).isEqualByComparingTo("-10");assertThat(p.getRentabilidade().estado()).isEqualTo("prejuizo");}
 @Test void receitaZeroNaoDividePorZero(){assertThat(RentabilidadePedido.avaliar(BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.ZERO).estado()).isEqualTo("prejuizo");}
}
