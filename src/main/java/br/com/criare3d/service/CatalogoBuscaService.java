package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.dto.HistoricoListaResponse;
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

    public Page<HistoricoListaResponse> buscarHistorico(
            String nome,
            LocalDate data,
            String plataforma,
            String produzido,
            String vendido,
            Pageable pageable) {

        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        String nomeFiltro = nome == null ? "" : nome.trim();

        boolean filtrarData = data != null;
        LocalDateTime inicio = data == null ? null : data.atStartOfDay();
        LocalDateTime fim = data == null ? null : data.plusDays(1).atStartOfDay();

        String plataformaFiltro = plataforma == null ? "" : plataforma.trim();

        String produzidoFiltro = normalizarStatus(produzido);
        String vendidoFiltro = normalizarStatus(vendido);

        boolean filtrarProduzido = !"TODOS".equals(produzidoFiltro);
        boolean valorProduzido = "SIM".equals(produzidoFiltro);

        boolean filtrarVendido = !"TODOS".equals(vendidoFiltro);
        boolean valorVendido = "SIM".equals(vendidoFiltro);

        return calculoHistoricoRepository.buscarHistoricoFiltrado(
                nomeFiltro,
                filtrarData,
                inicio,
                fim,
                plataformaFiltro,
                filtrarProduzido,
                valorProduzido,
                filtrarVendido,
                valorVendido,
                pageable
        );
    }

    private String normalizarStatus(String status) {
        if (status == null) {
            return "TODOS";
        }

        String valor = status.trim().toUpperCase();

        return switch (valor) {
            case "SIM", "NAO" -> valor;
            default -> "TODOS";
        };
    }
}
