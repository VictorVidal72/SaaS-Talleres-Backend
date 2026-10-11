package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.Cita;
import com.saastalleres.backend.domain.enums.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la gestión de citas de agenda,
 * control de disponibilidad de puestos y acceso público por token efímero.
 */
@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // 1. Localizar la cita mediante el token UUID público del conductor
    Optional<Cita> findByTokenUuid(UUID tokenUuid);

    // 2. Búsqueda segura multi-tenant para el panel privado del taller
    Optional<Cita> findByIdAndTallerId(Long id, Long tallerId);

    // 3. Obtener citas de un taller en un rango temporal (para pintar el calendario)
    List<Cita> findByTallerIdAndFechaInicioBetween(
            Long tallerId, 
            OffsetDateTime inicio, 
            OffsetDateTime fin
        );

    // 4. Comprobar disponibilidad real de un puesto en un rango temporal
    boolean existsByTallerIdAndPuestoIdAndEstadoInAndFechaInicioLessThanAndFechaFinGreaterThan(
                Long tallerId,
                Long puestoId,
                Collection<EstadoCita> estados,
                OffsetDateTime fin,
                OffsetDateTime inicio
        );

    // 5. Barrera anti-spam (RN-04): validar si la matrícula ya tiene cita activa
    boolean existsByTallerIdAndVehiculoMatriculaAndEstadoIn(
            Long tallerId, 
            String matricula, 
            Collection<EstadoCita> estados
    );

    // 6. Obtener citas de un taller filtrando por rango y estados concretos(disponibilidad pública)
    List<Cita> findByTallerIdAndFechaInicioBetweenAndEstadoIn(
            Long tallerId,
            OffsetDateTime inicio,
            OffsetDateTime fin,
            Collection<EstadoCita> estados
    );
}