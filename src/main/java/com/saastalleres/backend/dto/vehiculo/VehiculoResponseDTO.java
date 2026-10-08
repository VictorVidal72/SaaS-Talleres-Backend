package com.saastalleres.backend.dto.vehiculo;

import java.time.OffsetDateTime;

/**
 * Contrato de salida con los datos técnicos y de contacto del vehículo.
 * Empleado para alimentar las vistas del panel de taller y la recepción express.
 */
public record VehiculoResponseDTO(
    Long id,
    String matricula,
    String marca,
    String modelo,
    String color,
    String nombreContacto,
    String telefonoContacto,
    String emailContacto,
    OffsetDateTime creadoEn
) {}