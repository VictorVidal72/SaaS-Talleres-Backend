package com.saastalleres.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entidad asociativa que resuelve la relación N:M entre Taller y Vehiculo.
 * Registra la trazabilidad de intervenciones de un coche dentro de cada taller.
 */
@Entity
@Table(
    name = "talleres_vehiculos",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_taller_vehiculo", columnNames = {"taller_id", "vehiculo_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TallerVehiculo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taller_id", nullable = false)
    private Taller taller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "primera_visita", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime primeraVisita = OffsetDateTime.now();

    @Column(name = "ultima_visita", nullable = false)
    @Builder.Default
    private OffsetDateTime ultimaVisita = OffsetDateTime.now();
}