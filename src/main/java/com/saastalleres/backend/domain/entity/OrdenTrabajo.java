package com.saastalleres.backend.domain.entity;

import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad transaccional que gestiona la estancia física del vehículo
 * y el ciclo técnico de reparación tras el check-in (RF-11, RN-08).
 */
@Entity
@Table(name = "ordenes_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenTrabajo extends BaseEntity {

    @Column(name = "token_uuid", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID tokenUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taller_id", nullable = false)
    private Taller taller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", unique = true)
    private Cita cita;

    @Column(name = "fecha_llegada", nullable = false)
    @Builder.Default
    private OffsetDateTime fechaLlegada = OffsetDateTime.now();

    @Column(name = "fecha_entrega")
    private OffsetDateTime fechaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoOrdenTrabajo estado = EstadoOrdenTrabajo.RECEPCIONADO;

    @Column(name = "diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "kilometraje_entrada", nullable = false)
    private Integer kilometrajeEntrada;

    @Column(name = "coste_base", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costeBase = BigDecimal.ZERO;
}