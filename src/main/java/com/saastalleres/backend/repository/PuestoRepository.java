package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.Puesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la gestión de recursos físicos
 * (elevadores / boxes) con aislamiento multi-tenant.
 */
@Repository
public interface PuestoRepository extends JpaRepository<Puesto, Long> {

    // 1. Listar toda la infraestructura física de un taller
    List<Puesto> findByTallerId(Long tallerId);

    // 2. Obtener puestos activos disponibles para el motor de reservas
    List<Puesto> findByTallerIdAndActivoTrue(Long tallerId);

    // 3. Evitar nombres duplicados de boxes dentro de un mismo taller
    boolean existsByTallerIdAndNombre(Long tallerId, String nombre);

    // 4. Búsqueda segura de un puesto validando la pertenencia al inquilino
    Optional<Puesto> findByIdAndTallerId(Long id, Long tallerId);
}