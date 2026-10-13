package com.saastalleres.backend.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Máquina de estados de la reserva en agenda y sus transiciones válidas.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoCita {

    PENDIENTE_CONFIRMACION(true, false),
    CONFIRMADA(false, true),
    PROPUESTA_TALLER(true, false),
    FINALIZADA(false, false),
    RECHAZADA(false, false),
    CANCELADA(false, false);

    private final boolean permiteReprogramacion;
    private final boolean ocupaSlotDefinitivo;

    /**
     * Determina si la cita está en un estado terminal e inmutable.
     */
    public boolean esFinal() {
        return this == RECHAZADA || this == CANCELADA || this == FINALIZADA;
    }
}