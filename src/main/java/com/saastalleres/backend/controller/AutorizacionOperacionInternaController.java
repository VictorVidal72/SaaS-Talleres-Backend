package com.saastalleres.backend.controller;

import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionCreateRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResponseDTO;
import com.saastalleres.backend.service.AutorizacionOperacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para la gestión interna de autorizaciones imprevistas (RF-12).
 * Rutas securizadas mediante JWT y contexto estricto de taller multi-tenant (RN-01).
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/autorizaciones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN') or (hasRole('TALLER') and #tallerId == authentication.principal.tallerId)")
public class AutorizacionOperacionInternaController {

    private final AutorizacionOperacionService autorizacionService;

    /**
     * Registro de una nueva solicitud de sobrecoste imprevisto por parte del taller (RF-12).
     * Ejemplo: POST /api/v1/talleres/1/autorizaciones
     */
    @PostMapping
    public ResponseEntity<AutorizacionResponseDTO> solicitarAutorizacion(
            @PathVariable Long tallerId,
            @Valid @RequestBody AutorizacionCreateRequestDTO dto
    ) {
        AutorizacionResponseDTO response = autorizacionService.solicitarAutorizacion(tallerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta interna de todas las autorizaciones imprevistas de una orden de trabajo (RN-01).
     * Ejemplo: GET /api/v1/talleres/1/autorizaciones/ordenes/45
     */
    @GetMapping("/ordenes/{ordenTrabajoId}")
    public ResponseEntity<List<AutorizacionResponseDTO>> listarPorOrden(
            @PathVariable Long tallerId,
            @PathVariable Long ordenTrabajoId
    ) {
        List<AutorizacionResponseDTO> response = autorizacionService.listarPorOrden(tallerId, ordenTrabajoId);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta unitaria de una autorización por su ID interno dentro del taller (RN-01).
     * Ejemplo: GET /api/v1/talleres/1/autorizaciones/10
     */
    @GetMapping("/{autorizacionId}")
    public ResponseEntity<AutorizacionResponseDTO> consultarPorIdYTaller(
            @PathVariable Long tallerId,
            @PathVariable Long autorizacionId
    ) {
        AutorizacionResponseDTO response = autorizacionService.consultarPorIdYTaller(tallerId, autorizacionId);
        return ResponseEntity.ok(response);
    }
}