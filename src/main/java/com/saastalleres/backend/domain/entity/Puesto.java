package com.saastalleres.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de infraestructura que representa un recurso físico (box o elevador)
 * para el control de carga y prevención de colisiones en agenda (RF-05, RN-02).
 */
@Entity
@Table(name = "puestos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Puesto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taller_id", nullable = false)
    private Taller taller;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}