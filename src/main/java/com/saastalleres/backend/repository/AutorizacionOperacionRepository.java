package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.AutorizacionOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para las autorizaciones de operaciones imprevistas
 * y auditoría de firma remota del cliente (RF-12, RN-01, RN-07).
 */
@Repository
public interface AutorizacionOperacionRepository extends JpaRepository<AutorizacionOperacion, Long> {

    // 1. Acceso público del conductor mediante el enlace seguro de WhatsApp/SMS
    Optional<AutorizacionOperacion> findByTokenUuid(UUID tokenUuid);

    // 2. Historial de autorizaciones de una orden verificando pertenencia al taller
    List<AutorizacionOperacion> findByOrdenTrabajoIdAndTallerId(Long ordenTrabajoId, Long tallerId);

    // 3. Consulta unitaria segura aislada por tenant
    Optional<AutorizacionOperacion> findByIdAndTallerId(Long id, Long tallerId);
}