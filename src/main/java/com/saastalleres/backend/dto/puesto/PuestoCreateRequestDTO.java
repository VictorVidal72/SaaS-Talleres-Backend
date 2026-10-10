package com.saastalleres.backend.dto.puesto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PuestoCreateRequestDTO(
        @NotBlank(message = "El nombre o identificador del puesto es obligatorio")
        @Size(max = 50, message = "El nombre del puesto no puede exceder los 50 caracteres")
        String nombre
) {}