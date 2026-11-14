package br.com.criare3d;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationIntegrityTest {
    @Test void migracao47MantemOChecksumJaAplicado() throws Exception {
        var recurso=getClass().getResourceAsStream("/db/migration/V47__compradores_e_extrato.sql");
        assertThat(recurso).isNotNull();
        var checksum=new CRC32();
        try(var reader=new BufferedReader(new InputStreamReader(recurso,StandardCharsets.UTF_8))) {
            String linha;
            while((linha=reader.readLine())!=null)checksum.update(linha.getBytes(StandardCharsets.UTF_8));
        }
        assertThat((int)checksum.getValue()).as("Migração aplicada não deve ser reescrita; ajustes pertencem a novas versões").isEqualTo(1696685285);
    }
}
