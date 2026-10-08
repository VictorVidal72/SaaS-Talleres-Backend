package com.saastalleres.backend.controller;

import com.saastalleres.backend.dto.vehiculo.VehiculoCreateRequestDTO;
import com.saastalleres.backend.dto.vehiculo.VehiculoResponseDTO;
import com.saastalleres.backend.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de flota y recepción express de vehículos.
 * Implementa aislamiento estricto por tallerId y control perimetral RBAC.
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;

    /**
     * RF-08 / Recepción Express: Da de alta o actualiza un vehículo en el taller.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TALLER', 'SUPERADMIN') and #tallerId == authentication.principal.tallerId")
    public ResponseEntity<VehiculoResponseDTO> registrarVehiculo(
            @PathVariable Long tallerId,
            @Valid @RequestBody VehiculoCreateRequestDTO dto) {
        
        VehiculoResponseDTO response = vehiculoService.registrarOActualizar(tallerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RN-01: Lista todos los vehículos vinculados al taller autenticado.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TALLER', 'SUPERADMIN') and #tallerId == authentication.principal.tallerId")
    public ResponseEntity<List<VehiculoResponseDTO>> listarFlota(
            @PathVariable Long tallerId) {
        
        return ResponseEntity.ok(vehiculoService.listarFlotaTaller(tallerId));
    }

    /**
     * Búsqueda operativa por matrícula dentro del expediente del taller.
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('TALLER', 'SUPERADMIN') and #tallerId == authentication.principal.tallerId")
    public ResponseEntity<VehiculoResponseDTO> buscarPorMatricula(
            @PathVariable Long tallerId,
            @RequestParam String matricula) {
        
        return vehiculoService.buscarPorMatriculaEnTaller(tallerId, matricula)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * RF-03c: Validación pública por matrícula y teléfono para acceso sin credenciales.
     * Abierto al cliente final para recuperar su enlace de seguimiento (sin requerir JWT de taller).
     */
    @GetMapping("/recuperar")
    public ResponseEntity<VehiculoResponseDTO> recuperarAccesoConductor(
            @PathVariable Long tallerId,
            @RequestParam String matricula,
            @RequestParam String telefono) {
        
        return vehiculoService.buscarPorMatriculaYTelefonoEnTaller(tallerId, matricula, telefono)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}