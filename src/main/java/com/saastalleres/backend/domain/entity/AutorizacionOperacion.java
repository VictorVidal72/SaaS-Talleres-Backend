package com.saastalleres.backend.domain.entity;

import com.saastalleres.backend.domain.enums.EstadoAutorizacion;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad transaccional que gestiona las micro-autorizaciones de costes
 * imprevistos con respaldo de evidencia digital (RF-12, RN-07).
 */
@Entity
@Table(name = "autorizaciones_operacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizacionOperacion extends BaseEntity {

    @Column(name = "token_uuid", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID tokenUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taller_id", nullable = false)
    private Taller taller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_trabajo_id", nullable = false)
    private OrdenTrabajo ordenTrabajo;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "coste_extra", nullable = false, precision = 10, scale = 2)
    private BigDecimal costeExtra;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoAutorizacion estado = EstadoAutorizacion.PENDIENTE;

    @Column(name = "fecha_resolucion")
    private OffsetDateTime fechaResolucion;

    @Column(name = "ip_consentimiento", length = 45)
    private String ipConsentimiento;

    @Column(name = "user_agent_consentimiento", columnDefinition = "TEXT")
    private String userAgentConsentimiento;
}