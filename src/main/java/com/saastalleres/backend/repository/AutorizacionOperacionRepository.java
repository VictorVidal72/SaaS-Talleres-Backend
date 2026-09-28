package com.saastalleres.backend.repository;

import com.saastalleres.backend.domain.entity.AutorizacionOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para las autorizaciones de operaciones imprevistas
 * y auditoría de firma remota del cliente (RF-04, RN-03).
 */
@Repository
public interface AutorizacionOperacionRepository extends JpaRepository<AutorizacionOperacion, Long> {

    // 1. Acceso público del conductor mediante el enlace seguro de WhatsApp/SMS
    Optional<AutorizacionOperacion> findByTokenUuid(UUID tokenUuid);

    // 2. Historial de ampliaciones solicitadas para una orden de trabajo concreta
    List<AutorizacionOperacion> findByOrdenTrabajoId(Long ordenTrabajoId);

    // 3. Consulta defensiva verificando la pertenencia al taller autenticado
    Optional<AutorizacionOperacion> findByIdAndOrdenTrabajoTallerId(Long id, Long tallerId);
}