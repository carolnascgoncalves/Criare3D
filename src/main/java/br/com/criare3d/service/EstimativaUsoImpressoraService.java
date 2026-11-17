package br.com.criare3d.service;

import br.com.criare3d.repository.CalculoHistoricoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/** Estimativas editáveis de uso, independentes da consulta de preços do marketplace. */
@Service
@Transactional(readOnly = true)
public class EstimativaUsoImpressoraService {
    private final CalculoHistoricoRepository historico;
    private final Clock clock;

    public EstimativaUsoImpressoraService(CalculoHistoricoRepository historico, Clock clock) {
        this.historico = historico;
        this.clock = clock;
    }

    public record Estimativa(BigDecimal horasAteSubstituicao, BigDecimal horasMensais, boolean horasMensaisPadrao) {}

    public Estimativa estimar(String modelo, UUID impressoraId) {
        if (modelo == null || modelo.isBlank() || modelo.length() > 150) {
            throw new IllegalArgumentException("Informe um modelo de até 150 caracteres.");
        }
        var desde = LocalDateTime.now(clock).minusDays(30);
        Long minutos = impressoraId == null ? historico.somarMinutosMaquinaProduzidosDesde(desde)
            : historico.somarMinutosMaquinaProduzidosDaImpressoraDesde(desde, impressoraId);
        boolean padrao = minutos == null || minutos <= 0;
        var mensal = padrao ? new BigDecimal("100.00")
            : BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        String normalizado = modelo.toLowerCase(Locale.ROOT);
        var horas = new BigDecimal(normalizado.contains("mini") || normalizado.matches(".*\\ba1\\b.*")
            ? "3000" : "5000");
        return new Estimativa(horas, mensal, padrao);
    }
}
