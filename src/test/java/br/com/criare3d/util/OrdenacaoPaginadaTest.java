package br.com.criare3d.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenacaoPaginadaTest {
    @Test
    void limitaTamanhoDePaginaA50MesmoQuandoParametroEGrande() {
        var resultado = OrdenacaoPaginada.aplicar(
                PageRequest.of(3, 100_000), "nome", Map.of("nome", Sort.by("nome")));

        assertEquals(3, resultado.getPageNumber());
        assertEquals(50, resultado.getPageSize());
        assertTrue(resultado.getSort().getOrderFor("id").isAscending());
    }

    @Test
    void preservaTamanhoNormalDePagina() {
        var resultado = OrdenacaoPaginada.aplicar(
                PageRequest.of(0, 20), "nome", Map.of("nome", Sort.by("nome")));

        assertEquals(20, resultado.getPageSize());
    }
}
