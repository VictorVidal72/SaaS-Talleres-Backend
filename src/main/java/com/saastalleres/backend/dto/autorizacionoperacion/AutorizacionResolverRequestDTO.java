package com.saastalleres.backend.dto.autorizacionoperacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contrato inmutable para la resolución remota (aprobación o rechazo)
 * de una micro-autorización por parte del cliente final (RF-12, RN-07).
 */
public record AutorizacionResolverRequestDTO(

        @NotNull(message = "La decisión de aprobación o rechazo es obligatoria")
        Boolean aprobado,

        @Size(max = 500, message = "El motivo de rechazo no puede exceder los 500 caracteres")
        String motivoRechazo
) {
}