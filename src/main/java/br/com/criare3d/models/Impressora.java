package br.com.criare3d.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "impressora")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Impressora {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private @Getter UUID tenantId;

    @NotBlank(message="O nome da impressora é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da impressora deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_imp", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotBlank(message="O modelo da impressora é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da impressora deve ter entre 2 à 150 caracteres")
    @Column(name="modelo_imp", nullable = false, length = 150)
    private @Getter @Setter String modelo;

    @NotNull(message = "A potencia da impressora é obrigatória")
    @Column(name="potencia_imp", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal potenciaWatts;

    @Column(name = "valor_compra_imp", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal valorCompra;

    @Column(name = "vida_util_horas_imp", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal vidaUtilHoras;

    @Column(name = "depreciacao_hora_imp", precision = 12, scale = 8)
    private @Getter @Setter BigDecimal depreciacaoPorHora;

    @Column(name = "depreciacao_origem_imp", length = 30)
    private @Getter @Setter String depreciacaoOrigem;

    @Column(name = "anuncios_novos_imp")
    private @Getter @Setter Integer anunciosNovos;

    @Column(name = "anuncios_usados_imp")
    private @Getter @Setter Integer anunciosUsados;

    @Column(name = "depreciacao_manual_imp", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean depreciacaoManual = false;

    @Column(name = "horas_mensais_uso_imp", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal horasMensaisUso;

    @Column(name = "horas_mensais_manual_imp", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean horasMensaisManual = false;

    @Column(name = "manutencao_hora_imp", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal manutencaoPorHora;

    @Column(name = "tempo_aquecimento_min_imp", nullable = false)
    @Builder.Default
    private @Getter @Setter Integer tempoAquecimentoMinutos = 0;


    @OneToMany(mappedBy = "impressora")
    private Set<CalculoHistorico> historicos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private @Getter @Setter Usuario usuario;
}
