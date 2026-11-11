package br.com.criare3d.models;
import br.com.criare3d.models.Filamento;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.TenantId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "peca")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Peca {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private @Getter UUID tenantId;

    @NotBlank(message="O nome da peca é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da peca deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_peca", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres.")
    @Column(name = "categoria", length = 60)
    private @Getter @Setter String categoria;

    @NotNull(message = "A quantidade de filamento utilizado na peca é obrigatorio")
    @Column(name="qntFil_peca",nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "O Tempo de impressão da peca é obrigatorio")
    @Column(name= "tmp_peca",nullable = false)
    private @Getter @Setter Long tempoImpressaoMinutos;

    @Column(name = "dados_3mf_json", columnDefinition = "text")
    private @Getter @Setter String dadosArquivo3mf;

    @Column(name = "data_cadastro", updatable = false)
    private @Getter @Setter LocalDateTime dataCadastro;

    @PrePersist
    void registrarDataCadastro() {
        if (dataCadastro == null) dataCadastro = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    // Campo legado preservado para compatibilidade; não é mais usado como padrão do cálculo.
    @Column(name = "tempo_trabalho_minutos", nullable = false)
    @Builder.Default
    private @Getter @Setter Long tempoTrabalhoMinutos = 0L;

    @OneToMany(mappedBy = "peca")
    private Set<CalculoHistorico> historicos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private @Getter @Setter Usuario usuario;
}
