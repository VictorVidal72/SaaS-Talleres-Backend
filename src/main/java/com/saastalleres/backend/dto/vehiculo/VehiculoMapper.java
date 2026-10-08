package com.saastalleres.backend.dto.vehiculo;

import com.saastalleres.backend.domain.entity.Vehiculo;
import org.springframework.stereotype.Component;

/**
 * Componente traductor entre la entidad Vehiculo y sus contratos DTO.
 * Desacopla la capa de servicios de la construcción y transformación de objetos.
 */
@Component
public class VehiculoMapper {

    /**
     * Transforma el DTO de entrada en la entidad JPA para ser persistida.
     * Utiliza el patrón Builder de Lombok configurado en la entidad.
     */
    public Vehiculo toEntity(VehiculoCreateRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Vehiculo.builder()
                .matricula(dto.matricula())
                .marca(dto.marca())
                .modelo(dto.modelo())
                .color(dto.color())
                .nombreContacto(dto.nombreContacto())
                .telefonoContacto(dto.telefonoContacto())
                .emailContacto(dto.emailContacto())
                .build();
    }

    /**
     * Transforma la entidad JPA en el contrato de salida inmutable para la API.
     * Invoca el constructor canónico del Java Record.
     */
    public VehiculoResponseDTO toResponseDTO(Vehiculo entity) {
        if (entity == null) {
            return null;
        }

        return new VehiculoResponseDTO(
                entity.getId(),
                entity.getMatricula(),
                entity.getMarca(),
                entity.getModelo(),
                entity.getColor(),
                entity.getNombreContacto(),
                entity.getTelefonoContacto(),
                entity.getEmailContacto(),
                entity.getCreadoEn()
        );
    }
}