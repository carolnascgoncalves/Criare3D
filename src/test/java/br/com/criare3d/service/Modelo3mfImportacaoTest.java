package br.com.criare3d.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class Modelo3mfImportacaoTest {
    private MockMultipartFile arquivo(Map<String,String> entradas) throws Exception {
        var bytes=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(bytes)) {
            for(var entrada:entradas.entrySet()) {
                zip.putNextEntry(new ZipEntry(entrada.getKey()));zip.write(entrada.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("Metadata/plate_1.png"));zip.write(new byte[]{(byte)137,80,78,71,13,10,26,10});zip.closeEntry();
        }
        return new MockMultipartFile("arquivo3mf","Peca de consulta.3mf","application/octet-stream",bytes.toByteArray());
    }
    @Test void extraiTresFilamentosPreviewNomeEQuantidadePorPlaca() throws Exception {
        var resposta=new Modelo3mfParser().analisar(arquivo(Map.of(
            "3D/3dmodel.model","<model><metadata name='Title'>Dragão</metadata><build><item objectid='1'/></build></model>",
            "Metadata/model_settings.config","<config><plate><metadata key='plater_id' value='1'/><model_instance/><model_instance/></plate><plate><metadata key='plater_id' value='2'/><model_instance/><model_instance/><model_instance/></plate></config>",
            "Metadata/slice_info.config","<config><plate><metadata key='index' value='1'/><metadata key='prediction' value='3600'/><filament id='1' type='PLA' color='#ff0000' used_g='12'/><filament id='2' type='PLA' color='#0000ff' used_g='24'/><filament id='3' type='PLA' color='#ffffff' used_g='36'/></plate></config>"
        )));
        assertThat(resposta.nome()).isEqualTo("Dragão");assertThat(resposta.placas()).hasSize(2);
        var placa=resposta.placas().getFirst();assertThat(placa.quantidadePecas()).isEqualTo(2);
        assertThat(resposta.placas().get(1).quantidadePecas()).isEqualTo(3);
        assertThat(placa.imagemPreviewDataUri()).startsWith("data:image/png;base64,");
        assertThat(placa.tempoSegundos()).isEqualTo(3600);assertThat(placa.filamentos()).hasSize(3);
        assertThat(placa.filamentos()).extracting(f->f.pesoGramas()).containsExactly(12d,24d,36d);
    }
    @Test void contaInstanciasImprimiveisSemContarPartesDoObjeto() throws Exception {
        var resposta=new Modelo3mfParser().analisar(arquivo(Map.of("3D/3dmodel.model",
            "<model><resources><object id='1'><components><component objectid='2'/><component objectid='3'/></components></object></resources><build><item objectid='1'/><item objectid='1'/><item objectid='1' printable='0'/></build></model>")));
        assertThat(resposta.placas().getFirst().quantidadePecas()).isEqualTo(2);
        assertThat(resposta.nome()).isEqualTo("Peca de consulta");
    }

    @Test void consultaLeModeloMaiorQueQuatroMbSemCarregarGeometriaEmDom() throws Exception {
        var grande="<model><metadata name='Title'>Modelo grande</metadata><resources><!--"+"x".repeat(5*1024*1024)+"--></resources><build><item objectid='1'/></build></model>";
        var entradas=Map.of("3D/3dmodel.model",grande);
        assertThatIllegalArgumentException().isThrownBy(()->new Modelo3mfParser().analisar(arquivo(entradas))).withMessageContaining("metadados");
        var resposta=new Modelo3mfParser().analisarConsulta(arquivo(entradas));
        assertThat(resposta.nome()).isEqualTo("Modelo grande");
        assertThat(resposta.placas().getFirst().quantidadePecas()).isEqualTo(1);
    }
    @Test void devolveEstatisticasDeTodasAsPlacasMesmoAlemDaOitava() throws Exception {
        var entradas=new LinkedHashMap<String,String>();entradas.put("3D/3dmodel.model","<model/>");
        for(int i=1;i<=10;i++) entradas.put("Metadata/plate_"+i+".statistics.json","{\"summary\":{\"time\":60,\"weight\":1,\"filament\":[{\"id\":1,\"weight\":1}]}}");
        var resposta=new Modelo3mfParser().analisarConsulta(arquivo(entradas));
        assertThat(resposta.placas()).hasSize(10);assertThat(resposta.tempoTotalSegundos()).isEqualTo(600d);
    }
}
