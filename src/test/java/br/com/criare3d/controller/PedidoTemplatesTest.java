package br.com.criare3d.controller;

import br.com.criare3d.dto.PedidoDadosRequest;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.StatusPedido;
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
import static org.assertj.core.api.Assertions.*;

class PedidoTemplatesTest {
    private String renderizar(String pagina, Map<String,Object> dados) {
        var resolver=new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);
        request.setRequestURI("/orcamentos");
        var application=new org.springframework.web.context.support.StaticWebApplicationContext();
        application.setServletContext(servlet);application.refresh();
        servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,application);
        var contexto=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag("pt-BR"));
        contexto.setVariables(dados);contexto.setVariable("tenantNome","Empresa teste");contexto.setVariable("tenantDisponivel",true);
        var springContext=new org.springframework.web.servlet.support.RequestContext(request,new MockHttpServletResponse(),servlet,dados);
        contexto.setVariable("thymeleafRequestContext",new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(springContext,request));
        contexto.setVariable("tenantLogoUrl","/logo.png");contexto.setVariable("paginaAtiva","orcamentos");
        contexto.setVariable("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","teste"));
        return engine.process(pagina,contexto);
    }
    private PedidoOrcamento pedido() {
        var p=new PedidoOrcamento();p.setId(UUID.randomUUID());p.setCliente("Maria");p.setCriadoEm(LocalDateTime.now());
        var i=new ItemPedido();i.setId(UUID.randomUUID());i.setPedido(p);i.setNome("Suporte de livros");i.setMateriais("PLA azul");
        i.setQuantidade(5);i.setUsarEstoque(2);i.setPrecoUnitario(new BigDecimal("20.00"));i.setCusto(new BigDecimal("31.97"));
        p.getItens().add(i);return p;
    }
    @Test void mostraMateriaisFaltantesEAsPecasAtendidasPeloEstoque() {
        var p=pedido();var pronta=new ItemPedido();pronta.setNome("T-Rex");pronta.setQuantidade(4);pronta.setUsarEstoque(4);pronta.setPrecoUnitario(BigDecimal.TEN);pronta.setCusto(BigDecimal.ZERO);p.getItens().add(pronta);
        var m=new br.com.criare3d.service.MateriaisPedido.Material(UUID.randomUUID(),"PLA","Azul",new BigDecimal("1100"),new BigDecimal("100"),new BigDecimal("1000"),1,new BigDecimal("80"));
        var dados=new HashMap<String,Object>();dados.put("pedido",p);dados.put("etapa",2);dados.put("referencias",List.of());dados.put("dados",new PedidoDadosRequest());dados.put("previsoes",List.of());dados.put("materiaisPedido",new br.com.criare3d.service.MateriaisPedido.Resumo(List.of(m),new BigDecimal("80"),false));
        assertThat(renderizar("orcamentos/editar",dados)).contains("T-Rex","4 do estoque","Vai usar","Falta repor","80,00");
    }
    @Test void todasAsEtapasRenderizamSemSalvarOuMovimentarEstoque() {
        for(int etapa=1;etapa<=4;etapa++) {
            String html=renderizar("orcamentos/editar",Map.of("pedido",pedido(),"etapa",etapa,"referencias",List.of(),"dados",new PedidoDadosRequest(),"previsoes",List.of()));
            assertThat(html).contains("Nenhum estoque foi reservado","/css/pedidos.css","/js/pedidos.js");
            if(etapa==1 || etapa==3)assertThat(html).contains("Suporte de livros");
        }
    }
    @Test void propostaNaoExibeCustoLucroOuParametrosInternos() {
        var html=renderizar("orcamentos/proposta",Map.of("pedido",pedido()));
        assertThat(html).contains("Maria","Suporte de livros","PLA azul","100,00");
        assertThat(html).doesNotContain("31,97","Lucro estimado","Taxas estimadas","Estoque de filamento");
    }
    @Test void precoDoBancoEExibidoComDuasCasasEListaTemArquivo() {
        var p=pedido();p.getItens().getFirst().setPrecoUnitario(new BigDecimal("13.71000000"));
        var html=renderizar("orcamentos/editar",Map.of("pedido",p,"etapa",3,"referencias",List.of(),"dados",new PedidoDadosRequest(),"previsoes",List.of()));
        assertThat(html).contains("value=\"13.71\"").doesNotContain("value=\"13.71000000\"");
        assertThat(renderizar("orcamentos/listar",Map.of("pagina",new PageImpl<>(List.of(p)),"busca","","arquivados",false)))
                .contains("Arquivar","Arquivados");
        p.setArquivado(true);
        assertThat(renderizar("orcamentos/listar",Map.of("pagina",new PageImpl<>(List.of(p)),"busca","","arquivados",true))).contains("Restaurar");
    }
    @Test void listaEInicioFuncionamVaziosEComRascunhoSalvo() {
        assertThat(renderizar("orcamentos/listar",Map.of("pagina",new PageImpl<>(List.of(pedido())),"busca",""))).contains("Maria");
        var novo=new PedidoOrcamento();
        assertThat(renderizar("orcamentos/editar",Map.of("pedido",novo,"etapa",1,"referencias",List.of(),"dados",new PedidoDadosRequest(),"previsoes",List.of()))).contains("primeiro cálculo","/orcamentos/iniciar","Produção");
    }
    @Test void freteEConexaoRenderizamConfiguradosESemConfiguracao(){
        assertThat(renderizar("orcamentos/melhor-envio",Map.of("configurado",false,"conectado",false,"ambiente","sandbox","callback",""))).contains("Configure Client ID");
        var d=new br.com.criare3d.dto.FreteRequest();
        assertThat(renderizar("orcamentos/frete",Map.of("pedido",pedido(),"freteDados",d,"conectado",true,"ambiente","sandbox"))).contains("Peso total (kg)","Consultar opções de frete");
    }
    @Test void listagemMostraFiltroComTodosOsStatus(){
        var html=renderizar("orcamentos/listar",Map.of("pagina",new PageImpl<>(List.of()),"busca","","arquivados",false,"statusOpcoes",StatusPedido.values(),"statusFiltro",StatusPedido.ENTREGUE));
        assertThat(html).contains("Todos os status","Rascunho","Enviado","A produzir","Pronto para entregar","Entregue","Cancelado");
    }
    @Test void pedidoEnviadoExibeAprovacaoEProntoExibeEntrega() {
        var p=pedido();p.setStatus(StatusPedido.ENVIADO);
        assertThat(renderizar("orcamentos/detalhes",Map.of("pedido",p,"previsoes",List.of()))).contains("Cliente aprovou").doesNotContain("Confirmar entrega e venda");
        p.setStatus(StatusPedido.APROVADO);
        assertThat(renderizar("orcamentos/detalhes",Map.of("pedido",p,"previsoes",List.of())))
                .contains("Próxima etapa","Outras ações","Confirmar produção concluída").doesNotContain("Confirmar entrega e venda");
        p.setStatus(StatusPedido.PRONTO);
        assertThat(renderizar("orcamentos/detalhes",Map.of("pedido",p,"previsoes",List.of()))).contains("Confirmar entrega e venda").doesNotContain("Cliente aprovou");
    }
}
