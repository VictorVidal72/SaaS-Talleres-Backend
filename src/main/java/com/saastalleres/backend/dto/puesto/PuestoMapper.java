package com.saastalleres.backend.dto.puesto;

import com.saastalleres.backend.domain.entity.Puesto;
import org.springframework.stereotype.Component;

/**
 * Componente traductor entre la entidad Puesto y sus contratos DTO.
 * Desacopla la capa de servicios de la construcción y transformación de objetos.
 */
@Component
public class PuestoMapper {

    /**
     * Transforma el DTO de creación en la entidad JPA.
     * El puesto nace por defecto con estado activo = true.
     */
    public Puesto toEntity(PuestoCreateRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Puesto.builder()
                .nombre(dto.nombre())
                .activo(true)
                .build();
    }

    /**
     * Transforma la entidad JPA en el contrato de salida inmutable para la API.
     * Extrae de forma segura el tallerId de la relación ManyToOne.
     */
    public PuestoResponseDTO toResponseDTO(Puesto entity) {
        if (entity == null) {
            return null;
        }

        return new PuestoResponseDTO(
                entity.getId(),
                entity.getTaller() != null ? entity.getTaller().getId() : null,
                entity.getNombre(),
                entity.getActivo(),
                entity.getCreadoEn()
        );
    }
}