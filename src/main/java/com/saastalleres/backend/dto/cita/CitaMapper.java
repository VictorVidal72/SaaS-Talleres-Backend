package com.saastalleres.backend.dto.cita;

import com.saastalleres.backend.domain.entity.Cita;
import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.domain.enums.OrigenCita;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Mapeador desacoplado entre contratos DTO y la entidad de dominio Cita.
 */
@Component
public class CitaMapper {

    /**
     * Mapea una solicitud del portal web público (Conductor).
     * Siempre nace en estado PENDIENTE_CONFIRMACION, sin puesto asignado y origen WEB.
     */
    public Cita toEntity(CitaPublicaCreateRequestDTO dto, Taller taller, Vehiculo vehiculo) {
        if (dto == null) {
            return null;
        }

        OffsetDateTime fechaFinCalculada = dto.tipoServicio().calcularFin(dto.fechaInicio());

        return Cita.builder()
                .taller(taller)
                .vehiculo(vehiculo)
                .puesto(null) // Las citas web entran sin box asignado
                .fechaInicio(dto.fechaInicio())
                .fechaFin(fechaFinCalculada)
                .tipoServicio(dto.tipoServicio())
                .motivo(dto.motivo())
                .estado(EstadoCita.PENDIENTE_CONFIRMACION)
                .origen(OrigenCita.WEB)
                .build();
    }

    /**
     * Mapea una solicitud creada desde el panel de gestión del taller (Recepción / Teléfono).
     * Permite asociar puesto físico directamente y nace en estado CONFIRMADA.
     */
    public Cita toEntity(CitaInternaCreateRequestDTO dto, Taller taller, Vehiculo vehiculo, Puesto puesto) {
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
                .estado(EstadoCita.CONFIRMADA)
                .origen(OrigenCita.MANUAL) // O MOSTRADOR según el enum definido
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