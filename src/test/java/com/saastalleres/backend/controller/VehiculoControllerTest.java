package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.dto.vehiculo.VehiculoCreateRequestDTO;
import com.saastalleres.backend.dto.vehiculo.VehiculoResponseDTO;
import com.saastalleres.backend.service.VehiculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VehiculoControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private VehiculoService vehiculoService;

    @InjectMocks
    private VehiculoController vehiculoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehiculoController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("POST /api/v1/talleres/{id}/vehiculos - Retorna 201 Created cuando el payload es válido")
    void debeRegistrarVehiculoExitosamente() throws Exception {
        Long tallerId = 1L;
        VehiculoCreateRequestDTO requestDTO = new VehiculoCreateRequestDTO(
                "1234BBB",
                "SEAT",
                "Ibiza",
                "Blanco",
                "Juan Pérez",
                "+34600112233",
                "juan.perez@example.com"
        );

        VehiculoResponseDTO responseDTO = new VehiculoResponseDTO(
                100L,
                "1234BBB",
                "SEAT",
                "Ibiza",
                "Blanco",
                "Juan Pérez",
                "+34600112233",
                "juan.perez@example.com",
                OffsetDateTime.now()
        );

        when(vehiculoService.registrarOActualizar(eq(tallerId), any(VehiculoCreateRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/talleres/{tallerId}/vehiculos", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.matricula").value("1234BBB"))
                .andExpect(jsonPath("$.nombreContacto").value("Juan Pérez"));
    }

    @Test
    @DisplayName("POST /api/v1/talleres/{id}/vehiculos - Retorna 400 Bad Request si la matrícula infringe RN-04")
    void debeFallarValidacionSiMatriculaEsInvalida() throws Exception {
        Long tallerId = 1L;
        // Matrícula no válida: contiene vocales (infracción de RN-04 para matrículas post-2000)
        VehiculoCreateRequestDTO requestDTOInvalido = new VehiculoCreateRequestDTO(
                "1234AAA",
                "SEAT",
                "Ibiza",
                "Blanco",
                "Juan Pérez",
                "+34600112233",
                "juan.perez@example.com"
        );

        mockMvc.perform(post("/api/v1/talleres/{tallerId}/vehiculos", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTOInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{id}/vehiculos - Retorna 200 OK con la lista de la flota")
    void debeListarFlotaDelTaller() throws Exception {
        Long tallerId = 1L;
        VehiculoResponseDTO vehiculo1 = new VehiculoResponseDTO(
                100L, "1234BBB", "SEAT", "Ibiza", "Blanco",
                "Juan Pérez", "+34600112233", "juan@example.com", OffsetDateTime.now()
        );

        when(vehiculoService.listarFlotaTaller(tallerId)).thenReturn(List.of(vehiculo1));

        mockMvc.perform(get("/api/v1/talleres/{tallerId}/vehiculos", tallerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matricula").value("1234BBB"));
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{id}/vehiculos/recuperar - Retorna 200 OK para conductor sin credenciales (RF-03c)")
    void debeRecuperarAccesoConductor() throws Exception {
        Long tallerId = 1L;
        String matricula = "1234BBB";
        String telefono = "+34600112233";

        VehiculoResponseDTO responseDTO = new VehiculoResponseDTO(
                100L, matricula, "SEAT", "Ibiza", "Blanco",
                "Juan Pérez", telefono, "juan@example.com", OffsetDateTime.now()
        );

        when(vehiculoService.buscarPorMatriculaYTelefonoEnTaller(tallerId, matricula, telefono))
                .thenReturn(Optional.of(responseDTO));

        mockMvc.perform(get("/api/v1/talleres/{tallerId}/vehiculos/recuperar", tallerId)
                        .param("matricula", matricula)
                        .param("telefono", telefono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value(matricula))
                .andExpect(jsonPath("$.telefonoContacto").value(telefono));
    }
}