package com.saastalleres.backend.controller;

import com.saastalleres.backend.domain.enums.TipoServicio;
import com.saastalleres.backend.dto.cita.CitaPublicaCreateRequestDTO;
import com.saastalleres.backend.dto.cita.CitaResponseDTO;
import com.saastalleres.backend.dto.cita.DisponibilidadDiaDTO;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador de acceso público para clientes finales y conductores.
 * Expone la consulta de disponibilidad horaria, la solicitud de citas web
 * y el seguimiento/cancelación mediante token UUID seguro.
 * 
 * Estas rutas están pensadas para no requerir autenticación JWT previa.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class CitaPublicaController {

    private final CitaService citaService;

    /**
     * 1. Consultar huecos libres en un día específico para un taller y tipo de servicio.
     * Ejemplo: GET /api/v1/public/talleres/1/disponibilidad?fecha=2026-10-15&tipoServicio=ESTANDAR
     */
    @GetMapping("/talleres/{tallerId}/disponibilidad")
    public ResponseEntity<DisponibilidadDiaDTO> obtenerDisponibilidad(
            @PathVariable Long tallerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam TipoServicio tipoServicio
    ) {
        DisponibilidadDiaDTO disponibilidad = citaService.obtenerDisponibilidadPublica(tallerId, fecha, tipoServicio);
        return ResponseEntity.ok(disponibilidad);
    }

    /**
     * 2. Solicitar una nueva cita pública desde la web del taller.
     * Nace en estado PENDIENTE_CONFIRMACION y genera un tokenUuid.
     * Ejemplo: POST /api/v1/public/talleres/1/citas
     */
    @PostMapping("/talleres/{tallerId}/citas")
    public ResponseEntity<CitaResponseDTO> crearCitaPublica(
            @PathVariable Long tallerId,
            @Valid @RequestBody CitaPublicaCreateRequestDTO dto
    ) {
        CitaResponseDTO citaCreada = citaService.crearCitaPublica(tallerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(citaCreada);
    }

    /**
     * 3. Consultar los detalles y estado de una cita mediante su token UUID.
     * Útil para la pantalla de confirmación o enlace enviado al cliente.
     * Ejemplo: GET /api/v1/public/citas/550e8400-e29b-41d4-a716-446655440000
     */
    @GetMapping("/citas/{tokenUuid}")
    public ResponseEntity<CitaResponseDTO> obtenerCitaPorToken(
            @PathVariable UUID tokenUuid
    ) {
        CitaResponseDTO cita = citaService.obtenerCitaPorToken(tokenUuid);
        return ResponseEntity.ok(cita);
    }

    /**
     * 4. Cancelar la cita por parte del conductor usando su enlace público con token UUID.
     * El cuerpo de la petición puede recibir opcionalmente un motivo.
     * Ejemplo: PATCH /api/v1/public/citas/550e8400-e29b-41d4-a716-446655440000/cancelar
     */
    @PatchMapping("/citas/{tokenUuid}/cancelar")
    public ResponseEntity<CitaResponseDTO> cancelarCitaPorToken(
            @PathVariable UUID tokenUuid,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String motivo = (body != null) ? body.get("motivo") : null;
        CitaResponseDTO citaCancelada = citaService.cancelarCitaPorToken(tokenUuid, motivo);
        return ResponseEntity.ok(citaCancelada);
    }
}