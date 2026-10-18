package com.saastalleres.backend.controller;

import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoResponseDTO;
import com.saastalleres.backend.service.OrdenTrabajoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controlador de acceso público para seguimiento de reparaciones por parte del conductor.
 * Expone la consulta del estado de la orden mediante token UUID seguro.
 * 
 * Estas rutas están pensadas para no requerir autenticación JWT previa.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class OrdenTrabajoPublicaController {

    private final OrdenTrabajoService ordenTrabajoService;

    /**
     * Consultar los detalles y estado de avance de una orden de trabajo mediante su token UUID.
     * Útil para el visor web del cliente recibido por SMS/WhatsApp o correo.
     * Ejemplo: GET /api/v1/public/ordenes/550e8400-e29b-41d4-a716-446655440000
     */
    @GetMapping("/ordenes/{tokenUuid}")
    public ResponseEntity<OrdenTrabajoResponseDTO> obtenerPorTokenUuid(@PathVariable UUID tokenUuid){
        OrdenTrabajoResponseDTO orden = ordenTrabajoService.obtenerPorTokenUuid(tokenUuid);
        return ResponseEntity.ok(orden);
    }
}