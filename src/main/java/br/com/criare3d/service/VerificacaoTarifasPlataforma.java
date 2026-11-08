package br.com.criare3d.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class VerificacaoTarifasPlataforma {
    private static final Logger log = LoggerFactory.getLogger(VerificacaoTarifasPlataforma.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${criare.tarifas.conferidas-em:}")
    private String conferidasEm;

    @PostConstruct
    void avisarSePrecisaConferir() {
        var data = dataConferencia();
        if (data.isEmpty()) {
            log.warn("Data de conferência das tarifas das plataformas não configurada.");
        } else if (data.get().isBefore(LocalDate.now().minusDays(90))) {
            log.warn("As tarifas das plataformas foram conferidas há mais de 90 dias.");
        }
    }

    public String dataExibicao() {
        return dataConferencia().map(data -> data.format(FORMATO)).orElse("não informada");
    }

    public boolean precisaConferir() {
        var data = dataConferencia();
        return data.isEmpty() || data.get().isBefore(LocalDate.now().minusDays(90));
    }

    private java.util.Optional<LocalDate> dataConferencia() {
        if (conferidasEm == null || conferidasEm.isBlank()) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(LocalDate.parse(conferidasEm.trim()));
        } catch (DateTimeParseException e) {
            log.warn("A configuração criare.tarifas.conferidas-em deve usar AAAA-MM-DD.");
            return java.util.Optional.empty();
        }
    }
}
