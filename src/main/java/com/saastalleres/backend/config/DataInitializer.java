package com.saastalleres.backend.config;

import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.Usuario;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.repository.PuestoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import com.saastalleres.backend.repository.UsuarioRepository;
import com.saastalleres.backend.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeder de datos iniciales para entornos de desarrollo local.
 * Desactivado formalmente en suites de test automatizadas para garantizar
 * el aislamiento y la idempotencia de las pruebas de integración.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TallerRepository tallerRepository;
    private final UsuarioRepository usuarioRepository;
    private final PuestoRepository puestoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verificando siembra de datos de desarrollo...");

        // 1. Taller (Inquilino inicial)
        Taller taller = tallerRepository.findBySlug("taller-sanlucar-demo")
                .orElseGet(() -> {
                    Taller nuevoTaller = new Taller();
                    nuevoTaller.setNombreComercial("Taller Sanlúcar Demo");
                    nuevoTaller.setCif("B12345678");
                    nuevoTaller.setSlug("taller-sanlucar-demo");
                    nuevoTaller.setDireccion("Calle Central 1");
                    nuevoTaller.setCiudad("Sanlúcar de Barrameda");
                    nuevoTaller.setTelefono("956000111");
                    nuevoTaller.setEmail("info@tallersanlucar.com");
                    nuevoTaller.setActivo(true);
                    return tallerRepository.save(nuevoTaller);
                });

        // 2. Usuario Administrador del Taller
        if (!usuarioRepository.existsByEmail("admin@taller.com")) {
            Usuario admin = new Usuario();
            admin.setTaller(taller);
            admin.setNombre("Carlos Administrador");
            admin.setEmail("admin@taller.com");
            admin.setPasswordHash(passwordEncoder.encode("Password123!"));
            admin.setRol("ADMIN_TALLER");
            admin.setActivo(true);
            usuarioRepository.save(admin);
        }

        // 3. Puesto físico de trabajo
        if (puestoRepository.count() == 0) {
            Puesto puesto = new Puesto();
            puesto.setTaller(taller);
            puesto.setNombre("Elevador 1");
            puesto.setActivo(true);
            puestoRepository.save(puesto);
        }

        // 4. Vehículo de prueba (Clave natural: matrícula)
        if (!vehiculoRepository.existsByMatricula("1234-BBB")) {
            Vehiculo vehiculo = new Vehiculo();
            vehiculo.setMatricula("1234-BBB");
            vehiculo.setMarca("Seat");
            vehiculo.setModelo("Ibiza");
            vehiculo.setColor("Blanco");
            vehiculo.setNombreContacto("Juan Pérez");
            vehiculo.setTelefonoContacto("600123456");
            vehiculo.setEmailContacto("juan@example.com");
            vehiculoRepository.save(vehiculo);
        }

        log.info("Estado de datos de desarrollo verificado correctamente.");
    }
}