package br.com.criare3d.controller;
import br.com.criare3d.models.Configuracao;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ConfiguracaoSalvarTest {
 @Test void salvaComMarkupVariavelSemExigirCampoRecolhido() throws Exception {
  var service=mock(ConfiguracaoService.class);var atual=new Configuracao();atual.setId(UUID.randomUUID());atual.setMarkupPercentual(new BigDecimal("30"));when(service.buscarOuCriarConfiguracao()).thenReturn(atual);
  var mvc=MockMvcBuilders.standaloneSetup(new ConfiguracaoController(service)).build();
  mvc.perform(post("/configuracao/salvar").param("markupFixo","false").param("markupOpcao1","30").param("markupOpcao2","40").param("markupOpcao3","50").param("tarifaEnergiaKwh","0.85").param("embalagemFixa","false"))
   .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/configuracao?sucesso"));
  verify(service).update(eq(atual.getId()),argThat(c->c.getMarkupPercentual().compareTo(new BigDecimal("30"))==0));
 }
 @Test void percentualFixoExigeValorEDevolveFormularioComErro() throws Exception {
  var service=mock(ConfiguracaoService.class);when(service.buscarOuCriarConfiguracao()).thenReturn(new Configuracao());
  MockMvcBuilders.standaloneSetup(new ConfiguracaoController(service)).build().perform(post("/configuracao/salvar").param("markupFixo","true").param("markupOpcao1","30").param("markupOpcao2","40").param("markupOpcao3","50").param("tarifaEnergiaKwh","0.85").param("embalagemFixa","false"))
   .andExpect(status().isUnprocessableEntity()).andExpect(model().attributeExists("mensagemErro"));
  verify(service,never()).update(any(),any());
 }
}
