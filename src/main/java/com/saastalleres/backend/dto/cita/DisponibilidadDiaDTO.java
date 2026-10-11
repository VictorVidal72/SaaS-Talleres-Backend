package com.saastalleres.backend.dto.cita;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Contrato de respuesta para la disponibilidad pública de un día.
 * Devuelve la lista de horas de inicio que cuentan con al menos un elevador libre.
 */
public record DisponibilidadDiaDTO(
        LocalDate fecha,
        List<LocalTime> horasDisponibles
) {}