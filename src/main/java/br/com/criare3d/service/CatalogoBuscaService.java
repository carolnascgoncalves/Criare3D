package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CatalogoBuscaService {

    private final PecaRepository pecaRepository;
    private final ImpressoraRepository impressoraRepository;
    private final FilamentoRepository filamentoRepository;
    private final EmbalagemRepository embalagemRepository;
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    public CatalogoBuscaService(
            PecaRepository pecaRepository,
            ImpressoraRepository impressoraRepository,
            FilamentoRepository filamentoRepository,
            EmbalagemRepository embalagemRepository,
            CalculoHistoricoRepository calculoHistoricoRepository,
            UsuarioLogadoService usuarioLogadoService) {

        this.pecaRepository = pecaRepository;
        this.impressoraRepository = impressoraRepository;
        this.filamentoRepository = filamentoRepository;
        this.embalagemRepository = embalagemRepository;
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    public Page<Peca> buscarPecas(String nome, Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return pecaRepository.findByNomeContainingIgnoreCaseAndUsuario_Id(
                nome, usuarioId, pageable
        );
    }

    public Page<Impressora> buscarImpressoras(String nome, Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return impressoraRepository.findByNomeContainingIgnoreCaseAndUsuario_Id(
                nome, usuarioId, pageable
        );
    }

    public Page<Filamento> buscarFilamentos(String nome, Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return filamentoRepository.findByNomeContainingIgnoreCaseAndUsuario_Id(
                nome, usuarioId, pageable
        );
    }

    public Page<Embalagem> buscarEmbalagens(String nome, Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return embalagemRepository.findByNomeContainingIgnoreCaseAndUsuario_Id(
                nome, usuarioId, pageable
        );
    }

    public Page<CalculoHistorico> buscarHistorico(
            String nome, LocalDate data, Pageable pageable) {

        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        boolean temNome = nome != null && !nome.isBlank();

        if (data == null && !temNome) {
            return calculoHistoricoRepository.findAllByUsuario_Id(
                    usuarioId, pageable
            );
        }

        if (data == null) {
            return calculoHistoricoRepository
                    .findByNomePecaContainingIgnoreCaseAndUsuario_Id(
                            nome.trim(), usuarioId, pageable
                    );
        }

        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();

        if (!temNome) {
            return calculoHistoricoRepository
                    .findByDataCalculoGreaterThanEqualAndDataCalculoLessThanAndUsuario_Id(
                            inicio, fim, usuarioId, pageable
                    );
        }

        return calculoHistoricoRepository
                .findByNomePecaContainingIgnoreCaseAndDataCalculoGreaterThanEqualAndDataCalculoLessThanAndUsuario_Id(
                        nome.trim(), inicio, fim, usuarioId, pageable
                );
    }
}