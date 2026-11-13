package br.com.criare3d.service;
import br.com.criare3d.models.PedidoOrcamento;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class NumeracaoOrcamentoTest {
 @Test void numeroTemZerosSemTruncarNumerosMaiores() {
  var p=new PedidoOrcamento();p.setNumero(12L);assertThat(p.getCodigo()).isEqualTo("0012");
  p.setNumero(12345L);assertThat(p.getCodigo()).isEqualTo("12345");
 }
 @Test void rascunhoNovoAindaNaoTemNumero() {assertThat(new PedidoOrcamento().getCodigo()).isEqualTo("Novo");}
}
