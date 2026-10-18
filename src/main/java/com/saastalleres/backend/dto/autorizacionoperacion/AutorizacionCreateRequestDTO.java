package com.saastalleres.backend.dto.autorizacionoperacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Contrato inmutable para la solicitud interna de autorización de costes imprevistos (RF-12).
 */
public record AutorizacionCreateRequestDTO(

        @NotNull(message = "El identificador de la orden de trabajo es obligatorio")
        Long ordenTrabajoId,

        @NotBlank(message = "La descripción de la operación o recambio imprevisto es obligatoria")
        @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
        String descripcion,

        @NotNull(message = "El coste extra es obligatorio")
        @DecimalMin(value = "0.00", message = "El coste extra no puede ser negativo")
        BigDecimal costeExtra
) {
}