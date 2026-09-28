package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la ficha técnica global de Vehículos
 * y datos de contacto de su conductor habitual (modelo sin contraseñas).
 */
@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // 1. Para buscar el coche por su clave natural única (matrícula)
    Optional<Vehiculo> findByMatricula(String matricula);

    // 2. Para comprobar de forma rápida si el coche ya está dado de alta en la BD
    boolean existsByMatricula(String matricula);
}