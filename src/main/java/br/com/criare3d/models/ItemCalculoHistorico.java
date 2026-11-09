
package br.com.criare3d.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itemCalculoHistorico")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCalculoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private @Getter UUID tenantId;

    @Column(name = "nomeFilamento_item", nullable = false, length = 150)
    private @Getter @Setter String nomeFilamento;

    @Column(name = "corFilamento_item", length = 80)
    private @Getter @Setter String corFilamento;

    @Column(name = "codigoCor_item", length = 7)
    private @Getter @Setter String codigoCor;

    @Column(name = "precoRolo_item", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoRoloFilamento;

    @Column(name = "pesoRolo_item", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal pesoRoloGramas;

    @Column(name = "quantidade_item", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    @Column(name = "purga_gramas_item", nullable = false, precision = 12, scale = 6)
    @Builder.Default
    private @Getter @Setter BigDecimal purgaGramas = BigDecimal.ZERO;

    @Column(name = "custoFilamento_item", nullable = false, precision = 16, scale = 8)
    private @Getter @Setter BigDecimal custoFilamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_calculo_fk", nullable = false)
    private @Getter @Setter CalculoHistorico calculoHistorico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_filamento_fk")
    private @Getter @Setter Filamento filamento;
}
