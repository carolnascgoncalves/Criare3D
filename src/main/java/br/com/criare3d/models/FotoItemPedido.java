package br.com.criare3d.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FotoItemPedido {
    @Id
    private UUID itemId;
    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID tenantId;
    @Column(nullable = false, length = 30)
    private String tipo;
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] bytes;
}
