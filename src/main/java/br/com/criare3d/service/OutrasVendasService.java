package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
@Service @Transactional
public class OutrasVendasService {
    @Autowired private ProdutoLojaRepository produtos;
    @Autowired private VendaProdutoLojaRepository vendas;
    @Autowired private EmbalagemRepository embalagens;
    @Autowired private Clock clock;
    @Autowired private DashboardDadosService dashboard;
    @Autowired private UsuarioLogadoService usuario;
    private void valor(BigDecimal valor, boolean obrigatorio) {
        if(valor==null) {if(obrigatorio) throw new IllegalArgumentException("Informe o preço de venda.");return;}
        if(valor.signum()<0 || valor.compareTo(new BigDecimal("99999999.99"))>0 || valor.scale()>2)
            throw new IllegalArgumentException("Informe valores de até R$ 99.999.999,99, com no máximo duas casas decimais.");
    }
    public ProdutoLoja salvar(UUID id,String nome,BigDecimal preco,BigDecimal custo) {
        if(nome==null || nome.trim().length()<2 || nome.trim().length()>150) throw new IllegalArgumentException("Informe um nome entre 2 e 150 caracteres.");
        valor(preco,true);valor(custo,false);
        var p=id==null ? new ProdutoLoja() : produtos.findById(id).orElseThrow(()->new IllegalArgumentException("Produto não encontrado."));
        p.setNome(nome.trim());p.setPreco(preco);p.setCusto(custo);return produtos.save(p);
    }
    public void arquivar(UUID id,boolean arquivado) {
        var p=produtos.findById(id).orElseThrow(()->new IllegalArgumentException("Produto não encontrado."));p.setArquivado(arquivado);
    }
    public VendaProdutoLoja vender(UUID produtoId,int quantidade,BigDecimal preco,BigDecimal custo,LocalDate data) {
        return vender(produtoId,quantidade,preco,custo,data,VendaProdutoLoja.TipoEmbalagem.SEM,null,null);
    }
    public VendaProdutoLoja vender(UUID produtoId,int quantidade,BigDecimal preco,BigDecimal custo,LocalDate data,
            VendaProdutoLoja.TipoEmbalagem tipoEmbalagem,UUID embalagemId,BigDecimal valorEmbalagem) {
        return vender(produtoId,quantidade,preco,custo,data,tipoEmbalagem,embalagemId,valorEmbalagem,quantidade);
    }
    public VendaProdutoLoja vender(UUID produtoId,int quantidade,BigDecimal preco,BigDecimal custo,LocalDate data,
            VendaProdutoLoja.TipoEmbalagem tipoEmbalagem,UUID embalagemId,BigDecimal valorEmbalagem,int quantidadeEmbalagens) {
        if(tipoEmbalagem!=VendaProdutoLoja.TipoEmbalagem.SEM && (quantidadeEmbalagens<1 || quantidadeEmbalagens>10000))
            throw new IllegalArgumentException("Informe entre 1 e 10.000 embalagens.");
        if(quantidade<1 || quantidade>10000) throw new IllegalArgumentException("Informe entre 1 e 10.000 unidades.");
        valor(preco,false);valor(custo,false);
        var p=produtos.findById(produtoId).orElseThrow(()->new IllegalArgumentException("Produto não encontrado."));
        if(p.isArquivado()) throw new IllegalArgumentException("Reative o produto antes de registrar a venda.");
        LocalDate hoje=LocalDate.now(clock);if(data==null) data=hoje;
        if(data.isAfter(hoje) || data.isBefore(LocalDate.of(1900,1,1))) throw new IllegalArgumentException("Informe uma data de venda entre 1900 e hoje.");
        var v=new VendaProdutoLoja();v.setProduto(p);v.setNomeProduto(p.getNome());v.setQuantidade(quantidade);
        v.setPrecoUnitario(preco==null ? p.getPreco() : preco);v.setCustoUnitario(custo==null ? p.getCusto() : custo);
        if(tipoEmbalagem==null) throw new IllegalArgumentException("Escolha a opção de embalagem.");
        v.setTipoEmbalagem(tipoEmbalagem);v.setQuantidadeEmbalagens(tipoEmbalagem==VendaProdutoLoja.TipoEmbalagem.SEM?0:quantidadeEmbalagens);
        switch(tipoEmbalagem) {
            case SEM -> v.setCustoEmbalagem(BigDecimal.ZERO);
            case CADASTRADA -> {
                if(embalagemId==null) throw new IllegalArgumentException("Selecione a embalagem cadastrada.");
                var embalagem=embalagens.findById(embalagemId).filter(e->!e.isArquivado())
                    .orElseThrow(()->new IllegalArgumentException("Embalagem não encontrada ou arquivada."));
                valor(embalagem.getCustoUnitario(),true);
                v.setEmbalagemId(embalagem.getId());v.setNomeEmbalagem(embalagem.getNome());v.setCustoEmbalagem(embalagem.getCustoUnitario());
            }
            case PERSONALIZADA -> {
                if(valorEmbalagem==null) throw new IllegalArgumentException("Informe o custo de uma embalagem.");
                valor(valorEmbalagem,true);v.setNomeEmbalagem("Embalagem personalizada");v.setCustoEmbalagem(valorEmbalagem);
            }
        }
        v.setDataVenda(data.atTime(LocalTime.now(clock)));vendas.save(v);atualizarDashboard();return v;
    }
    public void cancelar(UUID id,int quantidade) {
        var v=vendas.bloquear(id).orElseThrow(()->new IllegalArgumentException("Venda não encontrada."));
        if(quantidade<1 || quantidade>v.getAtivas()) throw new IllegalArgumentException("Informe uma quantidade entre 1 e as unidades ativas da venda.");
        v.setCanceladas(v.getCanceladas()+quantidade);v.setDataCancelamento(LocalDateTime.now(clock));atualizarDashboard();
    }
    private void atualizarDashboard() {
        UUID tenant=usuario.buscarTenantIdAtual();
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() {dashboard.invalidar(tenant);}
            });
        } else dashboard.invalidar(tenant);
    }
}
