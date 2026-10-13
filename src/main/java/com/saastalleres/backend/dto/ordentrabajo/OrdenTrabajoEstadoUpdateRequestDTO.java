package com.saastalleres.backend.dto.ordentrabajo;

import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contrato inmutable para actualizar el estado operativo de la orden
 * en el tablero Kanban y registrar las anotaciones técnicas del mecánico.
 */
public record OrdenTrabajoEstadoUpdateRequestDTO(

        @NotNull(message = "El nuevo estado de la orden es obligatorio")
        EstadoOrdenTrabajo nuevoEstado,

        @Size(max = 2000, message = "El diagnóstico o notas técnicas no pueden exceder los 2000 caracteres")
        String diagnostico
) {}