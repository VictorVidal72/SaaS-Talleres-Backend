package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.AutorizacionOperacion;
import com.saastalleres.backend.domain.entity.OrdenTrabajo;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.enums.EstadoAutorizacion;
import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionCreateRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionOperacionMapper;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResolverRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResponseDTO;
import com.saastalleres.backend.exception.BusinessRuleException;
import com.saastalleres.backend.exception.ResourceNotFoundException;
import com.saastalleres.backend.repository.AutorizacionOperacionRepository;
import com.saastalleres.backend.repository.OrdenTrabajoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación que implementa la lógica de negocio para la gestión
 * y resolución remota de micro-autorizaciones de costes imprevistos (RF-12, RN-01, RN-07).
 */
@Service
@RequiredArgsConstructor
public class AutorizacionOperacionService {

    private final AutorizacionOperacionRepository autorizacionRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final TallerRepository tallerRepository;
    private final AutorizacionOperacionMapper autorizacionMapper;

    /**
     * Caso de uso: El taller crea una solicitud de autorización por un sobrecoste imprevisto (RF-12).
     */
    @Transactional
    public AutorizacionResponseDTO solicitarAutorizacion(Long tallerId, AutorizacionCreateRequestDTO dto) {
        // 1. Validar existencia del taller (tenant)
        Taller taller = tallerRepository.findById(tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Taller no encontrado con ID: " + tallerId));

        // 2. Validar que la orden de trabajo existe y pertenece estrictamente a este taller (RN-01)
        OrdenTrabajo orden = ordenTrabajoRepository.findByIdAndTallerId(dto.ordenTrabajoId(), tallerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orden de trabajo no encontrada con ID: " + dto.ordenTrabajoId() + " para el taller: " + tallerId));

        // 3. Regla de negocio: No se pueden solicitar autorizaciones sobre órdenes ya entregadas
        if (orden.getEstado() == EstadoOrdenTrabajo.ENTREGADO) {
            throw new BusinessRuleException("No se pueden solicitar autorizaciones para una orden de trabajo ya entregada");
        }

        // 4. Mapear, persistir en estado PENDIENTE con su token_uuid generado y devolver DTO
        AutorizacionOperacion nuevaAutorizacion = autorizacionMapper.toEntity(dto, taller, orden);
        AutorizacionOperacion autorizacionGuardada = autorizacionRepository.save(nuevaAutorizacion);

        return autorizacionMapper.toResponseDTO(autorizacionGuardada);
    }

    /**
     * Caso de uso: Consulta pública del estado y presupuesto extra mediante token UUID (sin login) (RN-03, RN-08).
     */
    @Transactional(readOnly = true)
    public AutorizacionResponseDTO consultarPorToken(UUID tokenUuid) {
        AutorizacionOperacion autorizacion = autorizacionRepository.findByTokenUuid(tokenUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Autorización no encontrada con el token proporcionado"));

        return autorizacionMapper.toResponseDTO(autorizacion);
    }

    /**
     * Caso de uso: El conductor aprueba o rechaza el presupuesto extra desde su móvil con evidencia digital (RF-12, RN-07).
     */
    @Transactional
    public AutorizacionResponseDTO resolverAutorizacion(
            UUID tokenUuid,
            AutorizacionResolverRequestDTO dto,
            String ipRemota,
            String userAgent
    ) {
        // 1. Recuperar la autorización por su identificador público opaco
        AutorizacionOperacion autorizacion = autorizacionRepository.findByTokenUuid(tokenUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Autorización no encontrada con el token proporcionado"));

        // 2. Regla de negocio e inmutabilidad: Verificar si aún admite resolución
        if (!autorizacion.getEstado().puedeResolver()) {
            throw new BusinessRuleException(
                    "La autorización ya fue resuelta previamente con estado: " + autorizacion.getEstado());
        }

        // 3. Aplicar la decisión del conductor
        if (Boolean.TRUE.equals(dto.aprobado())) {
            autorizacion.setEstado(EstadoAutorizacion.AUTORIZADO);
        } else {
            autorizacion.setEstado(EstadoAutorizacion.RECHAZADO);
        }

        // 4. Auditoría de consentimiento legal y firma digital (RN-07)
        autorizacion.setFechaResolucion(OffsetDateTime.now());
        autorizacion.setIpConsentimiento(ipRemota);
        autorizacion.setUserAgentConsentimiento(userAgent);

        AutorizacionOperacion autorizacionActualizada = autorizacionRepository.save(autorizacion);
        return autorizacionMapper.toResponseDTO(autorizacionActualizada);
    }

    /**
     * Caso de uso: Consulta interna de todas las autorizaciones asociadas a una orden de trabajo (RN-01).
     */
    @Transactional(readOnly = true)
    public List<AutorizacionResponseDTO> listarPorOrden(Long tallerId, Long ordenTrabajoId) {
        // Validar que la orden existe y pertenece al taller antes de devolver su historial
        if (!ordenTrabajoRepository.existsByIdAndTallerId(ordenTrabajoId, tallerId)) {
            throw new ResourceNotFoundException(
                    "Orden de trabajo no encontrada con ID: " + ordenTrabajoId + " para el taller: " + tallerId);
        }

        return autorizacionRepository.findByOrdenTrabajoIdAndTallerId(ordenTrabajoId, tallerId)
                .stream()
                .map(autorizacionMapper::toResponseDTO)
                .toList();
    }

    /**
     * Caso de uso: Consulta interna de una autorización individual con verificación multi-tenant (RN-01).
     */
    @Transactional(readOnly = true)
    public AutorizacionResponseDTO consultarPorIdYTaller(Long tallerId, Long autorizacionId) {
        AutorizacionOperacion autorizacion = autorizacionRepository.findByIdAndTallerId(autorizacionId, tallerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Autorización no encontrada con ID: " + autorizacionId + " para el taller: " + tallerId));

        return autorizacionMapper.toResponseDTO(autorizacion);
    }
}