package br.com.criare3d.controller;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class CompradorContatoTest {
    @Test void formataTelefoneBrasileiro() {
        assertThat(CompradorController.formatarTelefone("11945414013")).isEqualTo("(11) 94541-4013");
        assertThat(CompradorController.formatarTelefone("+55 (11) 94541-4013")).isEqualTo("(11) 94541-4013");
        assertThat(CompradorController.formatarTelefone("1134567890")).isEqualTo("(11) 3456-7890");
        assertThat(CompradorController.formatarTelefone("ramal 12")).isEqualTo("ramal 12");
    }
    @Test void montaLinkDoWhatsappSoQuandoOTelefoneForValido() {
        assertThat(CompradorController.linkWhatsapp("(11) 94541-4013")).isEqualTo("https://wa.me/5511945414013");
        assertThat(CompradorController.linkWhatsapp("5511945414013")).isEqualTo("https://wa.me/5511945414013");
        assertThat(CompradorController.linkWhatsapp("12345")).isNull();
        assertThat(CompradorController.linkWhatsapp(null)).isNull();
    }
    @Test void geraIniciaisDoNome() {
        assertThat(CompradorController.iniciais("Carolina Nascimento")).isEqualTo("CN");
        assertThat(CompradorController.iniciais("Maria de Souza Lima")).isEqualTo("ML");
        assertThat(CompradorController.iniciais("Bia")).isEqualTo("B");
        assertThat(CompradorController.iniciais("  ")).isEqualTo("?");
    }
}
