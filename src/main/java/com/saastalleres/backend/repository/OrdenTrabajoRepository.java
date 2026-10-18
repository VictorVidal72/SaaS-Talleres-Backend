package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.OrdenTrabajo;
import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para las órdenes de trabajo,
 * historial de reparaciones y tablero Kanban operativo.
 */
@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    // 1. Listar todas las órdenes de trabajo de un taller
    List<OrdenTrabajo> findByTallerId(Long tallerId);

    // 2. Órdenes filtradas por estado para alimentar el tablero Kanban (RF-05)
    List<OrdenTrabajo> findByTallerIdAndEstadoIn(Long tallerId, Collection<EstadoOrdenTrabajo> estados);

    // 3. Búsqueda segura multi-tenant para consultar o actualizar una orden
    Optional<OrdenTrabajo> findByIdAndTallerId(Long id, Long tallerId);

    // 4. Historial clínico de intervenciones de un vehículo concreto en el taller
    List<OrdenTrabajo> findByTallerIdAndVehiculoId(Long tallerId, Long vehiculoId);

    // 5. Localizar la orden originada a partir de una cita previa
    Optional<OrdenTrabajo> findByCitaId(Long citaId);

    // 6. Consulta directa indexada por token UUID para seguimiento público (RN-03, RN-08)
    Optional<OrdenTrabajo> findByTokenUuid(UUID tokenUuid);

    // 7. Verifica la existencia de una orden de trabajo aislada por taller (RN-01).
    boolean existsByIdAndTallerId(Long id, Long tallerId);
}