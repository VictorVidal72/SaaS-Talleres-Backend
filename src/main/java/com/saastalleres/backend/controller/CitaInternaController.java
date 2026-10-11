package com.saastalleres.backend.controller;

import com.saastalleres.backend.dto.cita.CitaInternaCreateRequestDTO;
import com.saastalleres.backend.dto.cita.CitaResponseDTO;
import com.saastalleres.backend.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión interna de citas desde el panel operativo del taller.
 * Expone la consulta del calendario por rangos, creación directa de citas,
 * confirmación con asignación de puesto y cancelación interna.
 * 
 * Rutas protegidas mediante autenticación JWT y contexto de taller (multi-tenant).
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/citas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN') or (hasRole('TALLER') and #tallerId == authentication.principal.tallerId)")
public class CitaInternaController {

    private final CitaService citaService;

    /**
     * 1. Listar citas dentro de un rango temporal (para pintar la agenda/calendario).
     * Ejemplo: GET /api/v1/talleres/1/citas?inicio=2026-10-15T00:00:00Z&fin=2026-10-15T23:59:59Z
     */
    @GetMapping
    public ResponseEntity<List<CitaResponseDTO>> listarCitasPorRango(
            @PathVariable Long tallerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fin
    ) {
        List<CitaResponseDTO> citas = citaService.listarCitasPorRango(tallerId, inicio, fin);
        return ResponseEntity.ok(citas);
    }

    /**
     * 2. Registrar una cita de manera interna (creada por teléfono o en mostrador).
     * Ejemplo: POST /api/v1/talleres/1/citas
     */
    @PostMapping
    public ResponseEntity<CitaResponseDTO> crearCitaInterna(
            @PathVariable Long tallerId,
            @Valid @RequestBody CitaInternaCreateRequestDTO dto
    ) {
        CitaResponseDTO citaCreada = citaService.crearCitaInterna(tallerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(citaCreada);
    }

    /**
     * 3. Confirmar una cita pública asignándole un elevador/puesto disponible.
     * Ejemplo: PATCH /api/v1/talleres/1/citas/10/confirmar?puestoId=2
     */
    @PatchMapping("/{citaId}/confirmar")
    public ResponseEntity<CitaResponseDTO> confirmarCitaPublica(
            @PathVariable Long tallerId,
            @PathVariable Long citaId,
            @RequestParam Long puestoId
    ) {
        CitaResponseDTO citaConfirmada = citaService.confirmarCitaPublica(tallerId, citaId, puestoId);
        return ResponseEntity.ok(citaConfirmada);
    }

    /**
     * 4. Cancelar una cita desde el panel de gestión del taller.
     * Ejemplo: PATCH /api/v1/talleres/1/citas/10/cancelar
     */
    @PatchMapping("/{citaId}/cancelar")
    public ResponseEntity<CitaResponseDTO> cancelarCita(
            @PathVariable Long tallerId,
            @PathVariable Long citaId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String motivo = (body != null) ? body.get("motivo") : null;
        CitaResponseDTO citaCancelada = citaService.cancelarCita(tallerId, citaId, motivo);
        return ResponseEntity.ok(citaCancelada);
    }
}