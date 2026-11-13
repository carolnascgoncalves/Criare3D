package br.com.criare3d.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Campos exibidos na listagem, sem carregar a foto armazenada no histórico. */
public record HistoricoListaResponse(
        UUID id,
        String nomePeca,
        LocalDateTime dataCalculo,
        String plataformaVenda,
        BigDecimal precoVendaPlataforma,
        BigDecimal precoSugerido,
        Boolean produzido,
        Boolean vendido,
        Boolean temFoto,
        Integer unidades,
        BigDecimal custoTotal,
        BigDecimal lucroLiquido,
        LocalDateTime dataVenda,
        Integer unidadesVendidas
) {
    public HistoricoListaResponse {
        if (nomePeca != null) nomePeca = org.springframework.web.util.HtmlUtils.htmlUnescape(org.springframework.web.util.HtmlUtils.htmlUnescape(nomePeca));
    }
}
