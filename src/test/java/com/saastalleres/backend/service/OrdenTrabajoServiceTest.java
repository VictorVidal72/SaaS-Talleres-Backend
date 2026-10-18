package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.Cita;
import com.saastalleres.backend.domain.entity.OrdenTrabajo;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoCreateRequestDTO;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoEstadoUpdateRequestDTO;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoMapper;
import com.saastalleres.backend.dto.ordentrabajo.OrdenTrabajoResponseDTO;
import com.saastalleres.backend.exception.BusinessRuleException;
import com.saastalleres.backend.exception.ResourceNotFoundException;
import com.saastalleres.backend.repository.CitaRepository;
import com.saastalleres.backend.repository.OrdenTrabajoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import com.saastalleres.backend.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Suite de pruebas unitarias para OrdenTrabajoService.
 * Valida la lógica de negocio, transiciones de estado, aislamiento multi-tenant y manejo defensivo.
 */
@ExtendWith(MockitoExtension.class)
class OrdenTrabajoServiceTest {

    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Mock
    private TallerRepository tallerRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private OrdenTrabajoMapper ordenTrabajoMapper;

    @InjectMocks
    private OrdenTrabajoService ordenTrabajoService;

    private Taller taller;
    private Vehiculo vehiculo;
    private OrdenTrabajo orden;
    private OrdenTrabajoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        taller = Taller.builder()
                .nombreComercial("Taller Central")
                .cif("B12345678")
                .slug("taller-central")
                .build();
        taller.setId(1L);

        vehiculo = Vehiculo.builder()
                .matricula("1234ABC")
                .marca("Seat")
                .modelo("Ibiza")
                .nombreContacto("Juan Pérez")
                .telefonoContacto("600123456")
                .build();
        vehiculo.setId(10L);

        orden = OrdenTrabajo.builder()
                .taller(taller)
                .vehiculo(vehiculo)
                .kilometrajeEntrada(120000)
                .diagnostico("Revisión de frenos")
                .costeBase(new BigDecimal("150.00"))
                .estado(EstadoOrdenTrabajo.RECEPCIONADO)
                .fechaLlegada(OffsetDateTime.now())
                .build();
        orden.setId(100L);

        responseDTO = new OrdenTrabajoResponseDTO(
                100L,
                UUID.randomUUID(),
                1L,
                10L,
                "1234ABC",
                "Seat",
                "Ibiza",
                null,
                OffsetDateTime.now(),
                null,
                EstadoOrdenTrabajo.RECEPCIONADO,
                "Recepcionado",
                "Revisión de frenos",
                120000,
                new BigDecimal("150.00"),
                OffsetDateTime.now()
        );
    }

    // =========================================================================
    // 1. TESTS DE RECEPCIÓN Y APERTURA (CHECK-IN)
    // =========================================================================
    @Nested
    @DisplayName("Casos de Uso: Recepción física y apertura de orden (Check-in)")
    class CheckInTests {

        @Test
        @DisplayName("Debe crear una orden de trabajo exitosamente sin cita previa (urgencia/directa)")
        void debeCrearOrdenSinCitaPrevia() {
            // Arrange
            OrdenTrabajoCreateRequestDTO dto = new OrdenTrabajoCreateRequestDTO(
                    10L, null, 120000, "Revisión de frenos", new BigDecimal("150.00")
            );

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(orden);
            when(ordenTrabajoMapper.toResponseDTO(orden)).thenReturn(responseDTO);

            // Act
            OrdenTrabajoResponseDTO resultado = ordenTrabajoService.crearOrdenTrabajo(1L, dto);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(100L);
            assertThat(resultado.estado()).isEqualTo(EstadoOrdenTrabajo.RECEPCIONADO);

            verify(tallerRepository).findById(1L);
            verify(vehiculoRepository).findById(10L);
            verify(ordenTrabajoRepository).save(any(OrdenTrabajo.class));
            verifyNoInteractions(citaRepository);
        }

        @Test
        @DisplayName("Debe crear orden y transicionar la cita asociada a FINALIZADA")
        void debeCrearOrdenYFinalizarCitaAsociada() {
            // Arrange
            Long citaId = 50L;
            OrdenTrabajoCreateRequestDTO dto = new OrdenTrabajoCreateRequestDTO(
                    10L, citaId, 120000, "Revisión programada", new BigDecimal("100.00")
            );

            Cita cita = Cita.builder()
                    .taller(taller)
                    .vehiculo(vehiculo)
                    .estado(EstadoCita.CONFIRMADA)
                    .build();
            cita.setId(citaId);

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(citaRepository.findByIdAndTallerId(citaId, 1L)).thenReturn(Optional.of(cita));
            when(ordenTrabajoRepository.findByCitaId(citaId)).thenReturn(Optional.empty());
            when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(orden);
            when(ordenTrabajoMapper.toResponseDTO(orden)).thenReturn(responseDTO);

            // Act
            OrdenTrabajoResponseDTO resultado = ordenTrabajoService.crearOrdenTrabajo(1L, dto);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(cita.getEstado()).isEqualTo(EstadoCita.FINALIZADA);

            verify(citaRepository).save(cita);
            verify(ordenTrabajoRepository).save(any(OrdenTrabajo.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el vehículo no coincide con el asignado a la cita")
        void debeLanzarExcepcionSiVehiculoNoCoincideConCita() {
            // Arrange
            Long citaId = 50L;
            Vehiculo otroVehiculo = Vehiculo.builder().matricula("9999ZZZ").build();
            otroVehiculo.setId(99L);

            Cita citaConOtroCoche = Cita.builder()
                    .taller(taller)
                    .vehiculo(otroVehiculo)
                    .estado(EstadoCita.CONFIRMADA)
                    .build();
            citaConOtroCoche.setId(citaId);

            OrdenTrabajoCreateRequestDTO dto = new OrdenTrabajoCreateRequestDTO(
                    10L, citaId, 120000, "Intento inconsistente", BigDecimal.ZERO
            );

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(citaRepository.findByIdAndTallerId(citaId, 1L)).thenReturn(Optional.of(citaConOtroCoche));

            // Act & Assert
            assertThatThrownBy(() -> ordenTrabajoService.crearOrdenTrabajo(1L, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no coincide con el vehículo asignado");

            verify(ordenTrabajoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción si la cita ya tiene una orden de trabajo activa")
        void debeLanzarExcepcionSiCitaYaTieneOrden() {
            // Arrange
            Long citaId = 50L;
            Cita cita = Cita.builder().taller(taller).vehiculo(vehiculo).build();
            cita.setId(citaId);

            OrdenTrabajoCreateRequestDTO dto = new OrdenTrabajoCreateRequestDTO(
                    10L, citaId, 120000, "Duplicado", BigDecimal.ZERO
            );

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(citaRepository.findByIdAndTallerId(citaId, 1L)).thenReturn(Optional.of(cita));
            when(ordenTrabajoRepository.findByCitaId(citaId)).thenReturn(Optional.of(orden));

            // Act & Assert
            assertThatThrownBy(() -> ordenTrabajoService.crearOrdenTrabajo(1L, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Ya existe una orden de trabajo activa");

            verify(ordenTrabajoRepository, never()).save(any());
        }
    }

    // =========================================================================
    // 2. TESTS DEL TABLERO KANBAN Y TRANSICIÓN DE ESTADOS
    // =========================================================================
    @Nested
    @DisplayName("Casos de Uso: Ciclo operativo y transición Kanban")
    class TransicionEstadoTests {

        @Test
        @DisplayName("Debe actualizar el estado a EN_REPARACION y modificar el diagnóstico")
        void debeActualizarEstadoYDiagnostico() {
            // Arrange
            OrdenTrabajoEstadoUpdateRequestDTO dto = new OrdenTrabajoEstadoUpdateRequestDTO(
                    EstadoOrdenTrabajo.EN_REPARACION, "Desmontando pastillas delanteras"
            );

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(ordenTrabajoRepository.findByIdAndTallerId(100L, 1L)).thenReturn(Optional.of(orden));
            when(ordenTrabajoRepository.save(orden)).thenReturn(orden);
            when(ordenTrabajoMapper.toResponseDTO(orden)).thenReturn(responseDTO);

            // Act
            OrdenTrabajoResponseDTO resultado = ordenTrabajoService.actualizarEstado(1L, 100L, dto);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(orden.getEstado()).isEqualTo(EstadoOrdenTrabajo.EN_REPARACION);
            assertThat(orden.getDiagnostico()).isEqualTo("Desmontando pastillas delanteras");
            assertThat(orden.getFechaEntrega()).isNull();
        }

        @Test
        @DisplayName("Debe registrar la fecha de entrega física al transicionar a ENTREGADO")
        void debeSellarFechaEntregaAlEntregar() {
            // Arrange
            OrdenTrabajoEstadoUpdateRequestDTO dto = new OrdenTrabajoEstadoUpdateRequestDTO(
                    EstadoOrdenTrabajo.ENTREGADO, "Vehículo devuelto al conductor"
            );

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(ordenTrabajoRepository.findByIdAndTallerId(100L, 1L)).thenReturn(Optional.of(orden));
            when(ordenTrabajoRepository.save(orden)).thenReturn(orden);
            when(ordenTrabajoMapper.toResponseDTO(orden)).thenReturn(responseDTO);

            // Act
            ordenTrabajoService.actualizarEstado(1L, 100L, dto);

            // Assert
            assertThat(orden.getEstado()).isEqualTo(EstadoOrdenTrabajo.ENTREGADO);
            assertThat(orden.getFechaEntrega()).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar excepción si se intenta modificar una orden que ya está entregada")
        void debeLanzarExcepcionSiOrdenYaFueEntregada() {
            // Arrange
            orden.setEstado(EstadoOrdenTrabajo.ENTREGADO);
            OrdenTrabajoEstadoUpdateRequestDTO dto = new OrdenTrabajoEstadoUpdateRequestDTO(
                    EstadoOrdenTrabajo.EN_REPARACION, "Intento de reactivación"
            );

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(ordenTrabajoRepository.findByIdAndTallerId(100L, 1L)).thenReturn(Optional.of(orden));

            // Act & Assert
            assertThatThrownBy(() -> ordenTrabajoService.actualizarEstado(1L, 100L, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ya ha sido entregada");

            verify(ordenTrabajoRepository, never()).save(any());
        }
    }

    // =========================================================================
    // 3. TESTS DE CONSULTA Y SEGUIMIENTO PÚBLICO
    // =========================================================================
    @Nested
    @DisplayName("Casos de Uso: Consultas internas y visor público por Token UUID")
    class ConsultaTests {

        @Test
        @DisplayName("Debe obtener una orden por su token UUID público")
        void debeObtenerPorTokenUuid() {
            // Arrange
            UUID token = UUID.randomUUID();
            when(ordenTrabajoRepository.findByTokenUuid(token)).thenReturn(Optional.of(orden));
            when(ordenTrabajoMapper.toResponseDTO(orden)).thenReturn(responseDTO);

            // Act
            OrdenTrabajoResponseDTO resultado = ordenTrabajoService.obtenerPorTokenUuid(token);

            // Assert
            assertThat(resultado).isNotNull();
            verify(ordenTrabajoRepository).findByTokenUuid(token);
        }

        @Test
        @DisplayName("Debe lanzar 404 si el token UUID público no existe")
        void debeLanzar404SiTokenNoExiste() {
            // Arrange
            UUID tokenInexistente = UUID.randomUUID();
            when(ordenTrabajoRepository.findByTokenUuid(tokenInexistente)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ordenTrabajoService.obtenerPorTokenUuid(tokenInexistente))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("token de seguimiento indicado");
        }

        @Test
        @DisplayName("Debe listar el historial de un vehículo en un taller concreto")
        void debeListarHistorialVehiculo() {
            // Arrange
            when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(ordenTrabajoRepository.findByTallerIdAndVehiculoId(1L, 10L)).thenReturn(List.of(orden));
            when(ordenTrabajoMapper.toResponseDTO(orden)).thenReturn(responseDTO);

            // Act
            List<OrdenTrabajoResponseDTO> historial = ordenTrabajoService.listarHistorialVehiculo(1L, 10L);

            // Assert
            assertThat(historial).hasSize(1);
            verify(ordenTrabajoRepository).findByTallerIdAndVehiculoId(1L, 10L);
        }
    }
}