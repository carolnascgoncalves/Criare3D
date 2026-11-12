package br.com.criare3d.controller;

import br.com.criare3d.models.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class OperacaoTemplatesTest {
    private String renderizar(String pagina, Map<String,Object> dados) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var servlet = new MockServletContext();
        var request = new MockHttpServletRequest(servlet);request.setRequestURI("/teste");
        var response = new MockHttpServletResponse();
        var contexto = new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),Locale.forLanguageTag("pt-BR"));
        contexto.setVariables(dados);
        contexto.setVariable("tenantNome","Empresa teste");contexto.setVariable("tenantDisponivel",true);
        contexto.setVariable("tenantLogoUrl","/logo.png");contexto.setVariable("paginaAtiva","vendas-estoque");
        contexto.setVariable("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","teste"));
        return engine.process(pagina,contexto);
    }
    private CalculoHistorico lote(boolean produzido) {
        return CalculoHistorico.builder().id(UUID.randomUUID()).nomePeca("Peça exemplo").unidades(3).unidadesVendidas(produzido ? 1 : 0)
                .produzido(produzido).dataProducao(produzido ? LocalDateTime.now() : null)
                .custoTotal(new BigDecimal("5")).precoSugerido(new BigDecimal("10")).custoFalha(BigDecimal.ZERO).build();
    }
    @Test void loteSemProducaoRenderizaPreviaSemLiberarVenda() {
        var dados = new HashMap<String,Object>();dados.put("lote",lote(false));dados.put("materiais",List.of(new OperacaoLoteController.Material("PLA",BigDecimal.TEN,new BigDecimal("100"))));
        dados.put("vendas",List.of());dados.put("falhas",List.of());dados.put("filamentos",List.of());
        dados.put("perdaMaterial",BigDecimal.ZERO);dados.put("estimadoVendas",BigDecimal.ZERO);dados.put("resultadoVendas",BigDecimal.ZERO);
        String html=renderizar("vendas-estoque/lote",dados);
        assertThat(html).contains("Registrar produção do lote","10,00 g no lote").doesNotContain("id=\"vendaUnidades\"");
    }
    @Test void materialInsuficienteDestacaFaltaSomenteAntesDaProducao(){
        var dados=new HashMap<String,Object>();var c=lote(false);dados.put("lote",c);dados.put("materiais",List.of(new OperacaoLoteController.Material("PLA",new BigDecimal("80.94"),new BigDecimal("8.78"))));
        dados.put("vendas",List.of());dados.put("falhas",List.of());dados.put("filamentos",List.of());dados.put("perdaMaterial",BigDecimal.ZERO);dados.put("estimadoVendas",BigDecimal.ZERO);dados.put("resultadoVendas",BigDecimal.ZERO);
        assertThat(renderizar("vendas-estoque/lote",dados)).contains("operacao-material-insuficiente","faltam 72,16 g");
        c.setProduzido(true);assertThat(renderizar("vendas-estoque/lote",dados)).doesNotContain("operacao-material-insuficiente","Estoque insuficiente");
        var exato=new OperacaoLoteController.Material("PLA",BigDecimal.TEN,BigDecimal.TEN);assertThat(exato.isInsuficiente()).isFalse();assertThat(exato.getFalta()).isZero();
    }
    @Test void loteComVendaRenderizaCancelamentoPorQuantidade() {
        var c=lote(true);var v=new VendaCalculo();v.setId(UUID.randomUUID());v.setCalculo(c);v.setUnidades(2);v.setUnidadesCanceladas(1);v.setDataVenda(LocalDateTime.now());
        var dados=new HashMap<String,Object>();dados.put("lote",c);dados.put("materiais",List.of());dados.put("vendas",List.of(v));dados.put("falhas",List.of());dados.put("filamentos",List.of());
        dados.put("perdaMaterial",BigDecimal.ZERO);dados.put("estimadoVendas",BigDecimal.TEN);dados.put("resultadoVendas",BigDecimal.TEN);
        assertThat(renderizar("vendas-estoque/lote",dados)).contains("Cancelar unidades","1 unidades ativas","vendaUnidades");
    }
    @Test void planejamentosRenderizamComReferenciaAoLote() {
        var p=new PlanejamentoProducao();p.setId(UUID.randomUUID());p.setCalculo(lote(false));p.setNomePeca("Peça exemplo");p.setNomeImpressora("Impressora exemplo");p.setUnidades(3);p.setInicio(LocalDateTime.now());p.setFim(LocalDateTime.now().plusHours(3));
        assertThat(renderizar("simulacao/planejamentos",Map.of("pagina",new PageImpl<>(List.of(p)))))
                .contains("Peça exemplo","Retirar da fila","Operação");
    }
    @Test void arquivoRenderizaBotaoDeRestaurar() {
        assertThat(renderizar("catalogo/arquivados",Map.of("itens",List.of(new ArquivoCatalogoController.Item(UUID.randomUUID(),"Peça arquivada","peca")),"page",0,"mais",false)))
                .contains("Peça arquivada","Restaurar");
    }
}
