package com.saastalleres.backend.controller;

import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoResponseDTO;
import com.saastalleres.backend.exception.GlobalExceptionHandler;
import com.saastalleres.backend.exception.ResourceNotFoundException;
import com.saastalleres.backend.service.OrdenTrabajoService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de pruebas de capa web (MockMvc) para OrdenTrabajoPublicaController.
 * Valida el enrutamiento HTTP, la deserialización de UUID y los contratos JSON para clientes externos.
 */
@ExtendWith(MockitoExtension.class)
class OrdenTrabajoPublicaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrdenTrabajoService ordenTrabajoService;

    @InjectMocks
    private OrdenTrabajoPublicaController ordenTrabajoPublicaController;

    @BeforeEach
    void setUp() {
        // Configuramos MockMvc con el controlador y el manejador global de excepciones
        mockMvc = MockMvcBuilders.standaloneSetup(ordenTrabajoPublicaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/public/ordenes/{tokenUuid} - Debe retornar 200 OK con el DTO si el token existe")
    void debeRetornar200OkCuandoTokenExiste() throws Exception {
        // Arrange
        UUID tokenUuid = UUID.randomUUID();
        OffsetDateTime ahora = OffsetDateTime.now();

        OrdenTrabajoResponseDTO responseDTO = new OrdenTrabajoResponseDTO(
                100L,
                tokenUuid,
                1L,
                10L,
                "1234ABC",
                "Seat",
                "Ibiza",
                null,
                ahora,
                null,
                EstadoOrdenTrabajo.EN_REPARACION,
                "En reparación",
                "Sustitución de pastillas delanteras",
                120000,
                new BigDecimal("150.00"),
                ahora
        );

        when(ordenTrabajoService.obtenerPorTokenUuid(tokenUuid)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/public/ordenes/{tokenUuid}", tokenUuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.tokenUuid").value(tokenUuid.toString()))
                .andExpect(jsonPath("$.matricula").value("1234ABC"))
                .andExpect(jsonPath("$.marca").value("Seat"))
                .andExpect(jsonPath("$.modelo").value("Ibiza"))
                .andExpect(jsonPath("$.estado").value("EN_REPARACION"))
                .andExpect(jsonPath("$.estadoDescripcion").value("En reparación"))
                .andExpect(jsonPath("$.diagnostico").value("Sustitución de pastillas delanteras"))
                .andExpect(jsonPath("$.kilometrajeEntrada").value(120000))
                .andExpect(jsonPath("$.costeBase").value(150.00));

        verify(ordenTrabajoService).obtenerPorTokenUuid(tokenUuid);
    }

    @Test
    @DisplayName("GET /api/v1/public/ordenes/{tokenUuid} - Debe retornar 404 Not Found si el token no existe")
    void debeRetornar404NotFoundCuandoTokenNoExiste() throws Exception {
        // Arrange
        UUID tokenInexistente = UUID.randomUUID();
        when(ordenTrabajoService.obtenerPorTokenUuid(tokenInexistente))
                .thenThrow(new ResourceNotFoundException("Orden no encontrada para el token indicado"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/public/ordenes/{tokenUuid}", tokenInexistente)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(ordenTrabajoService).obtenerPorTokenUuid(tokenInexistente);
    }

    @Test
    @DisplayName("GET /api/v1/public/ordenes/{tokenUuid} - Debe retornar 400 Bad Request si el parámetro no es un UUID válido")
    void debeRetornar400BadRequestCuandoTokenEsInvalido() throws Exception {
        // Act & Assert
        // Enviamos una cadena que no cumple con el formato UUID (Spring corta antes de llamar al servicio)
        mockMvc.perform(get("/api/v1/public/ordenes/{tokenUuid}", "token-invalido-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}