package br.com.criare3d.dto;

import br.com.criare3d.models.enums.TipoMovimentacao;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record FiltroMovimentacao(LocalDate inicio, LocalDate fim, String tipo, String direcao,
                                String busca, UUID filamento, boolean saldo, boolean tudo) {
    public FiltroMovimentacao {
        Objects.requireNonNull(inicio);
        Objects.requireNonNull(fim);
        if (!tudo && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        }
        tipo = TipoMovimentacao.valido(tipo) ? tipo : "";
        direcao = "entrada".equals(direcao) || "saida".equals(direcao) ? direcao : "";
        busca = busca == null ? "" : busca.strip();
    }
}
