package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.Usuario;
import com.saastalleres.backend.dto.auth.AuthResponseDTO;
import com.saastalleres.backend.dto.auth.LoginRequestDTO;
import com.saastalleres.backend.repository.UsuarioRepository;
import com.saastalleres.backend.security.JwtService;
import com.saastalleres.backend.security.UsuarioSecurityDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de procesar la lógica de negocio de autenticación:
 * validación de credenciales con Spring Security y emisión de tokens JWT multi-tenant.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        // 1. Delegamos en Spring Security la validación formal de email y contraseña hasheada con BCrypt
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // 2. Extraemos los detalles de seguridad del usuario autenticado
        UsuarioSecurityDetails userDetails = (UsuarioSecurityDetails) authentication.getPrincipal();

        // 3. Obtenemos la entidad Usuario completa de la base de datos para recuperar metadatos
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + request.email()));

        // 4. Generamos el token JWT firmado que incluye el tallerId en sus claims
        String jwtToken = jwtService.generateToken(userDetails);

        // 5. Construimos y devolvemos la respuesta empaquetada en el DTO inmutable
        return new AuthResponseDTO(
                jwtToken,
                "Bearer",
                usuario.getId(),
                usuario.getTaller().getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol()
        );
    }
}