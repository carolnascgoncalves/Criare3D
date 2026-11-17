package br.com.criare3d.controller;

import br.com.criare3d.service.MovimentacaoService;
import br.com.criare3d.util.Csv;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formato de download do site; não participa das regras do extrato. */
public final class ExtratoCsv {
    private static final DateTimeFormatter DATA_HORA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));

    private ExtratoCsv() {
    }

    public static byte[] gerar(MovimentacaoService.Exportacao exportacao) {
        var csv = new StringBuilder("\uFEFFData;Tipo;Descrição;Quantidade;Unidade;Filamento;Saldo após\r\n");
        for (var m : exportacao.linhas()) {
            var nome = m.getFilamentoId() == null ? "" :
                exportacao.nomesFilamentos().getOrDefault(m.getFilamentoId(), "Filamento removido");
            csv.append(Csv.campo(DATA_HORA.format(m.getData()))).append(';')
                .append(Csv.campo(m.getTipoInfo().getRotulo())).append(';')
                .append(Csv.campo(m.getDescricao())).append(';')
                .append(Csv.numero(m.getQuantidade().stripTrailingZeros())).append(';')
                .append(Csv.campo(m.getMedida())).append(';').append(Csv.campo(nome)).append(';')
                .append(Csv.numero(m.getSaldoApos())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
