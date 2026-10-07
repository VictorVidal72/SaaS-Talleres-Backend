package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.Usuario;
import com.saastalleres.backend.dto.auth.LoginRequestDTO;
import com.saastalleres.backend.repository.TallerRepository;
import com.saastalleres.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String CIF_PRUEBA = "B99887766";
    private final String EMAIL_PRUEBA = "admin@talleresperez.com";
    private final String PASSWORD_PRUEBA = "Password123!";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Reutilizar o crear usando un CIF propio de test aislado
        Taller taller = tallerRepository.findByCif(CIF_PRUEBA)
                .orElseGet(() -> tallerRepository.save(
                        Taller.builder()
                                .nombreComercial("Talleres Pérez")
                                .cif(CIF_PRUEBA)
                                .slug("talleres-perez-test")
                                .telefono("600112233")
                                .email("contacto@talleresperez.com")
                                .direccion("Calle Motor 12")
                                .ciudad("Sanlúcar de Barrameda")
                                .activo(true)
                                .build()
                ));

        // 2. Limpieza y preparación aislada del usuario de prueba
        usuarioRepository.findByEmail(EMAIL_PRUEBA).ifPresent(usuarioRepository::delete);

        Usuario usuario = Usuario.builder()
                .taller(taller)
                .email(EMAIL_PRUEBA)
                .passwordHash(passwordEncoder.encode(PASSWORD_PRUEBA))
                .nombre("Pedro Pérez")
                .rol("ADMIN_TALLER")
                .activo(true)
                .build();
        usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Debe autenticar correctamente y retornar token JWT con metadatos del taller")
    void login_Exitoso() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO(EMAIL_PRUEBA, PASSWORD_PRUEBA);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value(EMAIL_PRUEBA))
                .andExpect(jsonPath("$.rol").value("ADMIN_TALLER"))
                .andExpect(jsonPath("$.tallerId").isNumber());
    }

    @Test
    @DisplayName("Debe rechazar la petición con 401 Unauthorized cuando la contraseña es incorrecta")
    void login_CredencialesInvalidas() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO(EMAIL_PRUEBA, "PasswordErronea999");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe retornar 400 Bad Request cuando el DTO de entrada no cumple las validaciones de formato")
    void login_ValidacionDTO_FormatoInvalido() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("formato-no-valido", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe bloquear con 403 Forbidden el acceso a rutas protegidas sin cabecera Bearer")
    void accesoProtegido_SinToken_Rechazado() throws Exception {
        mockMvc.perform(get("/api/v1/talleres/1/presupuestos"))
                .andExpect(status().isForbidden());
    }
}