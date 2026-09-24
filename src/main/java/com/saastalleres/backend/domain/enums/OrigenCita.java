package com.saastalleres.backend.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Canal por el que ingresa la solicitud de cita a la plataforma.
 */
@Getter
@RequiredArgsConstructor
public enum OrigenCita {

    WEB("Reserva pública en línea por el conductor"),
    MANUAL("Registro interno asistido por personal de recepción");

    private final String descripcion;

    public boolean esRegistroInterno() {
        return this == MANUAL;
    }
}