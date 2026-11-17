package br.com.criare3d.controller;

import br.com.criare3d.models.Embalagem;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.service.CatalogoModelosImpressora;
import br.com.criare3d.service.interfaces.EmbalagemService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogoCadastroRapidoTest {
    private final FilamentoService filamentos = mock(FilamentoService.class);
    private final ImpressoraService impressoras = mock(ImpressoraService.class);
    private final EmbalagemService embalagens = mock(EmbalagemService.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        var controller = new CatalogoCadastroRapidoController();
        ReflectionTestUtils.setField(controller, "filamentos", filamentos);
        ReflectionTestUtils.setField(controller, "impressoras", impressoras);
        ReflectionTestUtils.setField(controller, "embalagens", embalagens);
        ReflectionTestUtils.setField(controller, "modelosImpressora", new CatalogoModelosImpressora());
        var opcoes = new CatalogoOpcoesController();
        ReflectionTestUtils.setField(opcoes, "modelosImpressora", new CatalogoModelosImpressora());
        mvc = MockMvcBuilders.standaloneSetup(controller, opcoes).build();
    }

    @Test
    void catalogoDeModelosExpoeAReferenciaUsadaNoServidorENoFormulario() throws Exception {
        mvc.perform(get("/catalogo-opcoes/modelos-impressora"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Bambu Lab A1 mini"))
            .andExpect(jsonPath("$[0].potenciaMediaPLA").value(80));
    }

    @Test
    void criaFilamentoPeloServiceSemInventarEstoqueEDevolveDadosParaSelecionar() throws Exception {
        var id = UUID.randomUUID();
        when(filamentos.create(any())).thenAnswer(chamada -> {
            Filamento filamento = chamada.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(filamento.getId()).isNull();
            org.assertj.core.api.Assertions.assertThat(filamento.getEstoqueGramas()).isZero();
            filamento.setId(id);
            return filamento;
        });
        mvc.perform(post("/catalogo-opcoes/cadastro/filamento").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"PLA","cor":"Amarelo","codigoCor":"#ffee00","pesoRoloGramas":"1000","precoRolo":"80.50"}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.nome").value("PLA · Amarelo"))
            .andExpect(jsonPath("$.atributos.codigoCor").value("#ffee00"))
            .andExpect(jsonPath("$.atributos.pesoRolo").value(1000))
            .andExpect(jsonPath("$.atributos.precoRolo").value(80.5))
            .andExpect(jsonPath("$.atributos.estoque").value(0));
    }

    @Test
    void quantidadeInformadaVaiParaOEstoqueDoFilamento() throws Exception {
        when(filamentos.create(any())).thenAnswer(chamada -> {
            Filamento filamento = chamada.getArgument(0);
            filamento.setId(UUID.randomUUID());
            return filamento;
        });
        mvc.perform(post("/catalogo-opcoes/cadastro/filamento").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"PLA","cor":"Amarelo","codigoCor":"#ffee00","pesoRoloGramas":1000,
             "precoRolo":80,"estoqueGramas":450.5}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.atributos.estoque").value(450.5));
    }

    @Test
    void modeloConhecidoPreenchePotenciaNoServidorSemExigirCampoManual() throws Exception {
        when(impressoras.create(any())).thenAnswer(chamada -> {
            Impressora impressora = chamada.getArgument(0);
            impressora.setId(UUID.randomUUID());
            return impressora;
        });
        mvc.perform(post("/catalogo-opcoes/cadastro/impressora").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Principal\",\"modelo\":\"bambu lab a1\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.atributos.potencia").value(95))
            .andExpect(jsonPath("$.atributos.modelo").value("Bambu Lab A1"));
        mvc.perform(post("/catalogo-opcoes/cadastro/impressora").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Principal\",\"modelo\":\"Bambu Lab A1\",\"potenciaWatts\":999}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.atributos.potencia").value(95));
    }

    @Test
    void modeloDesconhecidoSemPotenciaDevolveCampoPendente() throws Exception {
        mvc.perform(post("/catalogo-opcoes/cadastro/impressora").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Principal\",\"modelo\":\"Outro modelo\"}"))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.campos.potenciaWatts").exists());
        verifyNoInteractions(impressoras);
    }

    @Test
    void errosDeFilamentoSaoJsonPorCampoENaoSalvam() throws Exception {
        mvc.perform(post("/catalogo-opcoes/cadastro/filamento").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"","cor":"","codigoCor":"invalida","pesoRoloGramas":0,"precoRolo":-1,"estoqueGramas":-1}
            """))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.campos.nome").exists())
            .andExpect(jsonPath("$.campos.cor").exists()).andExpect(jsonPath("$.campos.codigoCor").exists())
            .andExpect(jsonPath("$.campos.pesoRoloGramas").exists())
            .andExpect(jsonPath("$.campos.precoRolo").exists()).andExpect(jsonPath("$.campos.estoqueGramas").exists());
        verifyNoInteractions(filamentos);
    }

    @Test
    void criaImpressoraComDadosEssenciaisESemExigirDepreciacao() throws Exception {
        when(impressoras.create(any())).thenAnswer(chamada -> {
            Impressora impressora = chamada.getArgument(0);
            impressora.setId(UUID.randomUUID());
            return impressora;
        });
        mvc.perform(post("/catalogo-opcoes/cadastro/impressora").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Impressora principal","modelo":"Bambu A1","potenciaWatts":100}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.atributos.potencia").value(100))
            .andExpect(jsonPath("$.atributos.modelo").value("Bambu A1"))
            .andExpect(jsonPath("$.atributos.tempoAquecimento").value(0));
    }

    @Test
    void validaModeloEPotenciaAntesDeSalvarImpressora() throws Exception {
        mvc.perform(post("/catalogo-opcoes/cadastro/impressora").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Impressora\",\"modelo\":\"\",\"potenciaWatts\":100}"))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.campos.modelo").exists());
        mvc.perform(post("/catalogo-opcoes/cadastro/impressora").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Impressora\",\"modelo\":\"Bambu A1\",\"potenciaWatts\":-1}"))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.campos.potenciaWatts").exists());
        verifyNoInteractions(impressoras);
    }

    @Test
    void embalagemDevolveCustoParaOCalculoERecusaCustoZero() throws Exception {
        when(embalagens.create(any())).thenAnswer(chamada -> {
            Embalagem embalagem = chamada.getArgument(0);
            embalagem.setId(UUID.randomUUID());
            return embalagem;
        });
        mvc.perform(post("/catalogo-opcoes/cadastro/embalagem").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Caixa\",\"custoUnitario\":2.5}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.atributos.custo").value(2.5));
        mvc.perform(post("/catalogo-opcoes/cadastro/embalagem").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Caixa\",\"custoUnitario\":0}"))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.campos.custoUnitario").exists());
        verify(embalagens, times(1)).create(any());
    }

    @Test
    void numeroInvalidoDevolveErroJsonNoPainel() throws Exception {
        mvc.perform(post("/catalogo-opcoes/cadastro/embalagem").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Caixa\",\"custoUnitario\":\"abc\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").exists());
        verifyNoInteractions(embalagens);
    }
}
