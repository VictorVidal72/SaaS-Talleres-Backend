package com.saastalleres.backend.controller;

import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResolverRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResponseDTO;
import com.saastalleres.backend.service.AutorizacionOperacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Adaptador de entrada REST para el canal público de consulta y resolución de
 * micro-autorizaciones de costes imprevistos para el conductor (RF-12, RN-03, RN-07).
 *
 * No requiere autenticación JWT previa. El perímetro está protegido criptográficamente
 * por el identificador opaco UUID remitido en el enlace web.
 */
@RestController
@RequestMapping("/api/v1/public/autorizaciones")
@RequiredArgsConstructor
public class AutorizacionOperacionPublicaController {

    private final AutorizacionOperacionService autorizacionService;

    /**
     * Consulta pública del detalle de un sobrecoste imprevisto mediante su token UUID.
     * Ejemplo: GET /api/v1/public/autorizaciones/550e8400-e29b-41d4-a716-446655440000
     */
    @GetMapping("/{tokenUuid}")
    public ResponseEntity<AutorizacionResponseDTO> consultarPorToken(@PathVariable UUID tokenUuid) {
        AutorizacionResponseDTO response = autorizacionService.consultarPorToken(tokenUuid);
        return ResponseEntity.ok(response);
    }

    /**
     * Resolución del conductor: Aprobación o rechazo del sobrecoste imprevisto (RF-12).
     * Sella la auditoría digital con IP remota y User-Agent del dispositivo (RN-07).
     * Ejemplo: POST /api/v1/public/autorizaciones/550e8400-e29b-41d4-a716-446655440000/resolver
     */
    @PostMapping("/{tokenUuid}/resolver")
    public ResponseEntity<AutorizacionResponseDTO> resolverAutorizacion(
            @PathVariable UUID tokenUuid,
            @Valid @RequestBody AutorizacionResolverRequestDTO dto,
            @RequestHeader(value = "User-Agent", defaultValue = "Desconocido") String userAgent,
            HttpServletRequest request
    ) {
        String ipRemota = request.getRemoteAddr();
        AutorizacionResponseDTO response = autorizacionService.resolverAutorizacion(
                tokenUuid, dto, ipRemota, userAgent);

        return ResponseEntity.ok(response);
    }
}