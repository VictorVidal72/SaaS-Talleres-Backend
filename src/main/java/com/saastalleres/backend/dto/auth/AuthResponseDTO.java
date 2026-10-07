package com.saastalleres.backend.dto.auth;

/**
 * Contrato de respuesta tras una autenticación exitosa.
 * Contiene el token JWT y los metadatos de sesión requeridos por el frontend.
 */
public record AuthResponseDTO(
    String token,
    String tipo,
    Long usuarioId,
    Long tallerId,
    String email,
    String nombre,
    String rol
) {
    /**
     * Constructor compacto: garantiza que si el tipo viene nulo o vacío,
     * tome por defecto el esquema estándar "Bearer".
     */
    public AuthResponseDTO {
        if (tipo == null || tipo.isBlank()) {
            tipo = "Bearer";
        }
    }

    /**
     * Constructor de conveniencia para instanciar la respuesta sin obligar
     * a pasar manualmente el literal "Bearer" en el servicio.
     */
    public AuthResponseDTO(
        String token,
        Long usuarioId,
        Long tallerId,
        String email,
        String nombre,
        String rol
    ) {
        this(token, "Bearer", usuarioId, tallerId, email, nombre, rol);
    }
}