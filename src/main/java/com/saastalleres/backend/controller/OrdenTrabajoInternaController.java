package com.saastalleres.backend.controller;

import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoCreateRequestDTO;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoEstadoUpdateRequestDTO;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoResponseDTO;
import com.saastalleres.backend.service.OrdenTrabajoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.Collection;
import java.util.List;

/**
 * Controlador para la gestión interna de órdenes de trabajo y ciclo operativo del taller.
 * Expone la recepción física (check-in), transición del tablero Kanban y consultas multi-tenant.
 * 
 * Rutas protegidas mediante autenticación JWT y aislamiento de inquilino (RN-01).
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/ordenes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN') or (hasRole('TALLER') and #tallerId == authentication.principal.tallerId)")
public class OrdenTrabajoInternaController {

    private final OrdenTrabajoService ordenTrabajoService;

    /**
     * 1. Registrar la recepción física del vehículo (check-in) y abrir una orden de trabajo.
     * Si viene de cita previa, la cita transiciona automáticamente a FINALIZADA.
     * Ejemplo: POST /api/v1/talleres/1/ordenes
     */
    @PostMapping
    public ResponseEntity<OrdenTrabajoResponseDTO> crearOrdenTrabajo(
            @PathVariable Long tallerId,
            @Valid @RequestBody OrdenTrabajoCreateRequestDTO dto
    ) {
        OrdenTrabajoResponseDTO nuevaOrden = ordenTrabajoService.crearOrdenTrabajo(tallerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaOrden);
    }

    /**
     * 2. Actualizar el estado operativo de la orden en el tablero Kanban y notas técnicas.
     * Si pasa a ENTREGADO, sella automáticamente la fecha de entrega física.
     * Ejemplo: PATCH /api/v1/talleres/1/ordenes/10/estado
     */
    @PatchMapping("/{ordenId}/estado")
    public ResponseEntity<OrdenTrabajoResponseDTO> actualizarEstado(
            @PathVariable Long tallerId,
            @PathVariable Long ordenId,
            @Valid @RequestBody OrdenTrabajoEstadoUpdateRequestDTO dto
    ) {
        OrdenTrabajoResponseDTO ordenActualizada = ordenTrabajoService.actualizarEstado(tallerId, ordenId, dto);
        return ResponseEntity.ok(ordenActualizada);
    }

    /**
     * 3. Obtener el detalle de una orden de trabajo específica dentro del taller.
     * Ejemplo: GET /api/v1/talleres/1/ordenes/10
     */
    @GetMapping("/{ordenId}")
    public ResponseEntity<OrdenTrabajoResponseDTO> obtenerPorId(
            @PathVariable Long tallerId,
            @PathVariable Long ordenId
    ) {
        OrdenTrabajoResponseDTO orden = ordenTrabajoService.obtenerPorId(tallerId, ordenId);
        return ResponseEntity.ok(orden);
    }

    /**
     * 4. Listar órdenes del taller, con soporte opcional de filtrado por estados (para pintar el Kanban).
     * Ejemplo todas: GET /api/v1/talleres/1/ordenes
     * Ejemplo Kanban activo: GET /api/v1/talleres/1/ordenes?estados=RECEPCIONADO,EN_REPARACION,ESPERANDO_PIEZA
     */
    @GetMapping
    public ResponseEntity<List<OrdenTrabajoResponseDTO>> listarOrdenes(
            @PathVariable Long tallerId,
            @RequestParam(required = false) Collection<EstadoOrdenTrabajo> estados
    ) {
        List<OrdenTrabajoResponseDTO> ordenes = (estados != null && !estados.isEmpty())
                ? ordenTrabajoService.listarPorEstados(tallerId, estados)
                : ordenTrabajoService.listarPorTaller(tallerId);
        return ResponseEntity.ok(ordenes);
    }

    /**
     * 5. Consultar el historial completo de órdenes de trabajo de un vehículo en este taller.
     * Ejemplo: GET /api/v1/talleres/1/ordenes/vehiculos/5
     */
    @GetMapping("/vehiculos/{vehiculoId}")
    public ResponseEntity<List<OrdenTrabajoResponseDTO>> listarHistorialVehiculo(
            @PathVariable Long tallerId,
            @PathVariable Long vehiculoId
    ) {
        List<OrdenTrabajoResponseDTO> historial = ordenTrabajoService.listarHistorialVehiculo(tallerId, vehiculoId);
        return ResponseEntity.ok(historial);
    }
}