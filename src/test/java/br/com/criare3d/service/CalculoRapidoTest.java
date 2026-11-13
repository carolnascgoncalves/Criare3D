package br.com.criare3d.service;
import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.CustoLojaRepository;
import br.com.criare3d.service.interfaces.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CalculoRapidoTest {
    @Test void consultaSemPecaCadastradaUsaMesmoMotorERateioSemPersistencia() {
        var pecas=mock(PecaService.class);var impressoras=mock(ImpressoraService.class);var filamentos=mock(FilamentoService.class);
        var embalagens=mock(EmbalagemService.class);var config=mock(ConfiguracaoService.class);var manutencao=mock(ManutencaoImpressoraService.class);
        var custos=mock(CustoLojaRepository.class);var clock=Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"),ZoneId.of("America/Sao_Paulo"));
        var servico=new CalculoService(new CalculoPlataformaService(),pecas,filamentos,impressoras,embalagens,config,manutencao,clock);
        ReflectionTestUtils.setField(servico,"custosLoja",custos);
        var i=new Impressora();i.setId(UUID.randomUUID());i.setNome("Impressora");i.setPotenciaWatts(new BigDecimal("100"));
        var f=new Filamento();f.setId(UUID.randomUUID());f.setNome("PLA");f.setCor("Vermelho");f.setCodigoCor("#ff0000");f.setPesoRoloGramas(new BigDecimal("1000"));f.setPrecoRolo(new BigDecimal("100"));
        when(impressoras.fetchById(i.getId())).thenReturn(Optional.of(i));when(filamentos.fetchByIds(any())).thenReturn(List.of(f));
        var configuracao=ConfiguracaoPadrao.criar();configuracao.setEmbalagemFixa(true);configuracao.setCustoEmbalagemFixa(BigDecimal.ZERO);configuracao.setTarifaEnergiaKwh(BigDecimal.ONE);
        when(config.buscarConfiguracao()).thenReturn(Optional.of(configuracao));when(manutencao.custoPorHora(i.getId())).thenReturn(BigDecimal.ZERO);
        var custo=new CustoLoja();custo.setDescricao("Loja");custo.setTipo(CustoLoja.Tipo.VALOR_UNIDADE);custo.setValor(new BigDecimal("5"));
        when(custos.findByAtivoTrueAndInicioLessThanEqualOrderByInicioAsc(LocalDate.of(2026,11,13))).thenReturn(List.of(custo));
        var request=new CalculoRequest();request.setImpressoraId(i.getId());request.setTempoImpressaoMinutos(60L);request.setUnidades(2);request.setPecasPorImpressao(2);request.setMarkupPercentual(new BigDecimal("30"));
        var item=new ItemCalculoRequest();item.setFilamentoId(f.getId());item.setQuantidadeFilamentoGramas(BigDecimal.TEN);request.setFilamentos(List.of(item));
        var resultado=servico.calcularAvulso(request,"Consulta");
        assertThat(request.getTempoImpressaoMinutos()).isEqualTo(60L);
        assertThat(resultado.getId()).isNull();assertThat(resultado.getPeca()).isNull();assertThat(resultado.getMinutosMaquinaLote()).isEqualTo(60L);
        assertThat(resultado.getCustoLoja()).isEqualByComparingTo("5");assertThat(resultado.getRateiosCustos()).hasSize(1);
        assertThat(resultado.getCustoTotal()).isEqualByComparingTo("6.05");assertThat(resultado.getPrecoSugerido()).isEqualByComparingTo("7.87");
        request.setTempoImpressaoMinutos(7L);request.setUnidades(10);request.setPecasPorImpressao(10);
        assertThat(servico.calcularAvulso(request,"Placa com dez peças").getMinutosMaquinaLote()).isEqualTo(7L);
        i.setTempoAquecimentoMinutos(5);
        configuracao.setPurgaGramasPorImpressao(new BigDecimal("2"));
        request.setTempoImpressaoMinutos(90L);request.setUnidades(4);item.setQuantidadeFilamentoGramas(new BigDecimal("80"));
        var conjunto=servico.calcularAvulsoTotais(request,"Duas placas",2);
        assertThat(conjunto.getMinutosMaquinaLote()).isEqualTo(100L);
        assertThat(conjunto.getFornadas()).isEqualTo(2);
        assertThat(conjunto.getGramasTotaisLote()).isEqualByComparingTo("84");
        assertThat(conjunto.getCustoFilamento()).isEqualByComparingTo("2");
        assertThat(conjunto.getCustoLoja()).isEqualByComparingTo("5");
        request.setTempoImpressaoMinutos(90L);request.setUnidades(1);item.setQuantidadeFilamentoGramas(new BigDecimal("80"));
        var umaCompleta=servico.calcularAvulsoTotais(request,"Arquivo completo",2);
        request.setTempoImpressaoMinutos(180L);request.setUnidades(2);item.setQuantidadeFilamentoGramas(new BigDecimal("160"));
        var duasCompletas=servico.calcularAvulsoTotais(request,"Arquivo completo",4);
        assertThat(umaCompleta.getCustoFilamento()).isEqualByComparingTo("8");
        assertThat(duasCompletas.getCustoFilamento()).isEqualByComparingTo(umaCompleta.getCustoFilamento());
        assertThat(duasCompletas.getCustoTotal()).isEqualByComparingTo(umaCompleta.getCustoTotal());
        assertThat(duasCompletas.getMinutosMaquinaLote()).isEqualTo(200L);
        assertThat(duasCompletas.getGramasTotaisLote()).isEqualByComparingTo("168");
        assertThat(duasCompletas.getCustoLoja()).isEqualByComparingTo("5");
        verifyNoInteractions(pecas,embalagens);verify(custos,never()).save(any());
    }
}
