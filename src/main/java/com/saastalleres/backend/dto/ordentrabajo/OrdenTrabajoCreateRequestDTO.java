package com.saastalleres.backend.dto.ordentrabajo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Contrato inmutable para la recepción física del vehículo y apertura de la orden de trabajo.
 */
public record OrdenTrabajoCreateRequestDTO(

        @NotNull(message = "El identificador del vehículo es obligatorio")
        Long vehiculoId,

        Long citaId,

        @NotNull(message = "El kilometraje de entrada es obligatorio")
        @Min(value = 0, message = "El kilometraje de entrada no puede ser negativo")
        Integer kilometrajeEntrada,

        @Size(max = 2000, message = "El diagnóstico o notas iniciales no pueden exceder los 2000 caracteres")
        String diagnostico,

        @DecimalMin(value = "0.00", message = "El coste base no puede ser negativo")
        BigDecimal costeBase
) {
    /**
     * Constructor compacto para asignar valores predeterminados seguros.
     */
    public OrdenTrabajoCreateRequestDTO {
        if (costeBase == null) {
            costeBase = BigDecimal.ZERO;
        }
    }
}