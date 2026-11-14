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
        var application=new org.springframework.web.context.support.StaticWebApplicationContext();
        application.setServletContext(servlet);application.refresh();
        servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,application);
        var contexto = new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),Locale.forLanguageTag("pt-BR"));
        contexto.setVariables(dados);
        var springContext=new org.springframework.web.servlet.support.RequestContext(request,response,servlet,dados);
        contexto.setVariable("thymeleafRequestContext",new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(springContext,request));
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
    @Test void loteTotalmenteVendidoMostraConclusaoESemFormularioDeVenda() {
        var c=lote(true);c.setUnidadesVendidas(3);
        var dados=new HashMap<String,Object>();dados.put("lote",c);dados.put("materiais",List.of());dados.put("vendas",List.of());dados.put("falhas",List.of());dados.put("filamentos",List.of());
        dados.put("perdaMaterial",BigDecimal.ZERO);dados.put("estimadoVendas",BigDecimal.TEN);dados.put("resultadoVendas",new BigDecimal("28.11"));
        String html=renderizar("vendas-estoque/lote",dados);
        assertThat(html).contains("LOTE VENDIDO","Todas as unidades foram vendidas","3 unidades vendidas","28,11","Ver vendas registradas")
            .doesNotContain("Registrar venda","id=\"vendaUnidades\"");
        c.setUnidadesVendidas(2);
        assertThat(renderizar("vendas-estoque/lote",dados)).contains("Registrar venda","2 de 3 unidades já vendidas","id=\"vendaUnidades\"")
            .doesNotContain("Todas as unidades foram vendidas");
    }
    @Test void planejamentosRenderizamComReferenciaAoLote() {
        var p=new PlanejamentoProducao();p.setId(UUID.randomUUID());p.setCalculo(lote(false));p.setNomePeca("Peça exemplo");p.setNomeImpressora("Impressora exemplo");p.setUnidades(3);p.setInicio(LocalDateTime.now());p.setFim(LocalDateTime.now().plusHours(3));
        assertThat(renderizar("simulacao/planejamentos",Map.of("pagina",new PageImpl<>(List.of(p)))))
                .contains("Peça exemplo","Retirar da fila","Operação");
        p.getCalculo().setProduzido(true);
        assertThat(renderizar("simulacao/planejamentos",Map.of("pagina",new PageImpl<>(List.of(p)),"concluidos",false)))
                .contains("Peça exemplo","Produzido · aguardando venda");
        assertThat(renderizar("simulacao/planejamentos",Map.of("pagina",new PageImpl<>(List.of(p)),"concluidos",true)))
                .doesNotContain("Produzido · aguardando venda");
    }
    @Test void arquivoRenderizaBotaoDeRestaurar() {
        assertThat(renderizar("catalogo/arquivados",Map.of("itens",List.of(new ArquivoCatalogoController.Item(UUID.randomUUID(),"Peça arquivada","peca")),"page",0,"mais",false)))
                .contains("Peça arquivada","Restaurar");
    }

    @Test void novasTelasRenderizamRateioEConsulta() {
        var c=new CustoLoja();c.setId(UUID.randomUUID());c.setDescricao("Despesas novembro");c.setInicio(java.time.LocalDate.of(2026,11,1));c.setMeta(new BigDecimal("1500"));c.setTipo(CustoLoja.Tipo.VALOR_UNIDADE);c.setValor(new BigDecimal("5"));
        var form=new CustoLojaController.Formulario();form.setInicio(c.getInicio());
        var r=new CustoLojaController.Resumo(c,new BigDecimal("50"),new BigDecimal("1450"),BigDecimal.ZERO);
        assertThat(renderizar("custos-loja/index",Map.of("custo",form,"resumos",List.of(r)))).contains("1.450,00","50,00","Pausar aplicação");
        var rateio=new RateioCustoHistorico();rateio.setCustoLoja(c);rateio.setCalculo(lote(true));rateio.setValorUnitario(new BigDecimal("5"));
        assertThat(renderizar("custos-loja/detalhes",Map.of("custo",c,"recuperado",new BigDecimal("50"),"restante",new BigDecimal("1450"),"itens",List.of(rateio),"page",0,"mais",false))).contains("Peça exemplo","5,00");
        var config=br.com.criare3d.service.ConfiguracaoPadrao.criar();
        assertThat(renderizar("calculo-rapido/index",Map.of("configuracao",config,"filamentos",List.of(),"impressoras",List.of(),"embalagens",List.of()))).contains("Cálculo rápido","data-rapido-form","sem cadastrar a peça");
    }
    @Test void loteIdentificaCorEExibeCustoDoLoteEUnidade() {
        var dados=new HashMap<String,Object>();dados.put("lote",lote(false));dados.put("materiais",List.of(new OperacaoLoteController.Material("PLA",BigDecimal.TEN,BigDecimal.TEN,"Vermelho","#ff0000",new BigDecimal("3"))));
        dados.put("vendas",List.of());dados.put("falhas",List.of());dados.put("filamentos",List.of());dados.put("perdaMaterial",BigDecimal.ZERO);dados.put("estimadoVendas",BigDecimal.ZERO);dados.put("resultadoVendas",BigDecimal.ZERO);
        assertThat(renderizar("vendas-estoque/lote",dados)).contains("Vermelho","#ff0000","R$ 15,00 no lote","R$ 5,00 por unidade");
    }

    @Test void outrasVendasMostramProdutoPrecoECancelamento() {
        var p=new ProdutoLoja();p.setId(UUID.randomUUID());p.setNome("Luz de LED");p.setPreco(new BigDecimal("2.50"));
        var v=new VendaProdutoLoja();v.setId(UUID.randomUUID());v.setNomeProduto(p.getNome());v.setProduto(p);v.setQuantidade(2);v.setPrecoUnitario(p.getPreco());v.setDataVenda(LocalDateTime.now());
        var paginaProdutos=new PageImpl<>(List.of(p));var paginaVendas=new PageImpl<>(List.of(v));
        var html=renderizar("outras-vendas/index",Map.of("produtoFormulario",new OutrasVendasController.ProdutoFormulario(),"produtos",paginaProdutos,"vendas",paginaVendas,"opcoes",List.of(p),"hoje",java.time.LocalDate.now()));
        assertThat(html).contains("Luz de LED","2,50","5,00","lucro não calculado","Cancelar unidades");
    }

    @Test void dashboardRenderizaIndicadoresDeOutrasVendas() {
        var historicos=org.mockito.Mockito.mock(br.com.criare3d.repository.CalculoHistoricoRepository.class);
        var base=org.mockito.Mockito.mock(br.com.criare3d.service.DashboardService.class);
        var usuario=org.mockito.Mockito.mock(br.com.criare3d.service.UsuarioLogadoService.class);
        org.mockito.Mockito.when(usuario.buscarTenantIdAtual()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.when(base.resumir(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any())).thenReturn(new br.com.criare3d.dto.DashboardResumo(0,0,0,BigDecimal.ZERO,0,0,BigDecimal.ZERO));
        var dados=new br.com.criare3d.service.DashboardDadosService(historicos,base,usuario,
            org.mockito.Mockito.mock(br.com.criare3d.service.interfaces.PecaService.class),
            org.mockito.Mockito.mock(br.com.criare3d.service.interfaces.FilamentoService.class),
            org.mockito.Mockito.mock(br.com.criare3d.service.interfaces.ImpressoraService.class),
            org.mockito.Mockito.mock(br.com.criare3d.service.interfaces.EmbalagemService.class));
        assertThat(renderizar("dashboard/index",dados.buscar("30"))).contains("Outros produtos","Gerenciar produtos e vendas","Vendas 3D por plataforma");
    }

    @Test void listasMostramPrecoEMenuSeguroSemInventarDados() {
        var p=new Peca();p.setId(UUID.randomUUID());p.setNome("T-Rex &amp; suporte");p.setQuantidadeFilamentoGramas(BigDecimal.TEN);p.setTempoImpressaoMinutos(30L);
        var dados=new HashMap<String,Object>();dados.put("pecas",List.of(p));dados.put("pagina",new PageImpl<>(List.of(p)));dados.put("busca","");dados.put("arquivosPeca",Map.of());dados.put("precosPeca",Map.of(p.getId(),new BigDecimal("12.50")));
        assertThat(renderizar("peca/listar",dados)).contains("12,50","Calcular","acoes-menu","/catalogo/peca/"+p.getId()+"/arquivar").doesNotContain("&amp;amp;");
        var f=new Filamento();f.setId(UUID.randomUUID());f.setNome("PLA");f.setCor("Amarelo");f.setPesoRoloGramas(new BigDecimal("1000"));f.setEstoqueGramas(new BigDecimal("1811"));f.setPrecoRolo(new BigDecimal("80"));
        dados.clear();dados.put("filamentos",List.of(f));dados.put("pagina",new PageImpl<>(List.of(f)));dados.put("busca","");dados.put("ordem","nomeAsc");dados.put("percentualEstoque",Map.of(f.getId(),100));dados.put("filamentosComHistoricoProduzido",Map.of(f.getId(),false));
        assertThat(renderizar("filamento/listar",dados)).contains("2 rolo(s) equivalentes","Acabando","acoes-menu").doesNotContain("1811 g de");
    }
    @Test void configuracaoRenderizaIndiceEInterruptores() {
        var c=new Configuracao();c.setMarkupFixo(false);c.setMarkupPercentual(new BigDecimal("30"));c.setMarkupOpcao1(new BigDecimal("30"));c.setMarkupOpcao2(new BigDecimal("40"));c.setMarkupOpcao3(new BigDecimal("50"));
        var html=renderizar("configuracao/index",Map.of("configuracao",br.com.criare3d.dto.ConfiguracaoRequest.toDto(c)));
        assertThat(html).contains("configuracao-indice","role=\"switch\"","data-exemplo-markup");
    }

    @Test void vendasComReservasLongRenderizamSemAmbiguidadeDeTipos() {
        var id=UUID.randomUUID();var h=new br.com.criare3d.dto.HistoricoListaResponse(id,"Peça reservada",LocalDateTime.now(),"NENHUMA",null,BigDecimal.TEN,true,false,false,4,BigDecimal.ONE,BigDecimal.ONE,null,0);
        var dados=new HashMap<String,Object>();dados.put("modoDetalhe",false);dados.put("aba","");dados.put("paraProduzir",List.of());dados.put("emEstoque",List.of(h));dados.put("vendidos",List.of());dados.put("reservadas",Map.of(id,4L));
        for(var k:List.of("lotesParaProduzir","lotesEmEstoque","lotesVendidos","unidadesParaProduzir","unidadesEmEstoque","unidadesVendidas","unidadesVendidasMes","lotesVendidosMes"))dados.put(k,0L);
        dados.put("valorEmEstoque",BigDecimal.ZERO);dados.put("lucroVendidoMes",BigDecimal.ZERO);
        assertThat(renderizar("vendas-estoque/listar",dados)).contains("Tudo reservado","4 reservadas","width:100.0%");
    }
    @Test void clientesEExtratoRenderizamSemRegistros() {
        assertThat(renderizar("compradores/index",Map.of("comprador",new Comprador(),"pagina",new PageImpl<>(List.of()),"busca","","arquivados",false)))
            .contains("Novo cliente","Nenhum cliente encontrado","Buscar clientes");
        assertThat(renderizar("movimentacoes/index",Map.of("pagina",new PageImpl<>(List.of()),"busca","","tipo","","inicio",java.time.LocalDate.of(2026,11,1),"fim",java.time.LocalDate.of(2026,11,14))))
            .contains("Extrato de movimentações","Nenhuma movimentação");
    }
    @Test void extratoComMovimentacaoMostraQuantidadeEOrigem() {
        var m=new Movimentacao();m.setId(UUID.randomUUID());m.setTipo("VENDA_3D");m.setDescricao("Peça vendida");m.setQuantidade(new BigDecimal("-2"));m.setMedida("un.");m.setOrigem("lote");m.setReferencia(UUID.randomUUID());m.setData(LocalDateTime.of(2026,11,14,10,0));
        assertThat(renderizar("movimentacoes/index",Map.of("pagina",new PageImpl<>(List.of(m)),"busca","","tipo","","inicio",java.time.LocalDate.of(2026,11,1),"fim",java.time.LocalDate.of(2026,11,14))))
            .contains("Peça vendida","-2 un.","Ver origem","/vendas-estoque/lote/");
    }
}
