package br.com.criare3d.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long total) {
    public PaginaResponse {
        itens = List.copyOf(itens);
    }

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements());
    }

    public static PageRequest pedido(int pagina, int tamanho, Sort ordem) {
        if (pagina < 0 || pagina > 100000 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Paginação inválida.");
        return PageRequest.of(pagina, tamanho, ordem);
    }
}
