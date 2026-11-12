package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CatalogoBuscaService {

    private final PecaRepository pecaRepository;
    private final ImpressoraRepository impressoraRepository;
    private final FilamentoRepository filamentoRepository;
    private final EmbalagemRepository embalagemRepository;
    private final CalculoHistoricoRepository calculoHistoricoRepository;

    public CatalogoBuscaService(
            PecaRepository pecaRepository,
            ImpressoraRepository impressoraRepository,
            FilamentoRepository filamentoRepository,
            EmbalagemRepository embalagemRepository,
            CalculoHistoricoRepository calculoHistoricoRepository) {

        this.pecaRepository = pecaRepository;
        this.impressoraRepository = impressoraRepository;
        this.filamentoRepository = filamentoRepository;
        this.embalagemRepository = embalagemRepository;
        this.calculoHistoricoRepository = calculoHistoricoRepository;
    }

    public Page<Peca> buscarPecas(String nome, Pageable pageable) {
        return pecaRepository.findByNomeContainingIgnoreCaseAndArquivadoFalse(nome, pageable);
    }

    public Page<Impressora> buscarImpressoras(String nome, Pageable pageable) {
        return impressoraRepository.findByNomeContainingIgnoreCaseAndArquivadoFalse(nome, pageable);
    }

    public Page<Filamento> buscarFilamentos(String nome, Pageable pageable) {
        return filamentoRepository.findByNomeContainingIgnoreCaseAndArquivadoFalse(nome, pageable);
    }

    public Page<Embalagem> buscarEmbalagens(String nome, Pageable pageable) {
        return embalagemRepository.findByNomeContainingIgnoreCaseAndArquivadoFalse(nome, pageable);
    }

    public Page<HistoricoListaResponse> buscarHistorico(
            String nome,
            LocalDate data,
            String plataforma,
            String produzido,
            String vendido,
            Pageable pageable) {
        return buscarHistorico(nome, data, plataforma, produzido, vendido, "", pageable);
    }

    public Page<HistoricoListaResponse> buscarHistorico(
            String nome,
            LocalDate data,
            String plataforma,
            String produzido,
            String vendido,
            String statusLote,
            Pageable pageable) {

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
                statusLote == null ? "" : statusLote,
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
