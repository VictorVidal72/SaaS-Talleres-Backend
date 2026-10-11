package com.saastalleres.backend.dto.cita;

import com.saastalleres.backend.domain.enums.OrigenCita;
import com.saastalleres.backend.domain.enums.TipoServicio;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Contrato inmutable de entrada para la solicitud/creación de una cita (RF-03, RF-08).
 * Valida perimetralmente las restricciones de negocio antes de tocar la base de datos.
 */
public record CitaCreateRequestDTO(

        @NotNull(message = "El identificador del vehículo es obligatorio")
        Long vehiculoId,

        Long puestoId,

        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        @FutureOrPresent(message = "La fecha de inicio no puede ser en el pasado")
        OffsetDateTime fechaInicio,

        @NotNull(message = "El tipo de servicio es obligatorio")
        TipoServicio tipoServicio,

        @NotBlank(message = "El motivo de la cita es obligatorio")
        @Size(max = 1000, message = "El motivo no puede exceder los 1000 caracteres")
        String motivo,

        OrigenCita origen
) {
    /**
     * Constructor compacto para asignar valores predeterminados seguros.
     */
    public CitaCreateRequestDTO {
        if (origen == null) {
            origen = OrigenCita.WEB;
        }
    }
}