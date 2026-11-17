package br.com.criare3d.controller;

import br.com.criare3d.models.*;
import br.com.criare3d.dto.CatalogoOpcaoMapper;
import br.com.criare3d.service.CatalogoModelosImpressora;
import br.com.criare3d.service.EstimativaUsoImpressoraService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/catalogo-opcoes")
@Transactional(readOnly = true)
public class CatalogoOpcoesController {
    @PersistenceContext
    private EntityManager em;
    @Autowired
    private CatalogoModelosImpressora modelosImpressora;

    @Autowired
    private EstimativaUsoImpressoraService estimativaUso;

    @GetMapping("/impressora/estimativa-uso")
    public EstimativaUsoImpressoraService.Estimativa estimativaUso(@RequestParam String modelo,
                                                                  @RequestParam(required = false) UUID impressoraId) {
        return estimativaUso.estimar(modelo, impressoraId);
    }

    @GetMapping("/modelos-impressora")
    public List<CatalogoModelosImpressora.Modelo> modelosImpressora() {
        return modelosImpressora.listar();
    }

    @ModelAttribute
    public void semCache(jakarta.servlet.http.HttpServletResponse resposta) {
        resposta.setHeader("Cache-Control", "no-store");
    }

    public record Opcoes(List<Map<String, Object>> itens, boolean mais) {
    }

    @GetMapping
    public Opcoes buscar(@RequestParam String tipo, @RequestParam(defaultValue = "") String busca, @RequestParam(defaultValue = "0") int page) {
        Class<?> classe = switch (tipo) {
            case "peca" -> Peca.class;
            case "filamento" -> Filamento.class;
            case "impressora" -> Impressora.class;
            case "embalagem" -> Embalagem.class;
            case "produto" -> ProdutoLoja.class;
            case "comprador" -> Comprador.class;
            default -> throw new IllegalArgumentException("Catálogo inválido.");
        };
        if (page < 0 || page > 100000 || busca.length() > 150) throw new IllegalArgumentException("Busca inválida.");
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Object.class);
        var entidade = query.from(classe);
        String termo = "%" + busca.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        var filtro = cb.like(cb.lower(entidade.get("nome")), termo, '\\');
        if (tipo.equals("filamento")) filtro = cb.or(filtro, cb.like(cb.lower(entidade.get("cor")), termo, '\\'));
        query.select(entidade).where(cb.isFalse(entidade.get("arquivado")), filtro).orderBy(cb.asc(entidade.get("nome")), cb.asc(entidade.get("id")));
        var resultados = em.createQuery(query).setFirstResult(page * 20).setMaxResults(21).getResultList();
        return new Opcoes(resultados.stream().limit(20).map(CatalogoOpcaoMapper::de).toList(), resultados.size() > 20);
    }

}
