package br.com.criare3d.dto;

import br.com.criare3d.models.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CatalogoOpcaoMapper {
    private CatalogoOpcaoMapper() {}

    public static Map<String, Object> de(Object entidade) {
        Map<String, Object> dados = new LinkedHashMap<>(), atributos = new LinkedHashMap<>();
        if (entidade instanceof Peca p) {
            dados.put("id", p.getId());
            dados.put("nome", p.getNome());
            atributos.put("gramas", p.getQuantidadeFilamentoGramas());
            atributos.put("tempo", p.getTempoImpressaoMinutos());
            atributos.put("temArquivo", p.getDadosArquivo3mf() != null && !p.getDadosArquivo3mf().isBlank());
        }
        if (entidade instanceof Filamento f) {
            dados.put("id", f.getId());
            dados.put("nome", f.getNome() + " · " + f.getCor());
            atributos.put("cor", f.getCor());
            atributos.put("codigoCor", f.getCodigoCor());
            atributos.put("precoRolo", f.getPrecoRolo());
            atributos.put("pesoRolo", f.getPesoRoloGramas());
            atributos.put("estoque", f.getEstoqueGramas());
        }
        if (entidade instanceof Impressora i) {
            dados.put("id", i.getId());
            dados.put("nome", i.getNome());
            atributos.put("potencia", i.getPotenciaWatts());
            atributos.put("tempoAquecimento", i.getTempoAquecimentoMinutos());
            atributos.put("modelo", i.getModelo());
        }
        if (entidade instanceof Embalagem e) {
            dados.put("id", e.getId());
            dados.put("nome", e.getNome());
            atributos.put("custo", e.getCustoUnitario());
        }
        if (entidade instanceof ProdutoLoja p) {
            dados.put("id", p.getId());
            dados.put("nome", p.getNome());
            atributos.put("preco", p.getPreco());
        }
        if (entidade instanceof Comprador c) {
            dados.put("id", c.getId());
            dados.put("nome", c.getNome());
            atributos.put("telefone", c.getTelefone());
            atributos.put("email", c.getEmail());
        }
        atributos.values().removeIf(Objects::isNull);
        dados.put("atributos", atributos);
        return dados;
    }
}
