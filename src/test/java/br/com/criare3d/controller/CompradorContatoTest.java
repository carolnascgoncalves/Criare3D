package br.com.criare3d.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompradorContatoTest {
    @Test
    void formataTelefoneBrasileiro() {
        assertThat(br.com.criare3d.util.ContatoCliente.formatarTelefone("11945414013")).isEqualTo("(11) 94541-4013");
        assertThat(br.com.criare3d.util.ContatoCliente.formatarTelefone("+55 (11) 94541-4013")).isEqualTo("(11) 94541-4013");
        assertThat(br.com.criare3d.util.ContatoCliente.formatarTelefone("1134567890")).isEqualTo("(11) 3456-7890");
        assertThat(br.com.criare3d.util.ContatoCliente.formatarTelefone("ramal 12")).isEqualTo("ramal 12");
    }

    @Test
    void montaLinkDoWhatsappSoQuandoOTelefoneForValido() {
        assertThat(br.com.criare3d.util.ContatoCliente.linkWhatsapp("(11) 94541-4013")).isEqualTo("https://wa.me/5511945414013");
        assertThat(br.com.criare3d.util.ContatoCliente.linkWhatsapp("5511945414013")).isEqualTo("https://wa.me/5511945414013");
        assertThat(br.com.criare3d.util.ContatoCliente.linkWhatsapp("12345")).isNull();
        assertThat(br.com.criare3d.util.ContatoCliente.linkWhatsapp(null)).isNull();
    }

    @Test
    void geraIniciaisDoNome() {
        assertThat(br.com.criare3d.util.ContatoCliente.iniciais("Carolina Nascimento")).isEqualTo("CN");
        assertThat(br.com.criare3d.util.ContatoCliente.iniciais("Maria de Souza Lima")).isEqualTo("ML");
        assertThat(br.com.criare3d.util.ContatoCliente.iniciais("Bia")).isEqualTo("B");
        assertThat(br.com.criare3d.util.ContatoCliente.iniciais("  ")).isEqualTo("?");
    }
}
