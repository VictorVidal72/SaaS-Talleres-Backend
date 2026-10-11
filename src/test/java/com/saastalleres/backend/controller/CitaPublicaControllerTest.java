package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.domain.enums.TipoServicio;
import com.saastalleres.backend.dto.cita.CitaPublicaCreateRequestDTO;
import com.saastalleres.backend.dto.cita.CitaResponseDTO;
import com.saastalleres.backend.dto.cita.DisponibilidadDiaDTO;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
class CitaPublicaControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CitaService citaService;

    @InjectMocks
    private CitaPublicaController citaPublicaController;

    private final Long tallerId = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(citaPublicaController).build();
    }

    @Test
    @DisplayName("GET /api/v1/public/talleres/{id}/disponibilidad - Retorna 200 y slots disponibles")
    void obtenerDisponibilidad_Exitoso() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        DisponibilidadDiaDTO disponibilidad = new DisponibilidadDiaDTO(
                fecha,
                List.of(LocalTime.of(8, 0), LocalTime.of(8, 45))
        );

        when(citaService.obtenerDisponibilidadPublica(eq(tallerId), eq(fecha), eq(TipoServicio.ESTANDAR)))
                .thenReturn(disponibilidad);

        mockMvc.perform(get("/api/v1/public/talleres/{tallerId}/disponibilidad", tallerId)
                        .param("fecha", "2026-10-15")
                        .param("tipoServicio", "ESTANDAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha", is("2026-10-15")))
                .andExpect(jsonPath("$.horasDisponibles[0]", is("08:00:00")))
                .andExpect(jsonPath("$.horasDisponibles[1]", is("08:45:00")));
    }

    @Test
    @DisplayName("POST /api/v1/public/talleres/{id}/citas - Retorna 201 Created al solicitar cita")
    void crearCitaPublica_Exitoso() throws Exception {
        String jsonPayload = """
                {
                    "matricula": "1234BBB",
                    "nombreCliente": "Juan Perez",
                    "telefonoCliente": "600111222",
                    "fechaInicio": "2026-10-15T08:00:00+02:00",
                    "tipoServicio": "ESTANDAR",
                    "motivo": "Revision periodica"
                }
                """;

        CitaResponseDTO response = mock(CitaResponseDTO.class);
        when(response.estado()).thenReturn(EstadoCita.PENDIENTE_CONFIRMACION);
        when(citaService.crearCitaPublica(eq(tallerId), any(CitaPublicaCreateRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/public/talleres/{tallerId}/citas", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", is("PENDIENTE_CONFIRMACION")));

        verify(citaService).crearCitaPublica(eq(tallerId), any(CitaPublicaCreateRequestDTO.class));
    }

    @Test
    @DisplayName("GET /api/v1/public/citas/{tokenUuid} - Retorna 200 con la cita consultada")
    void obtenerCitaPorToken_Exitoso() throws Exception {
        UUID tokenUuid = UUID.randomUUID();
        CitaResponseDTO response = mock(CitaResponseDTO.class);
        when(response.tokenUuid()).thenReturn(tokenUuid);
        when(response.estado()).thenReturn(EstadoCita.PENDIENTE_CONFIRMACION);

        when(citaService.obtenerCitaPorToken(tokenUuid)).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/citas/{tokenUuid}", tokenUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenUuid", is(tokenUuid.toString())))
                .andExpect(jsonPath("$.estado", is("PENDIENTE_CONFIRMACION")));
    }

    @Test
    @DisplayName("PATCH /api/v1/public/citas/{tokenUuid}/cancelar - Retorna 200 tras cancelacion publica")
    void cancelarCitaPorToken_Exitoso() throws Exception {
        UUID tokenUuid = UUID.randomUUID();
        CitaResponseDTO response = mock(CitaResponseDTO.class);
        when(response.estado()).thenReturn(EstadoCita.CANCELADA);

        when(citaService.cancelarCitaPorToken(eq(tokenUuid), eq("No puedo asistir"))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/public/citas/{tokenUuid}/cancelar", tokenUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivo", "No puedo asistir"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADA")));

        verify(citaService).cancelarCitaPorToken(tokenUuid, "No puedo asistir");
    }
}