package br.com.criare3d.service;

import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** Reaproveita parâmetros do cálculo, sem copiar seus estados de produção e venda. */
@Service
public class EntradaPedidoService {
    private final JsonMapper json = JsonMapper.builder().build();
    public CalculoRequest copiar(CalculoHistorico h) {
        if (h.getPeca()==null || h.getImpressora()==null) throw new IllegalArgumentException("Este cálculo antigo não tem peça ou impressora vinculada. Faça um novo cálculo.");
        CalculoRequest r = CalculoRascunhoMapper.copiar(h);
        r.setPecaId(h.getPeca().getId()); r.setImpressoraId(h.getImpressora().getId());
        r.setEmbalagemId(h.getEmbalagem()==null?null:h.getEmbalagem().getId());
        r.setCustoEmbalagemPersonalizada(h.getCustoEmbalagem());
        if (h.getPlataformaVenda()!=null) {
            try { r.setPlataformaVenda(br.com.criare3d.models.enums.PlataformaVenda.valueOf("TIKTOK_SHOP".equals(h.getPlataformaVenda()) ? "TIKTOK" : h.getPlataformaVenda())); }
            catch (IllegalArgumentException e) { r.setPlataformaVenda(br.com.criare3d.models.enums.PlataformaVenda.NENHUMA); }
        }
        List<ItemCalculoRequest> itens = new ArrayList<>();
        if (h.getItensFilamento()!=null) for (var i:h.getItensFilamento()) {
            if (i.getFilamento()==null) throw new IllegalArgumentException("Um material deste cálculo não está mais disponível.");
            var item=new ItemCalculoRequest(); item.setFilamentoId(i.getFilamento().getId()); item.setQuantidadeFilamentoGramas(i.getQuantidadeFilamentoGramas()); itens.add(item);
        }
        if (itens.isEmpty() && h.getFilamento()!=null) {
            var item=new ItemCalculoRequest(); item.setFilamentoId(h.getFilamento().getId()); item.setQuantidadeFilamentoGramas(h.getQuantidadeFilamentoGramas()); itens.add(item);
        }
        r.setFilamentos(itens);
        return r;
    }
    public String escrever(CalculoRequest entrada) {
        // Não serializa a foto temporária nem altera o rascunho da calculadora.
        CalculoRequest copia=json.readValue(json.writeValueAsString(entrada),CalculoRequest.class);
        copia.setFotoPecaBytes(null); copia.setFotoPecaContentType(null);
        String texto=json.writeValueAsString(copia);
        if (texto.length()>65536) throw new IllegalArgumentException("O cálculo ultrapassa o tamanho permitido para o orçamento.");
        return texto;
    }
    public CalculoRequest ler(String texto,int unidades) {
        CalculoRequest r=json.readValue(texto,CalculoRequest.class);
        r.setUnidades(unidades); r.setPecasPorImpressao(Math.min(unidades,Math.max(1,r.getPecasPorImpressao()==null?1:r.getPecasPorImpressao())));
        return r;
    }
}
