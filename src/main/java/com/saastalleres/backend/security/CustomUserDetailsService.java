package com.saastalleres.backend.security;
import com.saastalleres.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de infraestructura que busca al usuario en PostgreSQL y lo retorna
 * envuelto en UsuarioSecurityDetails para el ciclo de autenticación de Spring.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioSecurityDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No se encontró ningún usuario con el correo electrónico proporcionado: " + email
                ));
    }
}