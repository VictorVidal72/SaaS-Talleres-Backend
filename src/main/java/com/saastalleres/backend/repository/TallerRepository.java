package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.Taller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad raíz Taller (Inquilino).
 */
@Repository
public interface TallerRepository extends JpaRepository<Taller, Long> {

    Optional<Taller> findBySlug(String slug);

    Optional<Taller> findByCif(String cif);

    boolean existsBySlug(String slug);

    boolean existsByCif(String cif);
}