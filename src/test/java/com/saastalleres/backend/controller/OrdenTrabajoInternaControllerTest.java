package com.saastalleres.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoCreateRequestDTO;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoEstadoUpdateRequestDTO;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoResponseDTO;
import com.saastalleres.backend.exception.GlobalExceptionHandler;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de pruebas de capa web (MockMvc) para OrdenTrabajoInternaController.
 * Valida el enrutamiento HTTP, la serialización/deserialización JSON,
 * las validaciones declarativas @Valid y los contratos de respuesta del panel del taller.
 */
@ExtendWith(MockitoExtension.class)
class OrdenTrabajoInternaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OrdenTrabajoService ordenTrabajoService;

    @InjectMocks
    private OrdenTrabajoInternaController ordenTrabajoInternaController;

    private Long tallerId;
    private OffsetDateTime ahora;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ordenTrabajoInternaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        tallerId = 1L;
        ahora = OffsetDateTime.now();
    }

    private OrdenTrabajoResponseDTO buildMockResponse(Long id, Long vehiculoId, EstadoOrdenTrabajo estado) {
        return new OrdenTrabajoResponseDTO(
                id,
                UUID.randomUUID(),
                tallerId,
                vehiculoId,
                "1234ABC",
                "Seat",
                "Ibiza",
                null,
                ahora,
                (estado == EstadoOrdenTrabajo.ENTREGADO) ? ahora : null,
                estado,
                estado.getDescripcion(),
                "Revisión y cambio de filtros",
                120000,
                new BigDecimal("150.00"),
                ahora
        );
    }

    @Test
    @DisplayName("POST /api/v1/talleres/{tallerId}/ordenes - Retorna 201 Created cuando la petición es válida")
    void debeCrearOrdenTrabajoExitosamente() throws Exception {
        // Arrange
        OrdenTrabajoCreateRequestDTO requestDTO = new OrdenTrabajoCreateRequestDTO(
                10L,
                null,
                120000,
                "Revisión y cambio de filtros",
                new BigDecimal("150.00")
        );

        OrdenTrabajoResponseDTO responseDTO = buildMockResponse(100L, 10L, EstadoOrdenTrabajo.RECEPCIONADO);

        when(ordenTrabajoService.crearOrdenTrabajo(eq(tallerId), any(OrdenTrabajoCreateRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/talleres/{tallerId}/ordenes", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.tallerId").value(tallerId))
                .andExpect(jsonPath("$.vehiculoId").value(10L))
                .andExpect(jsonPath("$.matricula").value("1234ABC"))
                .andExpect(jsonPath("$.estado").value("RECEPCIONADO"))
                .andExpect(jsonPath("$.kilometrajeEntrada").value(120000));

        verify(ordenTrabajoService).crearOrdenTrabajo(eq(tallerId), any(OrdenTrabajoCreateRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/talleres/{tallerId}/ordenes - Retorna 400 Bad Request cuando faltan datos obligatorios")
    void debeRetornar400CuandoCrearOrdenTieneDatosInvalidos() throws Exception {
        // Arrange: DTO con vehiculoId nulo y kilometraje negativo (viola @NotNull y @PositiveOrZero)
        OrdenTrabajoCreateRequestDTO requestInvalido = new OrdenTrabajoCreateRequestDTO(
                null,
                null,
                -50,
                "Diagnóstico inválido",
                new BigDecimal("-10.00")
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/talleres/{tallerId}/ordenes", tallerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/talleres/{tallerId}/ordenes/{ordenId}/estado - Retorna 200 OK al cambiar de estado")
    void debeActualizarEstadoKanbanExitosamente() throws Exception {
        // Arrange
        Long ordenId = 100L;
        OrdenTrabajoEstadoUpdateRequestDTO updateDTO = new OrdenTrabajoEstadoUpdateRequestDTO(
                EstadoOrdenTrabajo.EN_REPARACION,
                "Iniciando desmontaje de frenos"
        );

        OrdenTrabajoResponseDTO responseDTO = buildMockResponse(ordenId, 10L, EstadoOrdenTrabajo.EN_REPARACION);

        when(ordenTrabajoService.actualizarEstado(eq(tallerId), eq(ordenId), any(OrdenTrabajoEstadoUpdateRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/talleres/{tallerId}/ordenes/{ordenId}/estado", tallerId, ordenId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ordenId))
                .andExpect(jsonPath("$.estado").value("EN_REPARACION"))
                .andExpect(jsonPath("$.estadoDescripcion").value(EstadoOrdenTrabajo.EN_REPARACION.getDescripcion()));

        verify(ordenTrabajoService).actualizarEstado(eq(tallerId), eq(ordenId), any(OrdenTrabajoEstadoUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{tallerId}/ordenes/{ordenId} - Retorna 200 OK con la orden solicitada")
    void debeObtenerOrdenPorIdExitosamente() throws Exception {
        // Arrange
        Long ordenId = 100L;
        OrdenTrabajoResponseDTO responseDTO = buildMockResponse(ordenId, 10L, EstadoOrdenTrabajo.RECEPCIONADO);

        when(ordenTrabajoService.obtenerPorId(tallerId, ordenId)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/talleres/{tallerId}/ordenes/{ordenId}", tallerId, ordenId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ordenId))
                .andExpect(jsonPath("$.tallerId").value(tallerId))
                .andExpect(jsonPath("$.matricula").value("1234ABC"));

        verify(ordenTrabajoService).obtenerPorId(tallerId, ordenId);
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{tallerId}/ordenes - Retorna 200 OK con listado general de órdenes")
    void debeListarTodasLasOrdenesDelTaller() throws Exception {
        // Arrange
        List<OrdenTrabajoResponseDTO> lista = List.of(
                buildMockResponse(100L, 10L, EstadoOrdenTrabajo.RECEPCIONADO),
                buildMockResponse(101L, 11L, EstadoOrdenTrabajo.EN_REPARACION)
        );

        when(ordenTrabajoService.listarPorTaller(tallerId)).thenReturn(lista);

        // Act & Assert
        mockMvc.perform(get("/api/v1/talleres/{tallerId}/ordenes", tallerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[1].id").value(101L));

        verify(ordenTrabajoService).listarPorTaller(tallerId);
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{tallerId}/ordenes?estados=... - Retorna 200 OK filtrando por estados para el Kanban")
    void debeListarOrdenesFiltradasPorEstados() throws Exception {
        // Arrange
        List<OrdenTrabajoResponseDTO> listaFiltrada = List.of(
                buildMockResponse(100L, 10L, EstadoOrdenTrabajo.RECEPCIONADO)
        );

        when(ordenTrabajoService.listarPorEstados(eq(tallerId), any())).thenReturn(listaFiltrada);

        // Act & Assert
        mockMvc.perform(get("/api/v1/talleres/{tallerId}/ordenes", tallerId)
                        .param("estados", "RECEPCIONADO,EN_REPARACION")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("RECEPCIONADO"));

        verify(ordenTrabajoService).listarPorEstados(eq(tallerId), any());
    }

    @Test
    @DisplayName("GET /api/v1/talleres/{tallerId}/ordenes/vehiculos/{vehiculoId} - Retorna 200 OK con historial del coche")
    void debeListarHistorialDeVehiculo() throws Exception {
        // Arrange
        Long vehiculoId = 10L;
        List<OrdenTrabajoResponseDTO> historial = List.of(
                buildMockResponse(100L, vehiculoId, EstadoOrdenTrabajo.ENTREGADO),
                buildMockResponse(102L, vehiculoId, EstadoOrdenTrabajo.RECEPCIONADO)
        );

        when(ordenTrabajoService.listarHistorialVehiculo(tallerId, vehiculoId)).thenReturn(historial);

        // Act & Assert
        mockMvc.perform(get("/api/v1/talleres/{tallerId}/ordenes/vehiculos/{vehiculoId}", tallerId, vehiculoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].vehiculoId").value(vehiculoId))
                .andExpect(jsonPath("$[1].vehiculoId").value(vehiculoId));

        verify(ordenTrabajoService).listarHistorialVehiculo(tallerId, vehiculoId);
    }
}