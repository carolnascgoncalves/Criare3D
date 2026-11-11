package br.com.criare3d.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ValidacaoCadastroTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void fechar() { FACTORY.close(); }

    @Test
    void impressoraVaziaPedeApenasModeloAntesDeValidarPotencia() {
        assertThat(VALIDATOR.validate(new ImpressoraRequest()))
                .extracting(erro -> erro.getPropertyPath().toString()).containsOnly("modelo");
    }

    @Test
    void modeloEscolhidoSemPotenciaPedePotencia() {
        var pedido = ImpressoraRequest.builder().nome("Minha impressora").modelo("Outro modelo").build();
        assertThat(VALIDATOR.validate(pedido))
                .extracting(erro -> erro.getPropertyPath().toString()).containsOnly("potenciaWatts");
    }

    @Test
    void modeloEpotenciaInformadosPermitemCadastro() {
        var pedido = ImpressoraRequest.builder().nome("Minha impressora").modelo("Outro modelo")
                .potenciaWatts(new BigDecimal("100")).build();
        assertThat(VALIDATOR.validate(pedido)).isEmpty();
    }

    @Test
    void depreciacaoPermiteDadosParciaisEHorasComDuasCasas() {
        var pedido = ImpressoraRequest.builder().nome("Minha impressora").modelo("Outro modelo")
                .potenciaWatts(new BigDecimal("100")).valorCompra(new BigDecimal("2000"))
                .horasMensaisUso(new BigDecimal("398.47")).build();
        assertThat(VALIDATOR.validate(pedido)).isEmpty();
    }

    @Test
    void depreciacaoOpcionalAindaRejeitaValoresNegativos() {
        var pedido = ImpressoraRequest.builder().nome("Minha impressora").modelo("Outro modelo")
                .potenciaWatts(new BigDecimal("100")).horasMensaisUso(new BigDecimal("-1")).build();
        assertThat(VALIDATOR.validate(pedido))
                .extracting(erro -> erro.getPropertyPath().toString()).containsOnly("horasMensaisUso");
    }

    @Test
    void categoriaEmBrancoNaoPermiteCadastrarPeca() {
        var pedido = PecaRequest.builder().nome("Peça").categoria("   ")
                .quantidadeFilamentoGramas(BigDecimal.TEN).tempoImpressaoMinutos(20L).build();
        assertThat(VALIDATOR.validate(pedido))
                .extracting(erro -> erro.getPropertyPath().toString()).containsOnly("categoria");
    }
}
