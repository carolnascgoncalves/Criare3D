package br.com.criare3d.api;

import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.StatusPedido;
import br.com.criare3d.repository.PedidoOrcamentoRepository;
import br.com.criare3d.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;

@RestController
@RequestMapping("/api/v1/orcamentos")
@SecurityRequirement(name="bearerAuth")
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ApiOrcamentosController {
    private final FotoPedidoService fotos;
    private final OrcamentoPedidoService service;
    private final OperacaoPedidoService operacoes;
    private final PlanejamentoPedidoService agenda;
    private final IdempotenciaService chaves;
    private final PedidoOrcamentoRepository pedidos;
    @io.swagger.v3.oas.annotations.media.Schema(name="PedidoMobileResumo")
    public record Resumo(UUID id,String codigo,String titulo,String cliente,String contato,String email,UUID compradorId,
        String situacao, StatusPedido status,boolean arquivado,boolean pago,long revisao,LocalDateTime criado,BigDecimal total){}
    @io.swagger.v3.oas.annotations.media.Schema(name="PedidoMobileLista")
    public record Lista(List<Resumo> itens,long total,int tamanho,BigDecimal receber,long prontos,long produzir){}
    @io.swagger.v3.oas.annotations.media.Schema(name="PedidoMobileItem")
    public record Item(UUID id,UUID referencia,String nome,String material,int quantidade,int estoque,long livre,BigDecimal preco,BigDecimal subtotal,boolean pronto){}
    @io.swagger.v3.oas.annotations.media.Schema(name="PedidoMobileDetalhe")
    public record Detalhe(Resumo resumo,List<Item> itens,PedidoDadosRequest dados,BigDecimal subtotal,BigDecimal desconto,BigDecimal custo,BigDecimal taxas,
        RentabilidadePedido resultado,LocalDateTime previsao,List<PlanejamentoPedidoService.Linha> linhas,List<PlanejamentoPedidoService.Ocupacao> ocupacoes,MateriaisPedido.Resumo materiais,String aviso){}
    private Resumo resumo(PedidoOrcamento p){
        return new Resumo(p.getId(),p.getCodigo(),p.getTitulo(),p.getCliente(),p.getContato(),p.getEmail(),p.getCompradorId(),p.getSituacao(),p.getStatus(),p.isArquivado(),p.getPagoEm()!=null,p.getRevisao(),p.getCriadoEm(),p.getTotal());
    }
    @GetMapping public Lista listar(@RequestParam(defaultValue="false") boolean arquivados,@RequestParam(required=false) StatusPedido status,@RequestParam(defaultValue="0") int pagina){
        var page=service.listar("",pagina,arquivados,status);
        var ativos=pedidos.findByArquivadoFalseAndStatusIn(List.of(StatusPedido.APROVADO,StatusPedido.PRONTO,StatusPedido.ENTREGUE));
        var receber=BigDecimal.ZERO;long prontos=0,produzir=0;
        for(var p:ativos){ if(p.getPagoEm()==null) receber=receber.add(p.getTotal());
            if(p.isOperacional()){if(p.getSituacao().equals(StatusPedido.PRONTO.getDescricao()))prontos++;else produzir++;}}
        return new Lista(page.stream().map(this::resumo).toList(),page.getTotalElements(),page.getSize(),receber,prontos,produzir);
    }
    @GetMapping("/pecas") public List<OrcamentoPedidoService.Referencia> pecas(){return service.referencias();}
    @GetMapping("/itens/{itemId}/miniatura")
    public org.springframework.http.ResponseEntity<ApiPainelController.FotoCatalogo> miniatura(@PathVariable UUID itemId) {
        return fotos.miniaturaDaProposta(itemId)
            .map(bytes -> org.springframework.http.ResponseEntity.ok(new ApiPainelController.FotoCatalogo("image/jpeg", Base64.getEncoder().encodeToString(bytes))))
            .orElseGet(() -> org.springframework.http.ResponseEntity.noContent().build());
    }
    @GetMapping("/{id}") public Detalhe abrir(@PathVariable UUID id){return detalhe(service.buscar(id));}
    private Detalhe detalhe(PedidoOrcamento p){
        PedidoDadosRequest d=new PedidoDadosRequest();
        org.springframework.beans.BeanUtils.copyProperties(p,d);
        d.setDescontoInformado(p.getDescontoPercentual()==null?p.getDesconto().toPlainString():p.getDescontoPercentual().stripTrailingZeros().toPlainString()+"%");
        boolean editar=p.getStatus().isEditavel()&&!p.isArquivado();
        List<PlanejamentoPedidoService.Linha> linhas=List.of();
        String aviso=null;
        if(editar)try{linhas=agenda.prever(p);}catch(IllegalArgumentException e){aviso=e.getMessage();}
        if (!editar) linhas=p.getItens().stream().filter(i -> i.getPlanejamento()!=null).map(i ->
            new PlanejamentoPedidoService.Linha(i.getId(),i.getNome(),i.getPlanejamento().getInicio(),i.getPlanejamento().getFim())).toList();
        var fim=editar?linhas.stream().map(PlanejamentoPedidoService.Linha::fim).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null):p.getPrevisao();
        MateriaisPedido.Resumo materiais=null;
        try{materiais=service.materiais(p);}catch(IllegalArgumentException e){aviso=e.getMessage();}
        return new Detalhe(resumo(p),p.getItens().stream().map(i->new Item(i.getId(),i.getLoteEstoque()==null?null:i.getLoteEstoque().getId(),i.getNome(),i.getMateriais(),i.getQuantidade(),i.getUsarEstoque(),i.getLoteEstoque()==null?0:service.disponivel(i.getLoteEstoque()),i.getPrecoUnitario(),i.getTotal(),i.isPronto())).toList(),
          d,p.getSubtotal(),p.getDesconto(),p.getCusto(),p.getTaxas(),p.getRentabilidade(),fim,linhas,editar?agenda.ocupacoes(p):List.of(),materiais,aviso);
    }
    private static void validarCliente(String nome,String email,String telefone){
        if(nome==null || nome.isBlank() || ((email==null || email.isBlank()) && br.com.criare3d.util.ContatoCliente.digitos(telefone).isEmpty()))
            throw new IllegalArgumentException("Informe o nome do cliente e pelo menos um telefone ou e-mail.");
    }

    @PostMapping @Transactional public Detalhe iniciar(@RequestParam UUID chave,@Valid @RequestBody SelecaoPedidoRequest selecao){
        if (selecao.getItens()==null || selecao.getItens().isEmpty() || selecao.getItens().stream().anyMatch(i -> i==null || i.getReferencia()==null))
            throw new IllegalArgumentException("Escolha uma peça válida para iniciar.");
        validarCliente(selecao.getCliente(), selecao.getEmail(), selecao.getContato());
        var signature=List.of(String.valueOf(selecao.getCompradorId()),String.valueOf(selecao.getCliente()),String.valueOf(selecao.getContato()),String.valueOf(selecao.getEmail()),selecao.getItens().stream().map(i->List.of(i.getReferencia(),i.getQuantidade(),i.getEstoque(),i.isSelecionado())).toList()).toString();
        var id=chaves.executar(chave,"INICIAR_ORCAMENTO",signature,()->service.adicionarVarios(null,selecao).getId().toString());
        return abrir(UUID.fromString(id));
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="PedidoMobileLinhaEdicao")
    public record LinhaEdicao(UUID id,UUID referencia,@Min(1) int quantidade,@Min(0) int estoque,@NotNull @Positive BigDecimal preco){}
    @io.swagger.v3.oas.annotations.media.Schema(name="PedidoMobileEdicao")
    public record Edicao(long revisao,@NotNull @Valid PedidoDadosRequest dados,@NotEmpty @Size(max=20) List<@NotNull @Valid LinhaEdicao> itens,boolean enviar){}
    @PutMapping("/{id}") @Transactional public Detalhe salvar(@PathVariable UUID id,@Valid @RequestBody Edicao e){
        validarCliente(e.dados().getCliente(),e.dados().getEmail(),e.dados().getContato());
        var p=service.editavel(id);
        if(p.getRevisao()!=e.revisao()) throw new IllegalArgumentException("A proposta mudou. Reabra para ver os valores atuais.");
        var manter=new HashSet<UUID>();
        for(var linha:e.itens()) {
            if(linha.id()==null && linha.referencia()==null) throw new IllegalArgumentException("Escolha uma peça válida.");
            if(linha.id()!=null) {
                OrcamentoPedidoService.item(p,linha.id());
                if(!manter.add(linha.id())) throw new IllegalArgumentException("Item repetido.");
            }
        }
        for(var item:List.copyOf(p.getItens())) if(!manter.contains(item.getId())) service.remover(id,item.getId());
        for(var linha:e.itens()){
            ItemPedido item;
            if(linha.id()==null){
                service.adicionar(id,linha.referencia(),linha.quantidade(),linha.estoque());
                item=p.getItens().getLast();
            } else item=OrcamentoPedidoService.item(p,linha.id());
            if(linha.quantidade()!=item.getQuantidade() || linha.estoque()!=item.getUsarEstoque() || item.getPrecoUnitario().compareTo(linha.preco())!=0)
                service.alterarItem(id,item.getId(),linha.quantidade(),linha.estoque(),linha.preco());
        }
        service.salvarDados(id,e.dados());
        if(e.enviar())service.enviar(id);
        return detalhe(p);
    }
    @PutMapping("/{id}/previa") @Transactional public Detalhe prever(@PathVariable UUID id,@Valid @RequestBody Edicao e){
        var resultado=salvar(id,new Edicao(e.revisao(),e.dados(),e.itens(),false));
        org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return resultado;
    }
    @PostMapping("/{id}/{acao}") @Transactional public Detalhe agir(@PathVariable UUID id,@PathVariable String acao,@RequestParam long revisao,@RequestParam(required=false) UUID item){
        var p=pedidos.bloquear(id).orElseThrow();
        if(p.getRevisao()!=revisao)throw new IllegalArgumentException("A proposta mudou. Reabra o pedido.");
        if (acao.startsWith("voltar-")) {
            String destino=acao.substring(7);
            operacoes.recuar(id,revisao,destino);
            if(destino.equals("rascunho")) service.revisar(id);
            return detalhe(p);
        }
        switch(acao){
            case "arquivar","restaurar" -> service.arquivar(id,acao.equals("arquivar"));
            case "aprovar" -> operacoes.aprovar(id,revisao);
            case "produzir" -> operacoes.produzir(id,item,revisao);
            case "pagar" -> operacoes.pagar(id,revisao);
            case "entregar" -> operacoes.entregar(id,revisao);
            case "revisar" -> service.revisar(id);
            case "cancelar" -> operacoes.cancelar(id,revisao);
            default -> throw new IllegalArgumentException("Ação inválida.");
        }
        return detalhe(p);
    }
}
