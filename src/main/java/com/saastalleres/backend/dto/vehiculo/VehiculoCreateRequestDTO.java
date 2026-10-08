package com.saastalleres.backend.dto.vehiculo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada para el alta o recepción de un vehículo en el taller.
 * Representa los datos validados del cliente y su vehículo.
 */
public record VehiculoCreateRequestDTO(

    @NotBlank(message = "La matrícula es obligatoria")
    @Size(min = 6, max = 10, message = "La matrícula debe tener un formato válido")
    @Pattern(
        regexp = "^(\\d{4}[BCDFGHJKLMNPRSTVWXYZ]{3}|[A-Z]{1,2}\\d{4}[A-Z]{0,2})$",
        message = "La matrícula debe cumplir el formato oficial español (ej. 1234BBB o CA1234AB)"
    )
    String matricula,

    @Size(max = 50, message = "La marca no puede superar los 50 caracteres")
    String marca,

    @Size(max = 50, message = "El modelo no puede superar los 50 caracteres")
    String modelo,

    @Size(max = 30, message = "El color no puede superar los 30 caracteres")
    String color,

    @NotBlank(message = "El nombre de contacto es obligatorio")
    @Size(max = 100, message = "El nombre de contacto no puede superar los 100 caracteres")
    String nombreContacto,

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Size(max = 20, message = "El teléfono de contacto no puede superar los 20 caracteres")
    @Pattern(
        regexp = "^[+0-9\\s]{6,20}$", 
        message = "El formato del teléfono de contacto no es válido"
    )
    String telefonoContacto,

    @Email(message = "El formato del email de contacto no es válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    String emailContacto
) {
    /**
     * Constructor compacto para sanitizar datos de entrada críticos.
     */
    public VehiculoCreateRequestDTO {
        if (matricula != null) {
            matricula = matricula.trim().toUpperCase();
        }
        if (nombreContacto != null) {
            nombreContacto = nombreContacto.trim();
        }
        if (telefonoContacto != null) {
            telefonoContacto = telefonoContacto.trim();
        }
        if (emailContacto != null) {
            emailContacto = emailContacto.trim().toLowerCase();
        }
    }
}