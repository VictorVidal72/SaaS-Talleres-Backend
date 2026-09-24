package com.saastalleres.backend.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ciclo de vida operativo de un vehículo durante su estancia en taller (RF-11).
 */
@Getter
@RequiredArgsConstructor
public enum EstadoOrdenTrabajo {

    RECEPCIONADO("Vehículo en recepción física tras check-in", false),
    ESPERANDO_PIEZA("Intervención detenida a la espera de recambios", false),
    PIEZA_RECIBIDA("Recambio disponible para reanudar trabajos", false),
    EN_REPARACION("Mecánicos interviniendo activamente en el box", false),
    LISTO_PARA_ENTREGA("Trabajos finalizados y verificado para recogida", false),
    ENTREGADO("Vehículo devuelto al conductor", true);

    private final String descripcion;
    private final boolean esEstadoFinal;
}