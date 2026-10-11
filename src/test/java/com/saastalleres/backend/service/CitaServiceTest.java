package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.Cita;
import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.TallerVehiculo;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.domain.enums.TipoServicio;
import com.saastalleres.backend.dto.cita.CitaMapper;
import com.saastalleres.backend.dto.cita.CitaPublicaCreateRequestDTO;
import com.saastalleres.backend.dto.cita.CitaResponseDTO;
import com.saastalleres.backend.dto.cita.DisponibilidadDiaDTO;
import com.saastalleres.backend.exception.BusinessRuleException;
import com.saastalleres.backend.exception.ResourceNotFoundException;
import com.saastalleres.backend.repository.CitaRepository;
import com.saastalleres.backend.repository.PuestoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import com.saastalleres.backend.repository.TallerVehiculoRepository;
import com.saastalleres.backend.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private TallerRepository tallerRepository;
    @Mock
    private PuestoRepository puestoRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private TallerVehiculoRepository tallerVehiculoRepository;
    @Mock
    private CitaMapper citaMapper;

    @InjectMocks
    private CitaService citaService;

    private static final ZoneId ZONA_HORARIA = ZoneId.of("Europe/Madrid");
    private Taller tallerEjemplo;

    @BeforeEach
    void setUp() {
        tallerEjemplo = Taller.builder()
                .nombreComercial("Taller Sanlucar")
                .horaAperturaManana(LocalTime.of(8, 0))
                .horaCierreManana(LocalTime.of(10, 0))
                .horaAperturaTarde(LocalTime.of(16, 0))
                .horaCierreTarde(LocalTime.of(17, 0))
                .build();
        ReflectionTestUtils.setField(tallerEjemplo, "id", 1L);
    }

    // =========================================================================
    // 1. TESTS DE DISPONIBILIDAD PÚBLICA
    // =========================================================================
    @Nested
    @DisplayName("Cálculo de Disponibilidad Pública")
    class DisponibilidadPublicaTests {

        @Test
        @DisplayName("Debe retornar lista vacía si el taller no tiene puestos configurados")
        void obtenerDisponibilidad_SinPuestos_RetornaVacio() {
            LocalDate fecha = LocalDate.of(2026, 10, 15);
            when(tallerRepository.findById(1L)).thenReturn(Optional.of(tallerEjemplo));
            when(puestoRepository.findByTallerId(1L)).thenReturn(Collections.emptyList());

            DisponibilidadDiaDTO resultado = citaService.obtenerDisponibilidadPublica(1L, fecha, TipoServicio.ESTANDAR);

            assertThat(resultado.fecha()).isEqualTo(fecha);
            assertThat(resultado.horasDisponibles()).isEmpty();
            verify(citaRepository, never()).findByTallerIdAndFechaInicioBetweenAndEstadoIn(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Debe calcular todas las franjas cuando todos los puestos están libres")
        void obtenerDisponibilidad_PuestosLibres_RetornaFranjasCompletas() {
            LocalDate fecha = LocalDate.of(2026, 10, 15);
            Puesto puesto = Puesto.builder().taller(tallerEjemplo).build();
            ReflectionTestUtils.setField(puesto, "id", 10L);

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(tallerEjemplo));
            when(puestoRepository.findByTallerId(1L)).thenReturn(List.of(puesto));
            when(citaRepository.findByTallerIdAndFechaInicioBetweenAndEstadoIn(eq(1L), any(), any(), anyCollection()))
                    .thenReturn(Collections.emptyList());

            DisponibilidadDiaDTO resultado = citaService.obtenerDisponibilidadPublica(1L, fecha, TipoServicio.ESTANDAR);

           assertThat(resultado.horasDisponibles()).containsExactly(
                    LocalTime.of(8, 0),
                    LocalTime.of(8, 45),
                    LocalTime.of(16, 0)
            );
        }

        @Test
        @DisplayName("Debe excluir las franjas horarias ocupadas por citas confirmadas")
        void obtenerDisponibilidad_ConCitaConfirmada_ExcluyeSlotOcupado() {
            LocalDate fecha = LocalDate.of(2026, 10, 15);
            Puesto puesto = Puesto.builder().taller(tallerEjemplo).build();
            ReflectionTestUtils.setField(puesto, "id", 10L);

            OffsetDateTime citaInicio = fecha.atTime(8, 0).atZone(ZONA_HORARIA).toOffsetDateTime();
            OffsetDateTime citaFin = fecha.atTime(8, 45).atZone(ZONA_HORARIA).toOffsetDateTime();

            Cita citaOcupada = Cita.builder()
                    .fechaInicio(citaInicio)
                    .fechaFin(citaFin)
                    .estado(EstadoCita.CONFIRMADA)
                    .build();

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(tallerEjemplo));
            when(puestoRepository.findByTallerId(1L)).thenReturn(List.of(puesto));
            when(citaRepository.findByTallerIdAndFechaInicioBetweenAndEstadoIn(eq(1L), any(), any(), anyCollection()))
                    .thenReturn(List.of(citaOcupada));

            DisponibilidadDiaDTO resultado = citaService.obtenerDisponibilidadPublica(1L, fecha, TipoServicio.ESTANDAR);

            assertThat(resultado.horasDisponibles()).doesNotContain(LocalTime.of(8, 0));
            assertThat(resultado.horasDisponibles()).containsExactly(
                    LocalTime.of(8, 45),
                    LocalTime.of(16, 0)
            );
        }
    }

    // =========================================================================
    // 2. TESTS DE CANAL PÚBLICO
    // =========================================================================
    @Nested
    @DisplayName("Creación y Seguimiento Público")
    class CanalPublicoTests {

        private CitaPublicaCreateRequestDTO requestDTO;

        @BeforeEach
        void init() {
            requestDTO = new CitaPublicaCreateRequestDTO(
                    "1234BBB",
                    "Juan Perez",
                    "600111222",
                    OffsetDateTime.now(ZONA_HORARIA).plusDays(2),
                    TipoServicio.ESTANDAR,
                    "Ruido al frenar"
            );
        }

        @Test
        @DisplayName("Debe rechazar la reserva si la matrícula ya tiene una cita activa (Anti-Spam)")
        void crearCitaPublica_VehiculoConCitaActiva_LanzaExcepcion() {
            when(tallerRepository.findById(1L)).thenReturn(Optional.of(tallerEjemplo));
            when(citaRepository.existsByTallerIdAndVehiculoMatriculaAndEstadoIn(eq(1L), eq("1234BBB"), anyCollection()))
                    .thenReturn(true);

            assertThatThrownBy(() -> citaService.crearCitaPublica(1L, requestDTO))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ya cuenta con una cita activa");

            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe persistir la cita pública en PENDIENTE_CONFIRMACION")
        void crearCitaPublica_Exitoso() {
            Vehiculo vehiculo = Vehiculo.builder().matricula("1234BBB").build();
            ReflectionTestUtils.setField(vehiculo, "id", 5L);

            Cita citaMock = Cita.builder()
                    .taller(tallerEjemplo)
                    .vehiculo(vehiculo)
                    .estado(EstadoCita.PENDIENTE_CONFIRMACION)
                    .build();
            ReflectionTestUtils.setField(citaMock, "id", 100L);

            CitaResponseDTO responseEsperado = mock(CitaResponseDTO.class);
            when(responseEsperado.estado()).thenReturn(EstadoCita.PENDIENTE_CONFIRMACION);

            when(tallerRepository.findById(1L)).thenReturn(Optional.of(tallerEjemplo));
            when(citaRepository.existsByTallerIdAndVehiculoMatriculaAndEstadoIn(eq(1L), eq("1234BBB"), anyCollection()))
                    .thenReturn(false);
            when(vehiculoRepository.findByMatricula("1234BBB")).thenReturn(Optional.of(vehiculo));
            when(vehiculoRepository.save(any())).thenReturn(vehiculo);
            when(tallerVehiculoRepository.findByTallerIdAndVehiculoId(1L, 5L)).thenReturn(Optional.empty());
            when(citaMapper.toEntity(eq(requestDTO), eq(tallerEjemplo), eq(vehiculo))).thenReturn(citaMock);
            when(citaRepository.save(citaMock)).thenReturn(citaMock);
            when(citaMapper.toResponseDTO(citaMock)).thenReturn(responseEsperado);

            CitaResponseDTO resultado = citaService.crearCitaPublica(1L, requestDTO);

            assertThat(resultado).isNotNull();
            assertThat(resultado.estado()).isEqualTo(EstadoCita.PENDIENTE_CONFIRMACION);
            verify(tallerVehiculoRepository).save(any(TallerVehiculo.class));
            verify(citaRepository).save(citaMock);
        }

        @Test
        @DisplayName("Debe lanzar excepción si el token UUID no existe")
        void obtenerCitaPorToken_NoExiste_LanzaExcepcion() {
            UUID tokenInvalido = UUID.randomUUID();
            when(citaRepository.findByTokenUuid(tokenInvalido)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.obtenerCitaPorToken(tokenInvalido))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No se encontró ninguna cita");
        }
    }

    // =========================================================================
    // 3. TESTS DE CONFIRMACIÓN Y CANCELACIÓN
    // =========================================================================
    @Nested
    @DisplayName("Gestión del Ciclo de Vida de Citas")
    class CicloDeVidaTests {

        @Test
        @DisplayName("Debe rechazar confirmar si la cita no está en PENDIENTE_CONFIRMACION")
        void confirmarCita_EstadoInvalido_LanzaExcepcion() {
            Cita citaYaConfirmada = Cita.builder().estado(EstadoCita.CONFIRMADA).build();
            ReflectionTestUtils.setField(citaYaConfirmada, "id", 10L);

            when(citaRepository.findByIdAndTallerId(10L, 1L)).thenReturn(Optional.of(citaYaConfirmada));

            assertThatThrownBy(() -> citaService.confirmarCitaPublica(1L, 10L, 2L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Solo se pueden confirmar citas en estado PENDIENTE_CONFIRMACION");
        }

        @Test
        @DisplayName("Debe rechazar confirmar si el elevador/puesto está ocupado en ese horario")
        void confirmarCita_PuestoOcupado_LanzaExcepcion() {
            OffsetDateTime inicio = OffsetDateTime.now(ZONA_HORARIA);
            OffsetDateTime fin = inicio.plusMinutes(30);

            Cita cita = Cita.builder().estado(EstadoCita.PENDIENTE_CONFIRMACION).fechaInicio(inicio).fechaFin(fin).build();
            ReflectionTestUtils.setField(cita, "id", 10L);

            Puesto puesto = Puesto.builder().build();
            ReflectionTestUtils.setField(puesto, "id", 2L);

            when(citaRepository.findByIdAndTallerId(10L, 1L)).thenReturn(Optional.of(cita));
            when(puestoRepository.findByIdAndTallerId(2L, 1L)).thenReturn(Optional.of(puesto));
            when(citaRepository.existsByTallerIdAndPuestoIdAndEstadoInAndFechaInicioLessThanAndFechaFinGreaterThan(
                    eq(1L), eq(2L), anyCollection(), eq(fin), eq(inicio)))
                    .thenReturn(true);

            assertThatThrownBy(() -> citaService.confirmarCitaPublica(1L, 10L, 2L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("El elevador/puesto seleccionado ya está ocupado");
        }

        @Test
        @DisplayName("Debe confirmar correctamente y asignar el puesto al elevador libre")
        void confirmarCita_Exitoso() {
            OffsetDateTime inicio = OffsetDateTime.now(ZONA_HORARIA);
            OffsetDateTime fin = inicio.plusMinutes(30);

            Cita cita = Cita.builder().estado(EstadoCita.PENDIENTE_CONFIRMACION).fechaInicio(inicio).fechaFin(fin).build();
            ReflectionTestUtils.setField(cita, "id", 10L);

            Puesto puesto = Puesto.builder().build();
            ReflectionTestUtils.setField(puesto, "id", 2L);

            CitaResponseDTO responseEsperado = mock(CitaResponseDTO.class);
            when(responseEsperado.estado()).thenReturn(EstadoCita.CONFIRMADA);

            when(citaRepository.findByIdAndTallerId(10L, 1L)).thenReturn(Optional.of(cita));
            when(puestoRepository.findByIdAndTallerId(2L, 1L)).thenReturn(Optional.of(puesto));
            when(citaRepository.existsByTallerIdAndPuestoIdAndEstadoInAndFechaInicioLessThanAndFechaFinGreaterThan(
                    eq(1L), eq(2L), anyCollection(), eq(fin), eq(inicio)))
                    .thenReturn(false);
            when(citaRepository.save(cita)).thenReturn(cita);
            when(citaMapper.toResponseDTO(cita)).thenReturn(responseEsperado);

            CitaResponseDTO resultado = citaService.confirmarCitaPublica(1L, 10L, 2L);

            assertThat(resultado.estado()).isEqualTo(EstadoCita.CONFIRMADA);
            assertThat(cita.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
            assertThat(cita.getPuesto()).isEqualTo(puesto);
            verify(citaRepository).save(cita);
        }

        @Test
        @DisplayName("Debe rechazar cancelar una cita que ya se encuentra en estado final")
        void cancelarCita_YaEnEstadoFinal_LanzaExcepcion() {
            Cita citaCancelada = Cita.builder().estado(EstadoCita.CANCELADA).build();
            ReflectionTestUtils.setField(citaCancelada, "id", 10L);

            when(citaRepository.findByIdAndTallerId(10L, 1L)).thenReturn(Optional.of(citaCancelada));

            assertThatThrownBy(() -> citaService.cancelarCita(1L, 10L, "Motivo"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No se puede cancelar una cita que ya se encuentra en estado CANCELADA");
        }

        @Test
        @DisplayName("Debe cancelar la cita y persistir el motivo correctamente")
        void cancelarCita_Exitoso() {
            Cita cita = Cita.builder().estado(EstadoCita.PENDIENTE_CONFIRMACION).build();
            ReflectionTestUtils.setField(cita, "id", 10L);

            CitaResponseDTO responseEsperado = mock(CitaResponseDTO.class);
            when(responseEsperado.estado()).thenReturn(EstadoCita.CANCELADA);

            when(citaRepository.findByIdAndTallerId(10L, 1L)).thenReturn(Optional.of(cita));
            when(citaRepository.save(cita)).thenReturn(cita);
            when(citaMapper.toResponseDTO(cita)).thenReturn(responseEsperado);

            CitaResponseDTO resultado = citaService.cancelarCita(1L, 10L, "Cliente no puede acudir");

            assertThat(resultado.estado()).isEqualTo(EstadoCita.CANCELADA);
            assertThat(cita.getEstado()).isEqualTo(EstadoCita.CANCELADA);
            assertThat(cita.getMotivo()).isEqualTo("Cliente no puede acudir");
            verify(citaRepository).save(cita);
        }
    }
}