package br.com.criare3d.controller;
import br.com.criare3d.service.*;
import br.com.criare3d.models.PedidoOrcamento;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NovoOrcamentoTest {
 @Test void novoOrcamentoAbreDiretoNaPrimeiraEtapaSemGravarRascunhoVazio(){
  var service=mock(OrcamentoPedidoService.class);var controller=new PedidoOrcamentoController(service,mock(OperacaoPedidoService.class),mock(PlanejamentoPedidoService.class),mock(FotoPedidoService.class),mock(FotoRascunhoService.class));
  var mv=controller.novo(null);assertThat(mv.getViewName()).isEqualTo("orcamentos/editar");assertThat(mv.getModel().get("etapa")).isEqualTo(1);
  assertThat(((PedidoOrcamento)mv.getModel().get("pedido")).getId()).isNull();
 }
}
