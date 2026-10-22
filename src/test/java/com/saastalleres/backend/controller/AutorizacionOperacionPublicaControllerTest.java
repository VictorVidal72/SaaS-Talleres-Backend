package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.domain.enums.EstadoAutorizacion;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResolverRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResponseDTO;
import com.saastalleres.backend.exception.GlobalExceptionHandler;
import com.saastalleres.backend.exception.ResourceNotFoundException;
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
@DisplayName("Tests Web / MockMvc - AutorizacionOperacionPublicaController")
class AutorizacionOperacionPublicaControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private AutorizacionOperacionService autorizacionService;

    @InjectMocks
    private AutorizacionOperacionPublicaController controller;

    private UUID tokenUuid;
    private AutorizacionResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        tokenUuid = UUID.randomUUID();
        responseDTO = new AutorizacionResponseDTO(
                100L,
                tokenUuid,
                1L,
                50L,
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
    @DisplayName("GET /api/v1/public/autorizaciones/{tokenUuid} - 200 OK con datos públicos")
    void consultarPorToken_Exito() throws Exception {
        when(autorizacionService.consultarPorToken(tokenUuid)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/public/autorizaciones/{tokenUuid}", tokenUuid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.tokenUuid").value(tokenUuid.toString()))
                .andExpect(jsonPath("$.matriculaVehiculo").value("1234BBB"))
                .andExpect(jsonPath("$.descripcion").value("Discos de freno desgastados"))
                .andExpect(jsonPath("$.costeExtra").value(150.00))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(autorizacionService).consultarPorToken(tokenUuid);
    }

    @Test
    @DisplayName("GET /api/v1/public/autorizaciones/{tokenUuid} - 404 Not Found cuando el token no existe")
    void consultarPorToken_NoExiste_404() throws Exception {
        UUID tokenInexistente = UUID.randomUUID();
        when(autorizacionService.consultarPorToken(tokenInexistente))
                .thenThrow(new ResourceNotFoundException("Autorización no encontrada"));

        mockMvc.perform(get("/api/v1/public/autorizaciones/{tokenUuid}", tokenInexistente)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/public/autorizaciones/{tokenUuid} - 400 Bad Request ante formato UUID inválido")
    void consultarPorToken_FormatoInvalido_400() throws Exception {
        mockMvc.perform(get("/api/v1/public/autorizaciones/{tokenUuid}", "token-no-valido-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(autorizacionService, never()).consultarPorToken(any());
    }

    @Test
    @DisplayName("POST /api/v1/public/autorizaciones/{tokenUuid}/resolver - 200 OK y captura auditoría IP/User-Agent (RN-07)")
    void resolverAutorizacion_Exito() throws Exception {
        AutorizacionResolverRequestDTO requestDTO = new AutorizacionResolverRequestDTO(true, null);

        AutorizacionResponseDTO resueltoDTO = new AutorizacionResponseDTO(
                100L,
                tokenUuid,
                1L,
                50L,
                "1234BBB",
                "Discos de freno desgastados",
                new BigDecimal("150.00"),
                EstadoAutorizacion.AUTORIZADO,
                EstadoAutorizacion.AUTORIZADO.getDescripcion(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(autorizacionService.resolverAutorizacion(
                eq(tokenUuid),
                eq(requestDTO),
                eq("83.54.120.45"),
                eq("Mozilla/5.0 (iPhone)")
        )).thenReturn(resueltoDTO);

        mockMvc.perform(post("/api/v1/public/autorizaciones/{tokenUuid}/resolver", tokenUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .header("User-Agent", "Mozilla/5.0 (iPhone)")
                        .with(request -> {
                            request.setRemoteAddr("83.54.120.45");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AUTORIZADO"));

        verify(autorizacionService).resolverAutorizacion(
                eq(tokenUuid), eq(requestDTO), eq("83.54.120.45"), eq("Mozilla/5.0 (iPhone)"));
    }

    @Test
    @DisplayName("POST /api/v1/public/autorizaciones/{tokenUuid}/resolver - 400 Bad Request si 'aprobado' es null")
    void resolverAutorizacion_ValidacionPerimetral_400() throws Exception {
        // Objeto sin valor en 'aprobado' (null)
        AutorizacionResolverRequestDTO requestInvalido = new AutorizacionResolverRequestDTO(null, "Rechazo sin marcar");

        mockMvc.perform(post("/api/v1/public/autorizaciones/{tokenUuid}/resolver", tokenUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());

        verify(autorizacionService, never()).resolverAutorizacion(any(), any(), any(), any());
    }
}