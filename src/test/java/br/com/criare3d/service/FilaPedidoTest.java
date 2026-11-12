package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.repository.PlanejamentoProducaoRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FilaPedidoTest {
 private final PlanejamentoProducaoRepository planos=mock(PlanejamentoProducaoRepository.class);
 private final EntradaPedidoService entradas=mock(EntradaPedidoService.class);
 private final Clock clock=Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"),ZoneId.of("America/Sao_Paulo"));
 private final PlanejamentoPedidoService service=new PlanejamentoPedidoService(planos,entradas,clock);
 private PedidoOrcamento pedido(LocalDateTime inicio){var p=new PedidoOrcamento();p.setInicioPretendido(inicio);var i=new ItemPedido();i.setId(UUID.randomUUID());i.setImpressoraId(UUID.randomUUID());i.setQuantidade(1);i.setNome("Peça");i.setEntradaJson("entrada");p.getItens().add(i);var r=new CalculoRequest();r.setTempoImpressaoMinutos(60L);r.setPecasPorImpressao(1);when(entradas.ler("entrada",1)).thenReturn(r);return p;}
 @Test void filaVaziaComecaNaDataPedidaSemAlegarOutroPlanejamento(){var inicio=LocalDateTime.of(2026,11,16,8,0);var p=pedido(inicio);var linha=service.prever(p).getFirst();assertThat(linha.inicio()).isEqualTo(inicio);assertThat(linha.observacao()).contains("Sem impressão anterior").doesNotContain("Aguardando planejamento");}
 @Test void fimDeSemanaAjustaExpedienteSemInventarOutraImpressao(){var p=pedido(LocalDateTime.of(2026,11,14,8,0));var linha=service.prever(p).getFirst();assertThat(linha.inicio()).isEqualTo(LocalDateTime.of(2026,11,16,8,0));assertThat(linha.observacao()).contains("expediente").doesNotContain("Aguardando planejamento");}
 @Test void planejamentoDaMesmaImpressoraExplicaEspera(){var p=pedido(LocalDateTime.of(2026,11,16,8,0));var plano=new PlanejamentoProducao();plano.setFim(LocalDateTime.of(2026,11,17,10,0));when(planos.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(p.getItens().getFirst().getImpressoraId())).thenReturn(Optional.of(plano));var linha=service.prever(p).getFirst();assertThat(linha.inicio()).isEqualTo(plano.getFim().plusMinutes(10));assertThat(linha.observacao()).contains("17/11/2026 10:00","planejamento pendente");}
 @Test void dataPassadaExplicaAjusteParaAgora(){var linha=service.prever(pedido(LocalDateTime.of(2026,11,10,8,0))).getFirst();assertThat(linha.inicio()).isEqualTo(LocalDateTime.now(clock));assertThat(linha.observacao()).contains("já passou");}
}
