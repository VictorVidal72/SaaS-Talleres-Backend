package com.saastalleres.backend.config;

import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.Usuario;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.repository.PuestoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import com.saastalleres.backend.repository.UsuarioRepository;
import com.saastalleres.backend.repository.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Semilla mínima indispensable para validar persistencia y habilitar
 * el login de pruebas en la siguiente fase de seguridad.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TallerRepository tallerRepository;
    private final UsuarioRepository usuarioRepository;
    private final PuestoRepository puestoRepository;
    private final VehiculoRepository vehiculoRepository;

    public DataInitializer(
            TallerRepository tallerRepository,
            UsuarioRepository usuarioRepository,
            PuestoRepository puestoRepository,
            VehiculoRepository vehiculoRepository
    ) {
        this.tallerRepository = tallerRepository;
        this.usuarioRepository = usuarioRepository;
        this.puestoRepository = puestoRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Evita duplicar datos si ya existen registros
        if (tallerRepository.count() > 0) {
            log.info("Base de datos ya inicializada. DataInitializer omitido.");
            return;
        }

        log.info("Sembrando registros mínimos de prueba...");

        // 1. Inquilino (Taller)
        Taller taller = new Taller();
        taller.setNombreComercial("Taller Sanlúcar Demo");
        taller.setCif("B12345678");
        taller.setSlug("taller-sanlucar-demo");
        taller.setDireccion("Calle Central 1");
        taller.setCiudad("Sanlúcar de Barrameda");
        taller.setTelefono("956000111");
        taller.setEmail("info@tallersanlucar.com");
        taller.setActivo(true);
        taller = tallerRepository.save(taller);

        // 2. Usuario Administrador (Contraseña plana: "Password123!")
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Usuario admin = new Usuario();
        admin.setTaller(taller);
        admin.setNombre("Carlos Administrador");
        admin.setEmail("admin@taller.com");
        admin.setPasswordHash(encoder.encode("Password123!"));
        admin.setRol("ROLE_ADMIN_TALLER");
        admin.setActivo(true);
        usuarioRepository.save(admin);

        // 3. Un Puesto físico
        Puesto puesto = new Puesto();
        puesto.setTaller(taller);
        puesto.setNombre("Elevador 1");
        puesto.setActivo(true);
        puestoRepository.save(puesto);

        // 4. Un Vehículo de prueba
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMatricula("1234-BBB");
        vehiculo.setMarca("Seat");
        vehiculo.setModelo("Ibiza");
        vehiculo.setColor("Blanco");
        vehiculo.setNombreContacto("Juan Pérez");
        vehiculo.setTelefonoContacto("600123456");
        vehiculo.setEmailContacto("juan@example.com");
        vehiculoRepository.save(vehiculo);

        log.info("Semilla mínima creada con éxito: Taller ID {}, Usuario admin@taller.com listo.", taller.getId());
    }
}