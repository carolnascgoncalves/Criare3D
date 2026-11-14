package br.com.criare3d.models;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor
public class Comprador {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @TenantId @Column(nullable=false,updatable=false) private UUID tenantId;
 @Version private Long versao;
 @NotBlank @Size(min=2,max=150) @Column(nullable=false,length=150) private String nome;
 @Size(max=150) @Column(length=150) private String telefone;
 @Email @Size(max=150) @Column(length=150) private String email;
 @Size(max=500) @Column(length=500) private String observacoes;
 private boolean arquivado;
}
