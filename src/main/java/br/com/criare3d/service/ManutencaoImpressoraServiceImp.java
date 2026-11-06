package br.com.criare3d.service;

import br.com.criare3d.dto.ManutencaoImpressoraRequest;
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
    @Transactional(readOnly = true)
    public List<ManutencaoImpressora> listar(UUID impressoraId) {
        buscarImpressoraDoUsuario(impressoraId);
        return manutencaoRepository.findAllByImpressora_IdOrderByDataDescIdDesc(impressoraId);
    }

    @Override
    public ManutencaoImpressora registrar(UUID impressoraId, ManutencaoImpressoraRequest request) {
        Impressora impressora = buscarImpressoraDoUsuario(impressoraId);
        ManutencaoImpressora registro = ManutencaoImpressora.builder()
                .descricao(request.getDescricao().trim())
                .valor(request.getValor())
                .data(request.getData())
                .impressora(impressora)
                .build();
        return manutencaoRepository.save(registro);
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
    public BigDecimal totalGasto(UUID impressoraId) {
        buscarImpressoraDoUsuario(impressoraId);
        return manutencaoRepository.somarValoresPorImpressoraEUsuario(
                impressoraId, usuarioLogadoService.buscarIdUsuarioAtual());
    }

    @Override
    @Transactional(readOnly = true)
    public long minutosImpressos(UUID impressoraId) {
        buscarImpressoraDoUsuario(impressoraId);
        Long minutos = historicoRepository.somarMinutosPorImpressoraEUsuario(
                impressoraId, usuarioLogadoService.buscarIdUsuarioAtual());
        return minutos == null ? 0L : minutos;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal custoPorHora(UUID impressoraId, BigDecimal fallbackManual) {
        BigDecimal total = totalGasto(impressoraId);
        long minutos = minutosImpressos(impressoraId);
        if (total.signum() <= 0 || minutos <= 0) {
            return fallbackManual == null ? BigDecimal.ZERO : fallbackManual;
        }
        BigDecimal horas = BigDecimal.valueOf(minutos)
                .divide(BigDecimal.valueOf(60), 10, RoundingMode.HALF_UP);
        return total.divide(horas, 8, RoundingMode.HALF_UP);
    }

    private Impressora buscarImpressoraDoUsuario(UUID impressoraId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return impressoraRepository.findByIdAndUsuario_Id(impressoraId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Impressora não encontrada."));
    }
}
