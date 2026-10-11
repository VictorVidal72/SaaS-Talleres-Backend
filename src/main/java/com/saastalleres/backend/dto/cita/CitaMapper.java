package com.saastalleres.backend.dto.cita;

import com.saastalleres.backend.domain.entity.Cita;
import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.domain.enums.EstadoCita;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Mapeador desacoplado entre contratos DTO y la entidad de dominio Cita.
 */
@Component
public class CitaMapper {

    /**
     * Transforma una solicitud de entrada en una entidad Cita lista para persistir.
     * La fecha de fin se calcula automáticamente según la duración intrínseca del servicio (RF-09).
     */
    public Cita toEntity(CitaCreateRequestDTO dto, Taller taller, Vehiculo vehiculo, Puesto puesto) {
        if (dto == null) {
            return null;
        }

        OffsetDateTime fechaFinCalculada = dto.tipoServicio().calcularFin(dto.fechaInicio());

        return Cita.builder()
                .taller(taller)
                .vehiculo(vehiculo)
                .puesto(puesto)
                .fechaInicio(dto.fechaInicio())
                .fechaFin(fechaFinCalculada)
                .tipoServicio(dto.tipoServicio())
                .motivo(dto.motivo())
                .estado(EstadoCita.PENDIENTE_CONFIRMACION)
                .origen(dto.origen())
                .build();
    }

    /**
     * Convierte una entidad Cita en el contrato inmutable de respuesta.
     * Maneja de forma segura las referencias opcionales a Puesto para evitar NullPointerException.
     */
    public CitaResponseDTO toResponseDTO(Cita entity) {
        if (entity == null) {
            return null;
        }

        Long puestoId = null;
        String puestoNombre = null;
        if (entity.getPuesto() != null) {
            puestoId = entity.getPuesto().getId();
            puestoNombre = entity.getPuesto().getNombre();
        }

        return new CitaResponseDTO(
                entity.getId(),
                entity.getTokenUuid(),
                entity.getTaller() != null ? entity.getTaller().getId() : null,
                entity.getVehiculo() != null ? entity.getVehiculo().getId() : null,
                entity.getVehiculo() != null ? entity.getVehiculo().getMatricula() : null,
                puestoId,
                puestoNombre,
                entity.getFechaInicio(),
                entity.getFechaFin(),
                entity.getTipoServicio(),
                entity.getMotivo(),
                entity.getEstado(),
                entity.getMotivoRechazoOPropuesta(),
                entity.getOrigen(),
                entity.getCreadoEn()
        );
    }
}