package com.saastalleres.backend.domain.entity;

import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.domain.enums.OrigenCita;
import com.saastalleres.backend.domain.enums.TipoServicio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad transaccional que gestiona la reserva en agenda y slots temporales.
 */
@Entity
@Table(name = "citas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita extends BaseEntity {

    @Column(name = "token_uuid", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID tokenUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taller_id", nullable = false)
    private Taller taller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id")
    private Puesto puesto;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private OffsetDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_servicio", nullable = false, length = 30)
    private TipoServicio tipoServicio;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoCita estado = EstadoCita.PENDIENTE_CONFIRMACION;

    @Column(name = "motivo_rechazo_o_propuesta", columnDefinition = "TEXT")
    private String motivoRechazoOPropuesta;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    @Builder.Default
    private OrigenCita origen = OrigenCita.WEB;
}