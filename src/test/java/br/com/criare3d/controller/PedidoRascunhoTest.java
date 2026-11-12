package br.com.criare3d.controller;
import br.com.criare3d.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.UUID;

class PedidoRascunhoTest {
 @Test void salvarPropostaNaoDependeDeCamposOcultosDeProducaoOuPreco()throws Exception {
  var service=mock(OrcamentoPedidoService.class);var c=new PedidoOrcamentoController(service,mock(OperacaoPedidoService.class),mock(PlanejamentoPedidoService.class),mock(FotoPedidoService.class),mock(FotoRascunhoService.class));
  var id=UUID.randomUUID();var mvc=MockMvcBuilders.standaloneSetup(c).build();
  mvc.perform(post("/orcamentos/"+id+"/rascunho").param("titulo","Orçamento salvo").param("cliente","").param("inicioPretendido","valor antigo inválido").param("frete","valor antigo inválido"))
   .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/orcamentos/"+id+"?etapa=4")).andExpect(flash().attribute("mensagemSucesso","Rascunho salvo."));
  verify(service).salvarCliente(eq(id),argThat(d->d.getTitulo().equals("Orçamento salvo")));verify(service,never()).salvarDados(any(),any());verify(service,never()).enviar(any());
 }
}
