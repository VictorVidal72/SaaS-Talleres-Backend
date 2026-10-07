package com.saastalleres.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Contrato de entrada para la autenticación de usuarios.
 * Implementado como Java Record inmutable con sanitización de credenciales.
 */
public record LoginRequestDTO(

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email no es válido")
    String email,

    @NotBlank(message = "La contraseña no puede estar vacía")
    String password
) {
    /**
     * Constructor compacto para normalizar el email eliminando espacios
     * accidentales y convirtiéndolo a minúsculas.
     */
    public LoginRequestDTO {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}