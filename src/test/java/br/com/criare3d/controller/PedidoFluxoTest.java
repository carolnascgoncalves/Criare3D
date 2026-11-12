package br.com.criare3d.controller;
import br.com.criare3d.models.*;
import br.com.criare3d.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PedidoFluxoTest {
 private final OrcamentoPedidoService service=mock(OrcamentoPedidoService.class);
 private final PlanejamentoPedidoService prazo=mock(PlanejamentoPedidoService.class);
 private final PedidoOrcamentoController controller=new PedidoOrcamentoController(service,mock(OperacaoPedidoService.class),prazo,mock(FotoPedidoService.class),mock(FotoRascunhoService.class));
 private PedidoOrcamento pedido(){var p=new PedidoOrcamento();p.setId(UUID.randomUUID());var i=new ItemPedido();i.setId(UUID.randomUUID());i.setQuantidade(2);i.setPrecoUnitario(BigDecimal.TEN);i.setCusto(BigDecimal.ONE);p.getItens().add(i);when(service.buscar(p.getId())).thenReturn(p);return p;}
 @Test void precoNaoConsultaMateriaisNemPrevisao(){var p=pedido();controller.abrir(p.getId(),3,false);verifyNoInteractions(prazo);verify(service,never()).materiais(any());verify(service,never()).referencias();}
 @Test void clienteConsultaSoReferenciasEMateriais(){var p=pedido();controller.abrir(p.getId(),1,false);verifyNoInteractions(prazo);verify(service).materiais(p);verify(service).referencias();}
 @Test void prazoIniciaSemCliqueENaoConsultaMateriais(){var p=pedido();var inicio=java.time.LocalDateTime.of(2026,11,20,8,0);when(prazo.prever(p)).thenReturn(List.of(new PlanejamentoPedidoService.Linha(p.getItens().getFirst().getId(),"Peça",inicio,inicio.plusHours(2))));assertThat(controller.abrir(p.getId(),2,false).getModel().get("iniciarSimulacao")).isEqualTo(true);verify(service,never()).materiais(any());}
 @Test void salvarSemProximaPermaneceNaEtapaAtual()throws Exception{var p=pedido();MockMvcBuilders.standaloneSetup(controller).build().perform(post("/orcamentos/"+p.getId()+"/dados").param("titulo","Teste").param("etapa","1")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/orcamentos/"+p.getId()+"?etapa=1"));}
 @Test void autosaveAtualizaTotalSemRedirecionarOuExibirToast()throws Exception{var p=pedido();MockMvcBuilders.standaloneSetup(controller).build().perform(post("/orcamentos/"+p.getId()+"/itens/"+p.getItens().getFirst().getId()+"/autosave").param("quantidade","2").param("preco","10.00").param("etapa","3")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(20)).andExpect(header().doesNotExist("Location"));verify(service,never()).materiais(any());}
 @Test void autosaveRejeitadoMantemMensagemDeErro()throws Exception{var p=pedido();doThrow(new IllegalArgumentException("Estoque mudou.")).when(service).alterarItem(any(),any(),anyInt(),anyInt(),any());MockMvcBuilders.standaloneSetup(controller).build().perform(post("/orcamentos/"+p.getId()+"/itens/"+p.getItens().getFirst().getId()+"/autosave").param("quantidade","2").param("preco","10.00")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erro").value("Estoque mudou."));}
}
