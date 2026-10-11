package com.saastalleres.backend.dto.cita;

import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.domain.enums.OrigenCita;
import com.saastalleres.backend.domain.enums.TipoServicio;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrato inmutable de salida con la información pública y operativa de una cita.
 * Oculta la complejidad relacional de la entidad Cita y expone el token de acceso seguro.
 */
public record CitaResponseDTO(
        Long id,
        UUID tokenUuid,
        Long tallerId,
        Long vehiculoId,
        String vehiculoMatricula,
        Long puestoId,
        String puestoNombre,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        TipoServicio tipoServicio,
        String motivo,
        EstadoCita estado,
        String motivoRechazoOPropuesta,
        OrigenCita origen,
        OffsetDateTime creadoEn
) {
}