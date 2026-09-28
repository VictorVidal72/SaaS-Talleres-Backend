package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.TallerVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la tabla asociativa Taller-Vehiculo
 * (Historial y ficha de clientes por taller).
 */
@Repository
public interface TallerVehiculoRepository extends JpaRepository<TallerVehiculo, Long> {

    // 1. Localizar el expediente específico de un coche en un taller
    Optional<TallerVehiculo> findByTallerIdAndVehiculoId(Long tallerId, Long vehiculoId);

    // 2. Comprobar si ya existe el vínculo entre el taller y el coche
    boolean existsByTallerIdAndVehiculoId(Long tallerId, Long vehiculoId);

    // 3. Listar todos los expedientes de vehículos vinculados a un taller (Aislamiento Multi-Tenant)
    List<TallerVehiculo> findByTallerId(Long tallerId);
}