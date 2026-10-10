package com.saastalleres.backend.controller;

import com.saastalleres.backend.dto.puesto.PuestoCreateRequestDTO;
import com.saastalleres.backend.dto.puesto.PuestoResponseDTO;
import com.saastalleres.backend.service.PuestoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión de capacidad física del taller (RF-05).
 * Aplica autorización RBAC y control de inquilino multi-tenant (RN-01).
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/puestos")
@RequiredArgsConstructor
public class PuestoController {

    private final PuestoService puestoService;

    /**
     * Da de alta un nuevo box o elevador en el taller.
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('TALLER') and #tallerId == authentication.principal.tallerId)")
    public ResponseEntity<PuestoResponseDTO> crearPuesto(
            @PathVariable Long tallerId,
            @Valid @RequestBody PuestoCreateRequestDTO requestDTO) {

        PuestoResponseDTO nuevoPuesto = puestoService.crearPuesto(tallerId, requestDTO);

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(nuevoPuesto.id())
                .toUri();

        return ResponseEntity.created(ubicacion).body(nuevoPuesto);
    }

    /**
     * Lista toda la infraestructura física del taller (activos e inactivos).
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('TALLER') and #tallerId == authentication.principal.tallerId)")
    public ResponseEntity<List<PuestoResponseDTO>> listarTodos(@PathVariable Long tallerId) {
        return ResponseEntity.ok(puestoService.listarTodosPorTaller(tallerId));
    }

    /**
     * Lista únicamente los puestos operativos para el motor de disponibilidad y reservas.
     */
    @GetMapping("/activos")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('TALLER') and #tallerId == authentication.principal.tallerId)")
    public ResponseEntity<List<PuestoResponseDTO>> listarActivos(@PathVariable Long tallerId) {
        return ResponseEntity.ok(puestoService.listarActivosPorTaller(tallerId));
    }

    /**
     * Conmuta el estado operativo de un puesto (activo / en mantenimiento).
     */
    @PatchMapping("/{puestoId}/estado")
    @PreAuthorize("hasRole('SUPERADMIN') or (hasRole('TALLER') and #tallerId == authentication.principal.tallerId)")
    public ResponseEntity<PuestoResponseDTO> cambiarEstadoOperativo(
            @PathVariable Long tallerId,
            @PathVariable Long puestoId,
            @RequestParam boolean activo) {

        return ResponseEntity.ok(puestoService.cambiarEstadoOperativo(tallerId, puestoId, activo));
    }
}