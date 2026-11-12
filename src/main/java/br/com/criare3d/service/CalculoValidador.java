package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.models.*;
import java.math.BigDecimal;

public final class CalculoValidador {
    private CalculoValidador() {}

    public static void validarRequisicao(CalculoRequest request) {
        if (request == null || request.getPecaId() == null) throw new IllegalArgumentException("Selecione uma peça.");
        if (request.getImpressoraId() == null) throw new IllegalArgumentException("Selecione uma impressora.");
        if (request.getFilamentos() == null || request.getFilamentos().isEmpty())
            throw new IllegalArgumentException("Adicione pelo menos um filamento");
        if (request.getFilamentos().size() > 100) throw new IllegalArgumentException("Use até 100 filamentos por cálculo.");
        if (request.getFilamentos().stream().anyMatch(i -> i == null || i.getFilamentoId() == null))
            throw new IllegalArgumentException("Selecione todos os filamentos");
    }

    public static void validar(CalculoRequest request, Peca peca, Impressora impressora, Configuracao configuracao) {
        validarRequisicao(request);
        if (peca.isArquivado() || impressora.isArquivado())
            throw new IllegalArgumentException("Restaure a peça e a impressora arquivadas antes de calcular.");
        if (impressora.getPotenciaWatts() == null || impressora.getPotenciaWatts().signum() <= 0)
            throw new IllegalArgumentException("A potência da impressora deve ser maior que zero");
        if (configuracao.getTarifaEnergiaKwh() == null || configuracao.getTarifaEnergiaKwh().signum() < 0)
            throw new IllegalArgumentException("A tarifa de energia é inválida");
        if (request.getTempoImpressaoMinutos() == null || request.getTempoImpressaoMinutos() <= 0)
            throw new IllegalArgumentException("O tempo de impressão deve ser maior que zero");
        if (request.getUnidades() == null || request.getUnidades() < 1 || request.getUnidades() > 10000)
            throw new IllegalArgumentException("Informe uma quantidade entre 1 e 10.000 unidades.");
        int capacidade = request.getPecasPorImpressao() == null ? 1 : request.getPecasPorImpressao();
        if (capacidade < 1 || capacidade > 10000)
            throw new IllegalArgumentException("Informe uma capacidade de 1 a 10.000 peças por impressão.");
        if (capacidade > request.getUnidades())
            throw new IllegalArgumentException("Peças por impressão não pode ser maior que a quantidade de unidades.");
        BigDecimal markup = request.getMarkupPercentual();
        if (markup == null || markup.signum() < 0) throw new IllegalArgumentException("Selecione um markup válido");
        if (!Boolean.TRUE.equals(configuracao.getEmbalagemFixa()) && request.getEmbalagemId() == null)
            throw new IllegalArgumentException("Selecione uma embalagem");
    }
}
