package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.dto.puesto.PuestoCreateRequestDTO;
import com.saastalleres.backend.dto.puesto.PuestoResponseDTO;
import com.saastalleres.backend.service.PuestoService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PuestoControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PuestoService puestoService;

    @InjectMocks
    private PuestoController puestoController;

    private final Long tallerId = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(puestoController).build();
    }

    @Test
    @DisplayName("POST /talleres/{tallerId}/puestos - Retorna 201 Created con cabecera Location")
    void debeCrearPuestoExitosamente() throws Exception {
        PuestoCreateRequestDTO requestDTO = new PuestoCreateRequestDTO("Elevador 1");
        PuestoResponseDTO responseDTO = new PuestoResponseDTO(
                10L,
                tallerId,
                "Elevador 1",
                true,
                OffsetDateTime.now()
        );

        when(puestoService.crearPuesto(eq(tallerId), any(PuestoCreateRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/talleres/{tallerId}/puestos", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.nombre", is("Elevador 1")))
                .andExpect(jsonPath("$.activo", is(true)))
                .andExpect(jsonPath("$.tallerId", is(1)));

        verify(puestoService).crearPuesto(eq(tallerId), any(PuestoCreateRequestDTO.class));
    }

    @Test
    @DisplayName("POST /talleres/{tallerId}/puestos - Retorna 400 Bad Request si el nombre está en blanco")
    void debeFallarAlCrearPuestoConNombreVacio() throws Exception {
        PuestoCreateRequestDTO requestInvalido = new PuestoCreateRequestDTO("   ");

        mockMvc.perform(post("/api/v1/talleres/{tallerId}/puestos", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());

        verify(puestoService, never()).crearPuesto(any(), any());
    }

    @Test
    @DisplayName("GET /talleres/{tallerId}/puestos - Retorna 200 OK con la lista completa de puestos")
    void debeListarTodosLosPuestos() throws Exception {
        List<PuestoResponseDTO> puestos = List.of(
                new PuestoResponseDTO(10L, tallerId, "Elevador 1", true, OffsetDateTime.now()),
                new PuestoResponseDTO(20L, tallerId, "Box Rápido", false, OffsetDateTime.now())
        );

        when(puestoService.listarTodosPorTaller(tallerId)).thenReturn(puestos);

        mockMvc.perform(get("/api/v1/talleres/{tallerId}/puestos", tallerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre", is("Elevador 1")))
                .andExpect(jsonPath("$[1].nombre", is("Box Rápido")));

        verify(puestoService).listarTodosPorTaller(tallerId);
    }

    @Test
    @DisplayName("GET /talleres/{tallerId}/puestos/activos - Retorna 200 OK solo con puestos operativos")
    void debeListarSoloPuestosActivos() throws Exception {
        List<PuestoResponseDTO> activos = List.of(
                new PuestoResponseDTO(10L, tallerId, "Elevador 1", true, OffsetDateTime.now())
        );

        when(puestoService.listarActivosPorTaller(tallerId)).thenReturn(activos);

        mockMvc.perform(get("/api/v1/talleres/{tallerId}/puestos/activos", tallerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].activo", is(true)));

        verify(puestoService).listarActivosPorTaller(tallerId);
    }

    @Test
    @DisplayName("PATCH /talleres/{tallerId}/puestos/{puestoId}/estado - Retorna 200 OK con estado actualizado")
    void debeCambiarEstadoOperativo() throws Exception {
        Long puestoId = 10L;
        PuestoResponseDTO actualizado = new PuestoResponseDTO(
                puestoId,
                tallerId,
                "Elevador 1",
                false,
                OffsetDateTime.now()
        );

        when(puestoService.cambiarEstadoOperativo(tallerId, puestoId, false))
                .thenReturn(actualizado);

        mockMvc.perform(patch("/api/v1/talleres/{tallerId}/puestos/{puestoId}/estado", tallerId, puestoId)
                        .param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.activo", is(false)));

        verify(puestoService).cambiarEstadoOperativo(tallerId, puestoId, false);
    }
}