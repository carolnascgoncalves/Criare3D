package br.com.criare3d.controller;
import br.com.criare3d.models.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@RestController @RequestMapping("/catalogo-opcoes") @Transactional(readOnly=true)
public class CatalogoOpcoesController  {
    @PersistenceContext private EntityManager em;
    @ModelAttribute public void semCache(jakarta.servlet.http.HttpServletResponse resposta) {
        resposta.setHeader("Cache-Control","no-store");
    }
    public record Opcoes(List<Map<String,Object>> itens,boolean mais)  {
    }
    @GetMapping public Opcoes buscar(@RequestParam String tipo,@RequestParam(defaultValue="")String busca,@RequestParam(defaultValue="0")int page)  {
        Class<?> classe=switch(tipo) {
            case "peca"->Peca.class;
            case "filamento"->Filamento.class;
            case "impressora"->Impressora.class;
            case "embalagem"->Embalagem.class;
            case "produto"->ProdutoLoja.class;
            case "comprador"->Comprador.class;
            default->throw new IllegalArgumentException("Catálogo inválido.");
        }
        ;
        if(page<0||page>100000||busca.length()>150)throw new IllegalArgumentException("Busca inválida.");
        var cb=em.getCriteriaBuilder();
        var query=cb.createQuery(Object.class);
        var entidade=query.from(classe);
        String termo="%"+busca.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        var filtro=cb.like(cb.lower(entidade.get("nome")),termo,'\\');
        if(tipo.equals("filamento"))filtro=cb.or(filtro,cb.like(cb.lower(entidade.get("cor")),termo,'\\'));
        query.select(entidade).where(cb.isFalse(entidade.get("arquivado")),filtro).orderBy(cb.asc(entidade.get("nome")),cb.asc(entidade.get("id")));
        var resultados=em.createQuery(query).setFirstResult(page*20).setMaxResults(21).getResultList();
        return new Opcoes(resultados.stream().limit(20).map(this::opcao).toList(),resultados.size()>20);
    }
    private Map<String,Object> opcao(Object entidade)  {
        Map<String,Object> dados=new LinkedHashMap<>(),atributos=new LinkedHashMap<>();
        if(entidade instanceof Peca p) {
            dados.put("id",p.getId());
            dados.put("nome",p.getNome());
            atributos.put("gramas",p.getQuantidadeFilamentoGramas());
            atributos.put("tempo",p.getTempoImpressaoMinutos());
            atributos.put("temArquivo",p.getDadosArquivo3mf()!=null&&!p.getDadosArquivo3mf().isBlank());
        }
        if(entidade instanceof Filamento f) {
            dados.put("id",f.getId());
            dados.put("nome",f.getNome()+" · "+f.getCor()+" · R$ "+f.getPrecoRolo()+" / "+f.getPesoRoloGramas()+" g");
            atributos.put("cor",f.getCor());
            atributos.put("codigoCor",f.getCodigoCor());
            atributos.put("precoRolo",f.getPrecoRolo());
            atributos.put("pesoRolo",f.getPesoRoloGramas());
            atributos.put("estoque",f.getEstoqueGramas());
        }
        if(entidade instanceof Impressora i) {
            dados.put("id",i.getId());
            dados.put("nome",i.getNome());
            atributos.put("potencia",i.getPotenciaWatts());
            atributos.put("tempoAquecimento",i.getTempoAquecimentoMinutos());
            atributos.put("modelo",i.getModelo());
        }
        if(entidade instanceof Embalagem e) {
            dados.put("id",e.getId());
            dados.put("nome",e.getNome()+" · R$ "+e.getCustoUnitario());
        }
        if(entidade instanceof ProdutoLoja p) {
            dados.put("id",p.getId());
            dados.put("nome",p.getNome());
        }
        if(entidade instanceof Comprador c) {
            dados.put("id",c.getId());
            dados.put("nome",c.getNome());
            atributos.put("telefone",c.getTelefone());
            atributos.put("email",c.getEmail());
        }
        atributos.values().removeIf(Objects::isNull);
        dados.put("atributos",atributos);
        return dados;
    }
}
