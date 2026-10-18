package com.saastalleres.backend.dto.autorizacionoperacion;

import com.saastalleres.backend.domain.entity.AutorizacionOperacion;
import com.saastalleres.backend.domain.entity.OrdenTrabajo;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.enums.EstadoAutorizacion;
import org.springframework.stereotype.Component;

/**
 * Mapper desacoplado para la transformación bidireccional entre
 * contratos DTOs y la entidad de dominio AutorizacionOperacion (ADR-007).
 */
@Component
public class AutorizacionOperacionMapper {

    /**
     * Construye una nueva entidad AutorizacionOperacion a partir del contrato de entrada
     * y las referencias maestras verificadas en el servicio.
     */
    public AutorizacionOperacion toEntity(
            AutorizacionCreateRequestDTO dto,
            Taller taller,
            OrdenTrabajo ordenTrabajo
    ) {
        if (dto == null) {
            return null;
        }

        return AutorizacionOperacion.builder()
                .taller(taller)
                .ordenTrabajo(ordenTrabajo)
                .descripcion(dto.descripcion().trim())
                .costeExtra(dto.costeExtra())
                .estado(EstadoAutorizacion.PENDIENTE)
                .build();
    }

    /**
     * Transforma una entidad JPA a su representación inmutable de salida,
     * aplanando relaciones y enriqueciendo descripciones legibles.
     */
    public AutorizacionResponseDTO toResponseDTO(AutorizacionOperacion entity) {
        if (entity == null) {
            return null;
        }

        String matricula = null;
        if (entity.getOrdenTrabajo() != null && entity.getOrdenTrabajo().getVehiculo() != null) {
            matricula = entity.getOrdenTrabajo().getVehiculo().getMatricula();
        }

        return new AutorizacionResponseDTO(
                entity.getId(),
                entity.getTokenUuid(),
                entity.getTaller() != null ? entity.getTaller().getId() : null,
                entity.getOrdenTrabajo() != null ? entity.getOrdenTrabajo().getId() : null,
                matricula,
                entity.getDescripcion(),
                entity.getCosteExtra(),
                entity.getEstado(),
                entity.getEstado() != null ? entity.getEstado().getDescripcion() : null,
                entity.getFechaResolucion(),
                entity.getCreadoEn()
        );
    }
}