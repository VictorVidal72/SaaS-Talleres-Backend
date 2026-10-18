package com.saastalleres.backend.dto.autorizacionoperacion;

import com.saastalleres.backend.domain.enums.EstadoAutorizacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrato inmutable de salida para la visualización y seguimiento
 * de micro-autorizaciones tanto en canal interno como público (RF-12, RN-07).
 */
public record AutorizacionResponseDTO(
        Long id,
        UUID tokenUuid,
        Long tallerId,
        Long ordenTrabajoId,
        String matriculaVehiculo,
        String descripcion,
        BigDecimal costeExtra,
        EstadoAutorizacion estado,
        String estadoDescripcion,
        OffsetDateTime fechaResolucion,
        OffsetDateTime creadoEn
) {
}