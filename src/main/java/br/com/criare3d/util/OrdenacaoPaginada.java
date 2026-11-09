package br.com.criare3d.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/** Cria paginação usando apenas opções de ordenação definidas pelo servidor. */
public final class OrdenacaoPaginada {
    private static final int TAMANHO_MAXIMO = 50;

    private OrdenacaoPaginada() {
    }

    public static String validarOrdem(String ordem, String padrao, Map<String, Sort> opcoes) {
        return opcoes.containsKey(ordem) ? ordem : padrao;
    }

    public static Pageable aplicar(Pageable pageable, String ordem, Map<String, Sort> opcoes) {
        Sort sort = opcoes.get(ordem).and(Sort.by(Sort.Order.asc("id")));
        int tamanho = Math.max(1, Math.min(pageable.getPageSize(), TAMANHO_MAXIMO));
        return PageRequest.of(pageable.getPageNumber(), tamanho, sort);
    }

    public static int ajustarPaginaAposExclusao(int pagina, long totalAntes, int removidos, int tamanhoPagina) {
        long totalDepois = Math.max(0, totalAntes - Math.max(0, removidos));
        int tamanho = Math.max(1, tamanhoPagina);
        long ultimaPagina = totalDepois == 0 ? 0 : (totalDepois - 1) / tamanho;
        return (int) Math.max(0, Math.min(Math.max(0, pagina), ultimaPagina));
    }
}
