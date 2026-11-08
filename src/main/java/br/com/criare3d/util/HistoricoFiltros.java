package br.com.criare3d.util;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public final class HistoricoFiltros {
    public static final Map<String, Sort> ORDENACOES = Map.ofEntries(
            Map.entry("nomeAsc", Sort.by(Sort.Direction.ASC, "nomePeca")),
            Map.entry("nomeDesc", Sort.by(Sort.Direction.DESC, "nomePeca")),
            Map.entry("dataAsc", Sort.by(Sort.Direction.ASC, "dataCalculo")),
            Map.entry("dataDesc", Sort.by(Sort.Direction.DESC, "dataCalculo")),
            Map.entry("custoAsc", Sort.by(Sort.Direction.ASC, "custoTotal")),
            Map.entry("custoDesc", Sort.by(Sort.Direction.DESC, "custoTotal")),
            Map.entry("precoAsc", JpaSort.unsafe(Sort.Direction.ASC, "coalesce(precoVendaPlataforma, precoSugerido)")),
            Map.entry("precoDesc", JpaSort.unsafe(Sort.Direction.DESC, "coalesce(precoVendaPlataforma, precoSugerido)"))
    );

    private static final Set<String> PLATAFORMAS = Set.of(
            "", "SHOPEE", "TIKTOK", "MERCADO_LIVRE", "SEM_PLATAFORMA");

    private HistoricoFiltros() { }

    public static String normalizarPlataforma(String plataforma) {
        String valor = plataforma == null ? "" : plataforma.trim().toUpperCase();
        return PLATAFORMAS.contains(valor) ? valor : "";
    }

    public static String normalizarStatus(String status) {
        if (status == null) return "TODOS";
        String valor = status.trim().toUpperCase();
        return "SIM".equals(valor) || "NAO".equals(valor) ? valor : "TODOS";
    }

    public static ModelAndView redirecionarComFiltros(
            String tipoMensagem, String mensagem, String busca, LocalDate data,
            String plataforma, String produzido, String vendido, String ordem, int page) {
        ModelAndView mv = new ModelAndView("redirect:/historico/listar");
        mv.addObject("busca", busca == null ? "" : busca);
        if (data != null) mv.addObject("data", data.toString());
        mv.addObject("plataforma", normalizarPlataforma(plataforma));
        mv.addObject("produzido", normalizarStatus(produzido));
        mv.addObject("vendido", normalizarStatus(vendido));
        mv.addObject("ordem", OrdenacaoPaginada.validarOrdem(ordem, "dataDesc", ORDENACOES));
        mv.addObject("page", Math.max(page, 0));
        mv.addObject(tipoMensagem, mensagem);
        return mv;
    }
}
