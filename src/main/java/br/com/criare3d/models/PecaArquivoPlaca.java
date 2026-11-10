package br.com.criare3d.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "peca_arquivo_placa")
@NoArgsConstructor
public class PecaArquivoPlaca {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private @Getter UUID tenantId;

    @Column(name = "peca_id", nullable = false, updatable = false)
    private @Getter @Setter UUID pecaId;

    @Column(name = "numero", nullable = false)
    private @Getter @Setter int numero;

    @Column(name = "mime_type", nullable = false, length = 30)
    private @Getter @Setter String mimeType;

    @Column(name = "preview", nullable = false)
    private @Getter @Setter byte[] preview;
}
