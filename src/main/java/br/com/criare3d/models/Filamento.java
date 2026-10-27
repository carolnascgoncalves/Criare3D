package br.com.criare3d.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "filamento")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Filamento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @NotBlank(message="O nome do filamento é obrigatório")
    @Size(min = 2, max = 150, message = "O nome do filamento deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_fil", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotBlank(message="A cor do filamento é obrigatória")
    @Size(min = 2, max = 80, message = "A cor do filamento deve ter entre 2 à 80 caracteres")
    @Column(name= "cor_fil", nullable = false, length = 80)
    private @Getter @Setter String cor;

    @Size(min = 0, max = 150, message = "A marca do filamento deve ter entre 2 à 150 caracteres")
    @Column(name= "marca_fil", length = 150)
    private @Getter @Setter String marca;

    @NotNull(message = "O peso do rolo comprado do filamento é obrigatorio")
    @Column(name="pesoRolo_fil", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal pesoRoloGramas;

    @NotNull(message = "O preco do filamento do filamento é obrigatorio")
    @Column(name="precoRolo_fil", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoRolo;


    @OneToMany(mappedBy = "filamento")
    private Set<CalculoHistorico> historicos;
}
