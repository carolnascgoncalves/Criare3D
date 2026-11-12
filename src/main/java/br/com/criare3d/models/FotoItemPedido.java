package br.com.criare3d.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor
public class FotoItemPedido {
 @Id private UUID itemId;
 @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
 @Column(nullable=false,length=30) private String tipo;
 @Column(nullable=false,columnDefinition="bytea") private byte[] bytes;
}
