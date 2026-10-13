package com.saastalleres.backend.dto.ordentrabajo;

import com.saastalleres.backend.domain.entity.OrdenTrabajo;
import org.springframework.stereotype.Component;

/**
 * Componente traductor desacoplado para transformar entre la entidad JPA OrdenTrabajo
 * y su contrato de salida inmutable OrdenTrabajoResponseDTO.
 */
@Component
public class OrdenTrabajoMapper {

    /**
     * Transforma una entidad OrdenTrabajo en su correspondiente DTO de respuesta.
     * Aplica aplanado de datos para evitar llamadas HTTP adicionales desde el frontend.
     */
    public OrdenTrabajoResponseDTO toResponseDTO(OrdenTrabajo orden) {
        if (orden == null) {
            return null;
        }

        Long citaId = (orden.getCita() != null) ? orden.getCita().getId() : null;
        String matricula = (orden.getVehiculo() != null) ? orden.getVehiculo().getMatricula() : null;
        String marca = (orden.getVehiculo() != null) ? orden.getVehiculo().getMarca() : null;
        String modelo = (orden.getVehiculo() != null) ? orden.getVehiculo().getModelo() : null;

        return new OrdenTrabajoResponseDTO(
                orden.getId(),
                orden.getTokenUuid(),
                orden.getTaller().getId(),
                orden.getVehiculo().getId(),
                matricula,
                marca,
                modelo,
                citaId,
                orden.getFechaLlegada(),
                orden.getFechaEntrega(),
                orden.getEstado(),
                orden.getEstado().getDescripcion(),
                orden.getDiagnostico(),
                orden.getKilometrajeEntrada(),
                orden.getCosteBase(),
                orden.getCreadoEn()
        );
    }
}