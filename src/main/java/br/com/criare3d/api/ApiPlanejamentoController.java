package br.com.criare3d.api;

import br.com.criare3d.models.PlanejamentoProducao;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.IdempotenciaService;
import br.com.criare3d.service.PlanejamentoProducaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/planejamentos")
@RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth")
@Transactional(readOnly=true)
public class ApiPlanejamentoController {
    private final PlanejamentoProducaoRepository planos;
    private final ImpressoraRepository impressoras;
    private final ItemPedidoRepository itens;
    private final PlanejamentoProducaoService service;
    private final IdempotenciaService operacoes;

    public record Trabalho(UUID id, UUID loteId, UUID pedidoId, UUID impressoraId, String impressora,
                           String nome, LocalDateTime inicio, LocalDateTime fim) {}
    public record Maquina(UUID id, String nome) {}
    public record Agenda(LocalDate inicio, List<Maquina> impressoras, List<Trabalho> trabalhos) {}
    public record Novo(UUID calculoId, int unidades, int capacidade, LocalDateTime inicio, int intervalo, int folga) {}

    @GetMapping
    public Agenda semana(@RequestParam(required=false) LocalDate inicio, @RequestParam(defaultValue="false") boolean concluidos) {
        if (inicio == null) {
            inicio = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(5);
            if (concluidos) inicio = planos.findByCanceladoFalseAndConcluidoTrueOrderByInicioDesc(PageRequest.of(0,1))
                    .stream().findFirst().map(p -> p.getInicio().toLocalDate().minusDays(2)).orElse(inicio);
        }
        var semana = planos.naSemana(inicio.atStartOfDay(), inicio.plusDays(7).atStartOfDay(), concluidos);
        var pedidos = new HashMap<UUID,UUID>();
        if (!semana.isEmpty()) for (var par : itens.vinculosPlanejamento(semana.stream().map(PlanejamentoProducao::getId).toList()))
            pedidos.put((UUID)par[0], (UUID)par[1]);
        var maquinas = impressoras.findByArquivadoFalse(Pageable.unpaged()).stream()
                .map(m -> new Maquina(m.getId(),m.getNome())).sorted(Comparator.comparing(Maquina::nome,String.CASE_INSENSITIVE_ORDER)).toList();
        return new Agenda(inicio, maquinas, semana.stream().map(p -> new Trabalho(p.getId(),p.getCalculo().getId(),
                pedidos.get(p.getId()),p.getImpressora().getId(),p.getNomeImpressora(),p.getNomePeca(),p.getInicio(),p.getFim())).toList());
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public LocalDate criar(@RequestParam UUID chave, @RequestBody Novo n) {
        if (n.calculoId()==null) throw new IllegalArgumentException("Escolha um lote.");
        var id = operacoes.executar(chave,"PLANEJAR_PRODUCAO",n.toString(),() ->
                service.salvar(n.calculoId(),n.unidades(),n.capacidade(),n.inicio(),n.intervalo(),n.folga()).getId().toString());
        return planos.findById(UUID.fromString(id)).orElseThrow().getInicio().toLocalDate();
    }
}
