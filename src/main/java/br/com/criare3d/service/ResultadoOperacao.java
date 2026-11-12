package br.com.criare3d.service;
/** Resultados de domínio; códigos textuais ficam restritos às URLs e mensagens existentes. */
public enum ResultadoOperacao {
    NAO_ENCONTRADO("naoEncontrado"),
    OK("ok"),
    VENDA_ATIVA("vendaAtiva"),
    ESTOQUE_INSUFICIENTE("estoqueInsuficiente"),
    VALOR_VENDA_INVALIDO("valorVendaInvalido"),
    QUANTIDADE_VENDA_INVALIDA("quantidadeVendaInvalida"),
    QUANTIDADE_VENDA_ACIMA_DISPONIVEL("quantidadeVendaAcimaDisponivel"),
    PRODUCAO_NECESSARIA("producaoNecessaria"),
    NENHUM_SELECIONADO("nenhumSelecionado"),
    RESERVA_ATIVA("reservaAtiva"),
    PEDIDO_VINCULADO("pedidoVinculado"),
    ACAO_INVALIDA("acaoInvalida");
    private final String codigo;
    ResultadoOperacao(String codigo) { this.codigo = codigo; }
    public String codigo() { return codigo; }
}
