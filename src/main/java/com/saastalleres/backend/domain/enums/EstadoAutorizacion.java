package com.saastalleres.backend.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estados de la solicitud de aprobación de operaciones o piezas imprevistas (RF-12, RN-07).
 */
@Getter
@RequiredArgsConstructor
public enum EstadoAutorizacion {

    PENDIENTE("Pendiente de respuesta por parte del conductor", false),
    AUTORIZADO("Presupuesto extra aprobado digitalmente", true),
    RECHAZADO("Operación descartada por el cliente", true);

    private final String descripcion;
    private final boolean esDecisionTomada;

    /**
     * Determina si la autorización aún admite interacción del cliente.
     */
    public boolean puedeResolver() {
        return !this.esDecisionTomada;
    }
}