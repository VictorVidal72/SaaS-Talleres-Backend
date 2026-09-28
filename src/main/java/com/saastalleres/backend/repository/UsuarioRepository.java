package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la gestión del personal del taller
 * y autenticación de seguridad (Spring Security + JWT).
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // 1. Para que Spring Security busque al usuario por email al hacer login
    Optional<Usuario> findByEmail(String email);

    // 2. Para validar rápidamente que el email no esté repetido al registrar un empleado
    boolean existsByEmail(String email);

    // 3. Aislamiento Multi-Tenant: listar todos los empleados de un taller concreto
    List<Usuario> findByTallerId(Long tallerId);

    // 4. Listar solo los empleados activos de un taller (para asignarles trabajos)
    List<Usuario> findByTallerIdAndActivoTrue(Long tallerId);
}