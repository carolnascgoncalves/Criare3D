package br.com.criare3d;

import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.*;
import br.com.criare3d.security.UsuarioPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

/** Executar apenas contra banco descartável: -Dcriare.integration=true e CRIARE_TEST_DB_URL. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named="criare.integration",matches="true")
@TestPropertySource(properties={"spring.datasource.url=${CRIARE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:55445/postgres}","spring.datasource.username=${CRIARE_TEST_DB_USER:codex_review}","spring.datasource.password=${CRIARE_TEST_DB_PASSWORD:}"})
class ConsolidacaoIntegrationTest {
 @Autowired TenantRepository tenants;
 @Autowired UsuarioRepository usuarios;
 @Autowired ConfiguracaoRepository configs;
 @Autowired CustoLojaRepository custos;
 @Autowired PecaRepository pecas;
 @Autowired ImpressoraRepository impressoras;
 @Autowired EmbalagemRepository embalagens;
 @Autowired FilamentoRepository filamentos;
 @Autowired CalculoHistoricoRepository historicos;
 @Autowired VendaCalculoRepository vendas;
 @Autowired VendaProdutoLojaRepository outras;
 @Autowired CompradorRepository compradores;
 @Autowired MovimentacaoRepository movimentos;
 @Autowired OrcamentoPedidoService orcamentos;
 @Autowired OperacaoPedidoService operacoes;
 @Autowired CalculoService calculos;
 @Autowired HistoricoCalculoService salvar;
 @Autowired VendaService vender;
 @Autowired OutrasVendasService loja;
 @Autowired br.com.criare3d.controller.CatalogoOpcoesController catalogo;
 @Autowired JdbcTemplate jdbc;
 @Autowired PlatformTransactionManager manager;
 private final List<UUID> criados=new ArrayList<>();
 private Usuario conta(){
  SecurityContextHolder.clearContext();var t=new Tenant();t.setNome("Loja integração");t.setSlug("consolidacao-"+UUID.randomUUID());t.setAtivo(true);t.setCorPrimaria("#86202d");t.setCorDestaque("#a8b5a0");t.setCorFundo("#f4eee4");tenants.saveAndFlush(t);criados.add(t.getId());
  var u=usuarios.saveAndFlush(Usuario.builder().email(UUID.randomUUID()+"@teste.local").password("hash-testado-em-outra-suite").perfil(Perfil.USER).tenant(t).build());autenticar(u);return u;
 }
 private void autenticar(Usuario u){var p=new UsuarioPrincipal(u);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(p,null,p.getAuthorities()));}
 @AfterEach void limpar(){SecurityContextHolder.clearContext();for(var tenant:criados){for(var tabela:List.of("movimentacao","operacao_confirmada","venda_produto_loja","produto_loja","rateio_custo_historico","custo_loja","foto_item_pedido","venda_calculo","item_pedido","pedido_orcamento","comprador","custo_personalizado","planejamento_producao","falha_producao","calculo_foto","item_calculo_historico","calculo_historico","configuracao","filamento","impressora","embalagem","peca","usuario"))jdbc.update("DELETE FROM "+tabela+" WHERE tenant_id=?",tenant);jdbc.update("DELETE FROM tenant WHERE id=?",tenant);}}
 @Test void calculoOrcamentoProducaoVendaECancelamentoMantemPrecosESaldos(){
  var usuario=conta();var c=ConfiguracaoPadrao.criar();c.setEmbalagemFixa(true);c.setCustoEmbalagemFixa(new BigDecimal("2"));c.setTarifaEnergiaKwh(BigDecimal.ONE);configs.saveAndFlush(c);
  var f=new Filamento();f.setNome("PLA");f.setCor("Preto");f.setPesoRoloGramas(new BigDecimal("1000"));f.setPrecoRolo(new BigDecimal("100"));f.setEstoqueGramas(new BigDecimal("1000"));f.setUsuario(usuario);filamentos.saveAndFlush(f);
  var i=Impressora.builder().nome("Impressora teste").modelo("Modelo teste").potenciaWatts(new BigDecimal("100")).tempoAquecimentoMinutos(0).build();impressoras.saveAndFlush(i);
  var p=Peca.builder().nome("Peça teste").quantidadeFilamentoGramas(BigDecimal.TEN).tempoImpressaoMinutos(30L).build();pecas.saveAndFlush(p);
  var r=new CalculoRequest();r.setPecaId(p.getId());r.setImpressoraId(i.getId());r.setUnidades(1);r.setPecasPorImpressao(1);r.setTempoImpressaoMinutos(30L);r.setMarkupPercentual(new BigDecimal("50"));
  var item=new ItemCalculoRequest();item.setFilamentoId(f.getId());item.setQuantidadeFilamentoGramas(BigDecimal.TEN);r.setFilamentos(List.of(item));
  var custo=new CustoLoja();custo.setDescricao("Despesa original");custo.setInicio(LocalDate.of(2020,1,1));custo.setMeta(new BigDecimal("1500"));custo.setTipo(CustoLoja.Tipo.VALOR_UNIDADE);custo.setValor(new BigDecimal("5"));custos.saveAndFlush(custo);
  var original=salvar.create(calculos.calcular(r));
  new TransactionTemplate(manager).executeWithoutResult(tx->{filamentos.findById(f.getId()).orElseThrow().setPrecoRolo(new BigDecimal("900"));custos.findById(custo.getId()).orElseThrow().setValor(new BigDecimal("50"));var cfg=configs.findById(c.getId()).orElseThrow();cfg.setEmbalagemFixa(false);cfg.setTarifaEnergiaKwh(new BigDecimal("9"));});
  var embalagem=new Embalagem();embalagem.setNome("Caixa atual");embalagem.setCustoUnitario(new BigDecimal("3"));embalagens.saveAndFlush(embalagem);
  var selecao=new SelecaoPedidoRequest();selecao.setCustosAtuais(true);selecao.setEmbalagemAtualId(embalagem.getId());var escolhido=new SelecaoPedidoRequest.Item();escolhido.setSelecionado(true);escolhido.setReferencia(original.getId());selecao.setItens(List.of(escolhido));
  var recalculado=orcamentos.adicionarVarios(null,selecao);
  assertThat(recalculado.getItens().getFirst().getCusto()).isGreaterThan(original.getCustoTotal());
  var pedido=orcamentos.iniciar(original.getId(),3,0);var id=pedido.getId();
  assertThat(pedido.getItens().getFirst().getCusto()).isEqualByComparingTo(original.getCustoTotal().multiply(new BigDecimal("3")));
  var comprador=new Comprador();comprador.setNome("Cliente vinculado");comprador.setTelefone("11999991234");compradores.saveAndFlush(comprador);
  var dados=new PedidoDadosRequest();dados.setCompradorId(comprador.getId());dados.setTitulo("Proposta teste");dados.setCliente("Cliente teste");dados.setContato("11999991234");dados.setEmail("cliente@example.com");orcamentos.salvarDados(id,dados);orcamentos.enviar(id);
  var atual=orcamentos.buscar(id);operacoes.aprovar(id,atual.getRevisao());atual=orcamentos.buscar(id);operacoes.produzir(id,atual.getItens().getFirst().getId(),atual.getRevisao());
  atual=orcamentos.buscar(id);var lote=atual.getItens().getFirst().getLoteProducao().getId();operacoes.entregar(id,atual.getRevisao());
  assertThat(historicos.findById(lote).orElseThrow().getUnidadesVendidas()).isEqualTo(3);
  assertThat(filamentos.findById(f.getId()).orElseThrow().getEstoqueGramas()).isEqualByComparingTo("970");
  atual=orcamentos.buscar(id);operacoes.cancelar(id,atual.getRevisao());assertThat(historicos.findById(lote).orElseThrow().getUnidadesVendidas()).isZero();
  var chave=UUID.randomUUID();assertThat(vender.registrarVenda(lote,1,null,null,null,chave)).isEqualTo(ResultadoOperacao.OK);vender.registrarVenda(lote,1,null,null,null,chave);
  assertThat(historicos.findById(lote).orElseThrow().getUnidadesVendidas()).isEqualTo(1);
  assertThatIllegalArgumentException().isThrownBy(()->vender.registrarVenda(lote,2,null,null,null,chave));
  assertThat(orcamentos.buscar(id).getEmail()).isEqualTo("cliente@example.com");
  assertThat(orcamentos.listarDoComprador(comprador.getId(),"",0,false,null).getTotalElements()).isEqualTo(1);
  assertThat(historicos.findById(lote).orElseThrow().getCustoLoja()).isEqualByComparingTo("5");
  assertThat(movimentos.count()).isGreaterThanOrEqualTo(4);
 }
 @Test void embalagemCompartilhadaIdempotenciaBuscaEIsolamento(){
  var a=conta();var produto=loja.salvar(null,"LED",new BigDecimal("2.50"),BigDecimal.ONE);var hoje=LocalDate.now(ZoneId.of("America/Sao_Paulo"));var chave=UUID.randomUUID();
  var venda=loja.vender(produto.getId(),10,null,null,hoje,VendaProdutoLoja.TipoEmbalagem.PERSONALIZADA,null,BigDecimal.ONE,1,chave);
  var repetida=loja.vender(produto.getId(),10,null,null,hoje,VendaProdutoLoja.TipoEmbalagem.PERSONALIZADA,null,BigDecimal.ONE,1,chave);assertThat(repetida.getId()).isEqualTo(venda.getId());
  assertThat(outras.resumir(hoje.atStartOfDay(),hoje.plusDays(1).atStartOfDay()).getLucro()).isEqualByComparingTo("14");loja.cancelar(venda.getId(),5);
  assertThat(outras.resumir(hoje.atStartOfDay(),hoje.plusDays(1).atStartOfDay()).getLucro()).isEqualByComparingTo("7");
  for(int n=0;n<25;n++){var c=new Comprador();c.setNome(String.format("Comprador %02d",n));c.setEmail("cliente"+n+"@example.com");compradores.saveAndFlush(c);}
  assertThat(catalogo.buscar("comprador","",0).itens()).hasSize(20);assertThat(catalogo.buscar("comprador","",1).itens()).hasSize(5);assertThat(catalogo.buscar("comprador","24",0).itens()).hasSize(1);
  var comprador=compradores.findAll().getFirst();conta();assertThat(compradores.findById(comprador.getId())).isEmpty();assertThat(outras.findById(venda.getId())).isEmpty();assertThat(catalogo.buscar("comprador","",0).itens()).isEmpty();assertThat(movimentos.count()).isZero();
  autenticar(a);assertThat(compradores.count()).isEqualTo(25);
 }
 @Test void confirmacoesSimultaneasCriamUmaUnicaVenda() throws Exception {
  var a=conta();var produto=loja.salvar(null,"Venda simultânea",BigDecimal.TEN,BigDecimal.ONE);var chave=UUID.randomUUID();
  var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var largada=new java.util.concurrent.CountDownLatch(1);
  try {
   java.util.concurrent.Callable<UUID> enviar=()->{autenticar(a);try{largada.await();return loja.vender(produto.getId(),1,null,null,null,VendaProdutoLoja.TipoEmbalagem.SEM,null,null,1,chave).getId();}finally{SecurityContextHolder.clearContext();}};
   var primeira=pool.submit(enviar);var segunda=pool.submit(enviar);largada.countDown();
   assertThat(primeira.get(15,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(segunda.get(15,java.util.concurrent.TimeUnit.SECONDS));
   assertThat(outras.count()).isEqualTo(1);assertThat(movimentos.count()).isEqualTo(1);
  } finally {pool.shutdownNow();}
 }
 @Test void dashboardArredondaEmbalagemCompartilhadaComoADetalheDaVenda(){
  conta();var produto=loja.salvar(null,"Arredondamento",new BigDecimal("2.50"),BigDecimal.ONE);var hoje=LocalDate.now(ZoneId.of("America/Sao_Paulo"));
  var venda=loja.vender(produto.getId(),3,null,null,hoje,VendaProdutoLoja.TipoEmbalagem.PERSONALIZADA,null,BigDecimal.ONE,1);loja.cancelar(venda.getId(),2);
  var detalhe=outras.findById(venda.getId()).orElseThrow();var resumo=outras.resumir(hoje.atStartOfDay(),hoje.plusDays(1).atStartOfDay());
  assertThat(detalhe.getLucro()).isEqualByComparingTo("1.17");assertThat(resumo.getLucro()).isEqualByComparingTo(detalhe.getLucro());
 }
}
