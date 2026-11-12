package br.com.criare3d.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "venda_calculo")
@NoArgsConstructor
public class VendaCalculo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter @Setter
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    @Getter
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculo_id", nullable = false)
    @Getter @Setter
    private CalculoHistorico calculo;

    @Getter @Setter private UUID pedidoId;

    @Column(name = "unidades", nullable = false)
    @Getter @Setter
    private Integer unidades;

    @Column(name = "data_venda", nullable = false)
    @Getter @Setter
    private LocalDateTime dataVenda;
    @Column(nullable = false)
    @Getter @Setter private int unidadesCanceladas;
    @Getter @Setter private LocalDateTime dataCancelamento;
    @Column(precision = 16, scale = 8) @Getter @Setter private java.math.BigDecimal precoUnitario;
    @Column(precision = 16, scale = 8) @Getter @Setter private java.math.BigDecimal descontoUnitario;
    @Column(precision = 16, scale = 8) @Getter @Setter private java.math.BigDecimal taxasUnitarias;
    @Column(precision = 16, scale = 8) @Getter @Setter private java.math.BigDecimal lucroUnitario;
    @Column(nullable = false) @Getter @Setter private boolean valoresReais;
    public int getUnidadesAtivas() { return unidades - unidadesCanceladas; }
}
