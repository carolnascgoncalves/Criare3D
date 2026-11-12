package br.com.criare3d.service;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationHistoricoTest {
    @Test
    void v32PreservaChecksumDaVersaoJaAplicada() throws Exception {
        // O Flyway calcula CRC32 das linhas UTF-8, sem os delimitadores de linha.
        // Alterações futuras devem usar uma migration nova, não editar a V32.
        var recurso = getClass().getResourceAsStream("/db/migration/V32__custom_costs_and_completed_planning.sql");
        assertThat(recurso).isNotNull();
        var crc = new CRC32();
        try (var leitor = new BufferedReader(new InputStreamReader(recurso, StandardCharsets.UTF_8))) {
            for (String linha; (linha = leitor.readLine()) != null;) {
                crc.update(linha.getBytes(StandardCharsets.UTF_8));
            }
        }
        assertThat((int) crc.getValue()).isEqualTo(50938521);
    }
}
