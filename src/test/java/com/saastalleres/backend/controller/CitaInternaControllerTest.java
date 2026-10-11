package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.dto.cita.CitaInternaCreateRequestDTO;
import com.saastalleres.backend.dto.cita.CitaResponseDTO;
import com.saastalleres.backend.service.CitaService;
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
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CitaInternaControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CitaService citaService;

    @InjectMocks
    private CitaInternaController citaInternaController;

    private final Long tallerId = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(citaInternaController).build();
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{tallerId}/citas - Retorna 200 OK con la lista de citas en el rango")
    void listarCitasPorRango_Exitoso() throws Exception {
        CitaResponseDTO citaMock = mock(CitaResponseDTO.class);
        when(citaMock.id()).thenReturn(100L);
        when(citaMock.estado()).thenReturn(EstadoCita.CONFIRMADA);

        when(citaService.listarCitasPorRango(eq(tallerId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(citaMock));

        mockMvc.perform(get("/api/v1/talleres/{tallerId}/citas", tallerId)
                        .param("inicio", "2026-10-15T08:00:00+02:00")
                        .param("fin", "2026-10-15T18:00:00+02:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(100)))
                .andExpect(jsonPath("$[0].estado", is("CONFIRMADA")));

        verify(citaService).listarCitasPorRango(eq(tallerId), any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("POST /api/v1/talleres/{tallerId}/citas - Retorna 201 Created al registrar cita interna")
    void crearCitaInterna_Exitoso() throws Exception {
        String jsonPayload = """
                {
                    "matricula": "5678CCC",
                    "nombreCliente": "Maria Gomez",
                    "telefonoCliente": "611222333",
                    "fechaInicio": "2026-10-15T10:00:00+02:00",
                    "fechaFin": "2026-10-15T11:00:00+02:00",
                    "tipoServicio": "DIAGNOSIS",
                    "puestoId": 2,
                    "motivo": "Cambio de embrague"
                }
                """;

        CitaResponseDTO response = mock(CitaResponseDTO.class);
        when(response.id()).thenReturn(101L);
        when(response.estado()).thenReturn(EstadoCita.CONFIRMADA);

        when(citaService.crearCitaInterna(eq(tallerId), any(CitaInternaCreateRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/talleres/{tallerId}/citas", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")));

        verify(citaService).crearCitaInterna(eq(tallerId), any(CitaInternaCreateRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/talleres/{tallerId}/citas/{citaId}/confirmar - Retorna 200 OK con puesto asignado")
    void confirmarCitaPublica_Exitoso() throws Exception {
        Long citaId = 10L;
        Long puestoId = 3L;

        CitaResponseDTO response = mock(CitaResponseDTO.class);
        when(response.id()).thenReturn(citaId);
        when(response.estado()).thenReturn(EstadoCita.CONFIRMADA);

        when(citaService.confirmarCitaPublica(tallerId, citaId, puestoId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/talleres/{tallerId}/citas/{citaId}/confirmar", tallerId, citaId)
                        .param("puestoId", puestoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")));

        verify(citaService).confirmarCitaPublica(tallerId, citaId, puestoId);
    }

    @Test
    @DisplayName("PATCH /api/v1/talleres/{tallerId}/citas/{citaId}/cancelar - Retorna 200 OK tras cancelacion interna")
    void cancelarCitaInterna_Exitoso() throws Exception {
        Long citaId = 10L;
        CitaResponseDTO response = mock(CitaResponseDTO.class);
        when(response.id()).thenReturn(citaId);
        when(response.estado()).thenReturn(EstadoCita.CANCELADA);

        when(citaService.cancelarCita(tallerId, citaId, "Falta recambio")).thenReturn(response);

        mockMvc.perform(patch("/api/v1/talleres/{tallerId}/citas/{citaId}/cancelar", tallerId, citaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivo", "Falta recambio"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.estado", is("CANCELADA")));

        verify(citaService).cancelarCita(tallerId, citaId, "Falta recambio");
    }
}