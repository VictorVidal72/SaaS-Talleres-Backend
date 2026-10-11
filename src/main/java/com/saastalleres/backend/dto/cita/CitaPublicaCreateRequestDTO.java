package com.saastalleres.backend.dto.cita;

import com.saastalleres.backend.domain.enums.TipoServicio;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Contrato inmutable para la solicitud pública de cita desde el portal web.
 * No expone IDs internos ni recursos físicos de taller.
 */
public record CitaPublicaCreateRequestDTO(

        @NotBlank(message = "La matrícula es obligatoria")
        @Pattern(regexp = "^[0-9]{4}[A-Z]{3}$", message = "Formato de matrícula no válido (ej. 1234ABC)")
        String matricula,

        @NotBlank(message = "El nombre de contacto es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder los 120 caracteres")
        String nombreCliente,

        @NotBlank(message = "El teléfono de contacto es obligatorio")
        @Pattern(regexp = "^(\\+34|0034|34)?[6789]\\d{8}$", message = "Teléfono de contacto inválido")
        String telefonoCliente,

        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        @FutureOrPresent(message = "La fecha de inicio no puede ser en el pasado")
        OffsetDateTime fechaInicio,

        @NotNull(message = "El tipo de servicio es obligatorio")
        TipoServicio tipoServicio,

        @NotBlank(message = "El motivo de la cita es obligatorio")
        @Size(max = 1000, message = "El motivo no puede exceder los 1000 caracteres")
        String motivo
) {}