package br.com.criare3d.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/** Cria paginação usando apenas opções de ordenação definidas pelo servidor. */
public final class OrdenacaoPaginada {

    private OrdenacaoPaginada() {
    }

    public static String validarOrdem(String ordem, String padrao, Map<String, Sort> opcoes) {
        return opcoes.containsKey(ordem) ? ordem : padrao;
    }

    public static Pageable aplicar(Pageable pageable, String ordem, Map<String, Sort> opcoes) {
        Sort sort = opcoes.get(ordem).and(Sort.by(Sort.Order.asc("id")));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
