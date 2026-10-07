package com.saastalleres.backend.security;

import com.saastalleres.backend.domain.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador de seguridad que envuelve a la entidad Usuario implementando UserDetails.
 * Provee acceso inmediato en memoria al tallerId y rol para el aislamiento multi-tenant.
 */
public class UsuarioSecurityDetails implements UserDetails {

    @Getter
    private final Long usuarioId;

    @Getter
    private final Long tallerId;

    @Getter
    private final String nombre;

    private final String email;
    private final String passwordHash;
    private final boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioSecurityDetails(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.tallerId = usuario.getTaller() != null ? usuario.getTaller().getId() : null;
        this.nombre = usuario.getNombre();
        this.email = usuario.getEmail();
        this.passwordHash = usuario.getPasswordHash();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());

        // Aseguramos el prefijo ROLE_ requerido por hasRole() en Spring Security
        String rolLimpio = usuario.getRol().startsWith("ROLE_") 
                ? usuario.getRol() 
                : "ROLE_" + usuario.getRol();
                
        this.authorities = List.of(new SimpleGrantedAuthority(rolLimpio));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}