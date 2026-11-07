package br.com.criare3d.service;

import br.com.criare3d.dto.ManutencaoImpressoraRequest;
import br.com.criare3d.dto.ResumoManutencaoImpressora;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.ManutencaoImpressora;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.ImpressoraRepository;
import br.com.criare3d.repository.ManutencaoImpressoraRepository;
import br.com.criare3d.service.interfaces.ManutencaoImpressoraService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ManutencaoImpressoraServiceImp implements ManutencaoImpressoraService {

    private final ManutencaoImpressoraRepository manutencaoRepository;
    private final ImpressoraRepository impressoraRepository;
    private final CalculoHistoricoRepository historicoRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    public ManutencaoImpressoraServiceImp(
            ManutencaoImpressoraRepository manutencaoRepository,
            ImpressoraRepository impressoraRepository,
            CalculoHistoricoRepository historicoRepository,
            UsuarioLogadoService usuarioLogadoService
    ) {
        this.manutencaoRepository = manutencaoRepository;
        this.impressoraRepository = impressoraRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    @Override
    public java.util.Optional<ManutencaoImpressora> registrar(UUID impressoraId, ManutencaoImpressoraRequest request) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        java.util.Optional<Impressora> encontrada = impressoraRepository.findByIdAndUsuario_Id(impressoraId, usuarioId);
        if (encontrada.isEmpty()) return java.util.Optional.empty();
        ManutencaoImpressora registro = ManutencaoImpressora.builder()
                .descricao(request.getDescricao().trim())
                .valor(request.getValor())
                .data(request.getData())
                .impressora(encontrada.get())
                .build();
        return java.util.Optional.of(manutencaoRepository.save(registro));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<ResumoManutencaoImpressora> resumo(UUID impressoraId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        java.util.Optional<Impressora> encontrada = impressoraRepository.findByIdAndUsuario_Id(impressoraId, usuarioId);
        if (encontrada.isEmpty()) return java.util.Optional.empty();

        Impressora impressora = encontrada.get();
        BigDecimal total = manutencaoRepository.somarValoresPorImpressoraEUsuario(impressoraId, usuarioId);
        Long minutos = historicoRepository.somarMinutosPorImpressoraEUsuario(impressoraId, usuarioId);
        if (total == null) total = BigDecimal.ZERO;
        if (minutos == null) minutos = 0L;

        BigDecimal custoPorHora = BigDecimal.ZERO;
        if (total.signum() > 0 && impressora.getVidaUtilHoras() != null
                && impressora.getVidaUtilHoras().signum() > 0) {
            custoPorHora = total.divide(impressora.getVidaUtilHoras(), 8, RoundingMode.HALF_UP);
        }

        return java.util.Optional.of(new ResumoManutencaoImpressora(
                impressora,
                manutencaoRepository.findAllByImpressora_IdOrderByDataDescIdDesc(impressoraId),
                total,
                minutos,
                custoPorHora));
    }

    @Override
    public void excluir(UUID impressoraId, UUID manutencaoId) {
        buscarImpressoraDoUsuario(impressoraId);
        manutencaoRepository.findByIdAndImpressora_Id(manutencaoId, impressoraId)
                .ifPresent(manutencaoRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public long contar(UUID impressoraId) {
        buscarImpressoraDoUsuario(impressoraId);
        return manutencaoRepository.countByImpressora_Id(impressoraId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal custoPorHora(UUID impressoraId) {
        Impressora impressora = buscarImpressoraDoUsuario(impressoraId);
        BigDecimal vidaUtilHoras = impressora.getVidaUtilHoras();
        if (vidaUtilHoras == null || vidaUtilHoras.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal total = manutencaoRepository.somarValoresPorImpressoraEUsuario(
                impressoraId, usuarioLogadoService.buscarIdUsuarioAtual());
        if (total == null || total.signum() <= 0) return BigDecimal.ZERO;

        // Distribui os gastos de manutenção pela vida útil prevista da impressora.
        // Dividir apenas pelas horas já produzidas fazia uma manutenção recente
        // recair quase inteira sobre cada peça quando havia pouco histórico.
        return total.divide(vidaUtilHoras, 8, RoundingMode.HALF_UP);
    }

    private Impressora buscarImpressoraDoUsuario(UUID impressoraId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return impressoraRepository.findByIdAndUsuario_Id(impressoraId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Impressora não encontrada."));
    }
}
