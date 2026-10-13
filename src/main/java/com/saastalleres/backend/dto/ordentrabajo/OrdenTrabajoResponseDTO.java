package com.saastalleres.backend.dto.ordentrabajo;

import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrato inmutable de salida para la visualización de órdenes de trabajo.
 * Empleado tanto por el panel de gestión del taller (Kanban) como por el visor
 * de seguimiento del cliente sin credenciales mediante token UUID.
 */
public record OrdenTrabajoResponseDTO(
        Long id,
        UUID tokenUuid,
        Long tallerId,
        Long vehiculoId,
        String matricula,
        String marca,
        String modelo,
        Long citaId,
        OffsetDateTime fechaLlegada,
        OffsetDateTime fechaEntrega,
        EstadoOrdenTrabajo estado,
        String estadoDescripcion,
        String diagnostico,
        Integer kilometrajeEntrada,
        BigDecimal costeBase,
        OffsetDateTime creadoEn
) {}