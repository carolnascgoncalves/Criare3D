package br.com.criare3d.service;

import br.com.criare3d.dto.FiltroMovimentacao;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Movimentacao;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.repository.MovimentacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MovimentacaoService {
    public static final int LIMITE_CSV = 5000;
    @Autowired
    private MovimentacaoRepository movimentos;
    @Autowired
    private UsuarioLogadoService usuario;
    @Autowired
    private Clock clock;
    @Autowired
    private FilamentoRepository filamentos;

    public record Extrato(Page<Movimentacao> pagina, List<MovimentacaoRepository.ResumoMedida> resumo,
                          MovimentacaoRepository.SaldoAbertura abertura, Filamento filamentoSelecionado) {
        public BigDecimal pecasVendidas() {
            return resumo.stream().map(MovimentacaoRepository.ResumoMedida::getPecasVendidas)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public record Exportacao(List<Movimentacao> linhas, Map<UUID, String> nomesFilamentos, boolean limitada) {
    }

    @Transactional(readOnly = true)
    public Extrato extrato(FiltroMovimentacao filtro, Pageable pageable) {
        var pagina = consultar(filtro, pageable);
        var inicio = filtro.inicio().atStartOfDay();
        var fim = filtro.fim().plusDays(1).atStartOfDay();
        var resumo = movimentos.resumo(inicio, fim, filtro.tipo(), filtro.direcao(), filtro.saldo(),
            filtro.busca(), filtro.filamento(), filtro.tudo());
        var abertura = filtro.saldo() ? null : movimentos.saldoAbertura(inicio, fim,
            filtro.filamento(), filtro.busca(), filtro.tudo());
        var selecionado = filtro.filamento() == null ? null : filamentos.findById(filtro.filamento()).orElseThrow();
        return new Extrato(pagina, resumo, abertura, selecionado);
    }

    @Transactional(readOnly = true)
    public Exportacao exportar(FiltroMovimentacao filtro) {
        var pagina = consultar(filtro, PageRequest.of(0, LIMITE_CSV, Sort.by(Sort.Direction.DESC, "data", "id")));
        var ids = pagina.getContent().stream().map(Movimentacao::getFilamentoId)
            .filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> nomes = ids.isEmpty() ? Map.of() : filamentos.findAllById(ids).stream()
            .collect(Collectors.toMap(Filamento::getId, f -> f.getNome() + " · " + f.getCor()));
        return new Exportacao(pagina.getContent(), nomes, pagina.getTotalElements() > LIMITE_CSV);
    }

    public Map<UUID, String> coresDosFilamentos(List<Movimentacao> linhas) {
        var ids = linhas.stream().map(m -> m.getFilamentoId() != null ? m.getFilamentoId()
                : "filamento".equals(m.getOrigem()) ? m.getReferencia() : null)
                .filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : filamentos.findAllByIdIn(ids).stream()
                .filter(f -> f.getCodigoCor() != null)
                .collect(Collectors.toMap(Filamento::getId, Filamento::getCodigoCor));
    }

    private Page<Movimentacao> consultar(FiltroMovimentacao filtro, Pageable pageable) {
        return movimentos.extrato(filtro.inicio().atStartOfDay(), filtro.fim().plusDays(1).atStartOfDay(),
            filtro.tipo(), filtro.direcao(), filtro.saldo(), filtro.busca(), filtro.filamento(), filtro.tudo(), pageable);
    }

    public void registrar(String tipo, String descricao, BigDecimal quantidade, String medida, String origem, UUID referencia) {
        registrar(tipo, descricao, quantidade, medida, origem, referencia, null, null);
    }

    public void registrar(String tipo, String descricao, BigDecimal quantidade, String medida, String origem, UUID referencia, UUID filamentoId, BigDecimal saldoApos) {
        if (quantidade == null || quantidade.signum() == 0) return;
        var m = new Movimentacao();
        m.setTipo(tipo);
        m.setDescricao(descricao.substring(0, Math.min(180, descricao.length())));
        m.setQuantidade(quantidade);
        m.setMedida(medida);
        m.setOrigem(origem);
        m.setReferencia(referencia);
        m.setData(LocalDateTime.now(clock));
        m.setUsuarioId(usuario.buscarIdUsuarioAtual());
        m.setFilamentoId(filamentoId);
        m.setSaldoApos(saldoApos);
        movimentos.save(m);
    }
}
