package com.saastalleres.backend.dto.puesto;

import java.time.OffsetDateTime;

public record PuestoResponseDTO(
        Long id,
        Long tallerId,
        String nombre,
        Boolean activo,
        OffsetDateTime createdAt
) {}