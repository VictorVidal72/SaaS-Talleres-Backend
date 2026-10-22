package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.domain.enums.EstadoAutorizacion;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionCreateRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResponseDTO;
import com.saastalleres.backend.exception.GlobalExceptionHandler;
import com.saastalleres.backend.service.AutorizacionOperacionService;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Web / MockMvc - AutorizacionOperacionInternaController")
class AutorizacionOperacionInternaControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private AutorizacionOperacionService autorizacionService;

    @InjectMocks
    private AutorizacionOperacionInternaController controller;

    private final Long tallerId = 1L;
    private final Long ordenTrabajoId = 50L;
    private final Long autorizacionId = 100L;
    private UUID tokenUuid;
    private AutorizacionResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        tokenUuid = UUID.randomUUID();
        responseDTO = new AutorizacionResponseDTO(
                autorizacionId,
                tokenUuid,
                tallerId,
                ordenTrabajoId,
                "1234BBB",
                "Discos de freno desgastados",
                new BigDecimal("150.00"),
                EstadoAutorizacion.PENDIENTE,
                EstadoAutorizacion.PENDIENTE.getDescripcion(),
                null,
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/talleres/{tallerId}/autorizaciones - 201 Created al solicitar autorización")
    void solicitarAutorizacion_Exito() throws Exception {
        AutorizacionCreateRequestDTO requestDTO = new AutorizacionCreateRequestDTO(
                ordenTrabajoId, "Discos de freno desgastados", new BigDecimal("150.00"));

        when(autorizacionService.solicitarAutorizacion(eq(tallerId), any(AutorizacionCreateRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/talleres/{tallerId}/autorizaciones", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(autorizacionId))
                .andExpect(jsonPath("$.tallerId").value(tallerId))
                .andExpect(jsonPath("$.ordenTrabajoId").value(ordenTrabajoId))
                .andExpect(jsonPath("$.costeExtra").value(150.00))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(autorizacionService).solicitarAutorizacion(eq(tallerId), any(AutorizacionCreateRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/talleres/{tallerId}/autorizaciones - 400 Bad Request ante validación fallida (@Valid)")
    void solicitarAutorizacion_ValidacionFallida_400() throws Exception {
        // DTO con datos inválidos: descripción en blanco y coste nulo
        AutorizacionCreateRequestDTO requestInvalido = new AutorizacionCreateRequestDTO(
                null, "   ", null);

        mockMvc.perform(post("/api/v1/talleres/{tallerId}/autorizaciones", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());

        verify(autorizacionService, never()).solicitarAutorizacion(any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{tallerId}/autorizaciones/ordenes/{ordenTrabajoId} - 200 OK y listado de autorizaciones")
    void listarPorOrden_Exito() throws Exception {
        when(autorizacionService.listarPorOrden(tallerId, ordenTrabajoId))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/v1/talleres/{tallerId}/autorizaciones/ordenes/{ordenTrabajoId}", tallerId, ordenTrabajoId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(autorizacionId))
                .andExpect(jsonPath("$[0].ordenTrabajoId").value(ordenTrabajoId));

        verify(autorizacionService).listarPorOrden(tallerId, ordenTrabajoId);
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{tallerId}/autorizaciones/{autorizacionId} - 200 OK y detalle de autorización")
    void consultarPorIdYTaller_Exito() throws Exception {
        when(autorizacionService.consultarPorIdYTaller(tallerId, autorizacionId))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/talleres/{tallerId}/autorizaciones/{autorizacionId}", tallerId, autorizacionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(autorizacionId))
                .andExpect(jsonPath("$.tallerId").value(tallerId))
                .andExpect(jsonPath("$.descripcion").value("Discos de freno desgastados"));

        verify(autorizacionService).consultarPorIdYTaller(tallerId, autorizacionId);
    }
}