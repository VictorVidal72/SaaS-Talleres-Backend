package com.saastalleres.backend.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Clasificación operativa de la intervención con duración intrínseca (RF-09).
 */
@Getter
@RequiredArgsConstructor
public enum TipoServicio {

    ESTANDAR(45, "Mantenimiento o servicio estándar"),
    DIAGNOSIS(30, "Recepción técnica para valoración de avería desconocida");

    private final int duracionMinutos;
    private final String descripcion;

    /**
     * Calcula la fecha/hora de finalización a partir del momento de inicio.
     */
    public OffsetDateTime calcularFin(OffsetDateTime inicio) {
        if (inicio == null) {
            return null;
        }
        return inicio.plusMinutes(this.duracionMinutos);
    }
}