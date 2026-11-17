package br.com.criare3d.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Valores ainda incompletos do formulário; nunca são usados como custos confiáveis. */
public record RascunhoFormulario(@Size(max = 80) Map<String, Object> campos,
                                @Size(max = 100) List<@Valid Material> filamentos,
                                @Min(1) @Max(4) int etapa, Long atualizadoEm) {
    public record Material(@Size(max = 36) String filamentoId, @Size(max = 80) String quantidade,
                           boolean confirmado, boolean confirmadoPeloUsuario) {
    }

    public RascunhoFormulario {
        campos = campos == null ? Map.of() : Map.copyOf(campos);
        filamentos = filamentos == null ? List.of() : List.copyOf(filamentos);
        campos.forEach((nome, valor) -> {
            if (nome.length() > 80 || !(valor instanceof String || valor instanceof Boolean || valor instanceof Number)
                || valor.toString().length() > 500) throw new IllegalArgumentException("Rascunho com campos inválidos.");
        });
    }
}
