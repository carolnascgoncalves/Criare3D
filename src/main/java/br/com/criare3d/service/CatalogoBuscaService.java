package br.com.criare3d.service;

import br.com.criare3d.models.*;
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
        return pecaRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Page<Impressora> buscarImpressoras(String nome, Pageable pageable) {
        return impressoraRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Page<Filamento> buscarFilamentos(String nome, Pageable pageable) {
        return filamentoRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Page<Embalagem> buscarEmbalagens(String nome, Pageable pageable) {
        return embalagemRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Page<CalculoHistorico> buscarHistorico(String nome, LocalDate data, Pageable pageable) {

        boolean temNome = nome != null && !nome.isBlank();

        if (data == null && !temNome) {
            return calculoHistoricoRepository.findAll(pageable);
        }

        if (data == null) {
            return calculoHistoricoRepository.findByNomePecaContainingIgnoreCase(nome.trim(), pageable);
        }

        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();

        if (!temNome) {
            return calculoHistoricoRepository.findByDataCalculoGreaterThanEqualAndDataCalculoLessThan(
                    inicio, fim, pageable);
        }

        return calculoHistoricoRepository.findByNomePecaContainingIgnoreCaseAndDataCalculoGreaterThanEqualAndDataCalculoLessThan(nome.trim(), inicio, fim, pageable);
    }
}