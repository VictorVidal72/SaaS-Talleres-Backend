package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.AutorizacionOperacion;
import com.saastalleres.backend.domain.entity.OrdenTrabajo;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.domain.enums.EstadoAutorizacion;
import com.saastalleres.backend.domain.enums.EstadoOrdenTrabajo;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionCreateRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionOperacionMapper;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResolverRequestDTO;
import com.saastalleres.backend.dto.autorizacionoperacion.AutorizacionResponseDTO;
import com.saastalleres.backend.exception.BusinessRuleException;
import com.saastalleres.backend.exception.ResourceNotFoundException;
import com.saastalleres.backend.repository.AutorizacionOperacionRepository;
import com.saastalleres.backend.repository.OrdenTrabajoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - AutorizacionOperacionService")
class AutorizacionOperacionServiceTest {

    @Mock
    private AutorizacionOperacionRepository autorizacionRepository;

    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Mock
    private TallerRepository tallerRepository;

    @Spy
    private AutorizacionOperacionMapper autorizacionMapper;

    @InjectMocks
    private AutorizacionOperacionService service;

    private Taller taller;
    private Vehiculo vehiculo;
    private OrdenTrabajo ordenTrabajo;
    private AutorizacionOperacion autorizacion;
    private UUID tokenUuid;

    @BeforeEach
    void setUp() {
        taller = Taller.builder()
                .nombreComercial("Taller Sanlúcar")
                .cif("B12345678")
                .build();
        taller.setId(1L);

        vehiculo = Vehiculo.builder()
                .matricula("1234BBB")
                .nombreContacto("Juan Pérez")
                .telefonoContacto("600112233")
                .build();
        vehiculo.setId(10L);

        ordenTrabajo = OrdenTrabajo.builder()
                .taller(taller)
                .vehiculo(vehiculo)
                .estado(EstadoOrdenTrabajo.EN_REPARACION)
                .kilometrajeEntrada(120000)
                .costeBase(new BigDecimal("100.00"))
                .build();
        ordenTrabajo.setId(50L);

        tokenUuid = UUID.randomUUID();
        autorizacion = AutorizacionOperacion.builder()
                .tokenUuid(tokenUuid)
                .taller(taller)
                .ordenTrabajo(ordenTrabajo)
                .descripcion("Discos de freno desgastados")
                .costeExtra(new BigDecimal("150.00"))
                .estado(EstadoAutorizacion.PENDIENTE)
                .build();
        autorizacion.setId(100L);
    }

    // ========================================================================
    // CASOS DE USO: solicitarAutorizacion
    // ========================================================================

    @Test
    @DisplayName("solicitarAutorizacion - Éxito cuando taller y orden son válidos y orden activa")
    void solicitarAutorizacion_Exito() {
        AutorizacionCreateRequestDTO dto = new AutorizacionCreateRequestDTO(
                50L, "Discos de freno desgastados", new BigDecimal("150.00"));

        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
        when(ordenTrabajoRepository.findByIdAndTallerId(50L, 1L)).thenReturn(Optional.of(ordenTrabajo));
        when(autorizacionRepository.save(any(AutorizacionOperacion.class))).thenAnswer(invocation -> {
            AutorizacionOperacion guardada = invocation.getArgument(0);
            guardada.setId(100L);
            return guardada;
        });

        AutorizacionResponseDTO respuesta = service.solicitarAutorizacion(1L, dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.id()).isEqualTo(100L);
        assertThat(respuesta.tallerId()).isEqualTo(1L);
        assertThat(respuesta.ordenTrabajoId()).isEqualTo(50L);
        assertThat(respuesta.matriculaVehiculo()).isEqualTo("1234BBB");
        assertThat(respuesta.costeExtra()).isEqualByComparingTo("150.00");
        assertThat(respuesta.estado()).isEqualTo(EstadoAutorizacion.PENDIENTE);
        verify(autorizacionRepository).save(any(AutorizacionOperacion.class));
    }

    @Test
    @DisplayName("solicitarAutorizacion - Falla con ResourceNotFoundException si el taller no existe")
    void solicitarAutorizacion_TallerNoExiste() {
        AutorizacionCreateRequestDTO dto = new AutorizacionCreateRequestDTO(
                50L, "Discos rotos", new BigDecimal("150.00"));

        when(tallerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitarAutorizacion(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Taller no encontrado");

        verify(autorizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitarAutorizacion - Falla con ResourceNotFoundException si la orden es de otro taller (RN-01)")
    void solicitarAutorizacion_OrdenAjenaOTallerInvalido() {
        AutorizacionCreateRequestDTO dto = new AutorizacionCreateRequestDTO(
                50L, "Discos rotos", new BigDecimal("150.00"));

        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
        when(ordenTrabajoRepository.findByIdAndTallerId(50L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitarAutorizacion(1L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Orden de trabajo no encontrada");

        verify(autorizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitarAutorizacion - Falla con BusinessRuleException si la orden ya está ENTREGADO")
    void solicitarAutorizacion_OrdenYaEntregada() {
        ordenTrabajo.setEstado(EstadoOrdenTrabajo.ENTREGADO);
        AutorizacionCreateRequestDTO dto = new AutorizacionCreateRequestDTO(
                50L, "Discos rotos", new BigDecimal("150.00"));

        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
        when(ordenTrabajoRepository.findByIdAndTallerId(50L, 1L)).thenReturn(Optional.of(ordenTrabajo));

        assertThatThrownBy(() -> service.solicitarAutorizacion(1L, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya entregada");

        verify(autorizacionRepository, never()).save(any());
    }

    // ========================================================================
    // CASOS DE USO: consultarPorToken
    // ========================================================================

    @Test
    @DisplayName("consultarPorToken - Éxito devuelve DTO con token válido")
    void consultarPorToken_Exito() {
        when(autorizacionRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(autorizacion));

        AutorizacionResponseDTO respuesta = service.consultarPorToken(tokenUuid);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.tokenUuid()).isEqualTo(tokenUuid);
        assertThat(respuesta.estado()).isEqualTo(EstadoAutorizacion.PENDIENTE);
    }

    @Test
    @DisplayName("consultarPorToken - Falla con ResourceNotFoundException si el UUID no existe")
    void consultarPorToken_TokenInexistente() {
        UUID tokenInvalido = UUID.randomUUID();
        when(autorizacionRepository.findByTokenUuid(tokenInvalido)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultarPorToken(tokenInvalido))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Autorización no encontrada");
    }

    // ========================================================================
    // CASOS DE USO: resolverAutorizacion (Consentimiento y Auditoría RN-07)
    // ========================================================================

    @Test
    @DisplayName("resolverAutorizacion - Aprobación sella estado AUTORIZADO, fecha, IP y User-Agent")
    void resolverAutorizacion_Aprobado() {
        AutorizacionResolverRequestDTO dto = new AutorizacionResolverRequestDTO(true, null);

        when(autorizacionRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(autorizacion));
        when(autorizacionRepository.save(any(AutorizacionOperacion.class))).thenAnswer(i -> i.getArgument(0));

        AutorizacionResponseDTO respuesta = service.resolverAutorizacion(
                tokenUuid, dto, "192.168.1.50", "Mozilla/5.0 (iPhone)");

        assertThat(respuesta.estado()).isEqualTo(EstadoAutorizacion.AUTORIZADO);
        assertThat(autorizacion.getFechaResolucion()).isNotNull();
        assertThat(autorizacion.getIpConsentimiento()).isEqualTo("192.168.1.50");
        assertThat(autorizacion.getUserAgentConsentimiento()).isEqualTo("Mozilla/5.0 (iPhone)");
        verify(autorizacionRepository).save(autorizacion);
    }

    @Test
    @DisplayName("resolverAutorizacion - Rechazo sella estado RECHAZADO con auditoría")
    void resolverAutorizacion_Rechazado() {
        AutorizacionResolverRequestDTO dto = new AutorizacionResolverRequestDTO(false, "Demasiado caro");

        when(autorizacionRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(autorizacion));
        when(autorizacionRepository.save(any(AutorizacionOperacion.class))).thenAnswer(i -> i.getArgument(0));

        AutorizacionResponseDTO respuesta = service.resolverAutorizacion(
                tokenUuid, dto, "192.168.1.50", "Mozilla/5.0 (Android)");

        assertThat(respuesta.estado()).isEqualTo(EstadoAutorizacion.RECHAZADO);
        assertThat(autorizacion.getFechaResolucion()).isNotNull();
        assertThat(autorizacion.getIpConsentimiento()).isEqualTo("192.168.1.50");
        verify(autorizacionRepository).save(autorizacion);
    }

    @Test
    @DisplayName("resolverAutorizacion - Falla con BusinessRuleException si ya fue resuelta previamente")
    void resolverAutorizacion_YaResuelta() {
        autorizacion.setEstado(EstadoAutorizacion.AUTORIZADO); // Ya resuelta
        AutorizacionResolverRequestDTO dto = new AutorizacionResolverRequestDTO(true, null);

        when(autorizacionRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(autorizacion));

        assertThatThrownBy(() -> service.resolverAutorizacion(
                tokenUuid, dto, "192.168.1.50", "Mozilla/5.0"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya fue resuelta previamente");

        verify(autorizacionRepository, never()).save(any());
    }

    // ========================================================================
    // CASOS DE USO: Consultas Internas
    // ========================================================================

    @Test
    @DisplayName("listarPorOrden - Éxito devuelve lista de autorizaciones del taller")
    void listarPorOrden_Exito() {
        when(ordenTrabajoRepository.existsByIdAndTallerId(50L, 1L)).thenReturn(true);
        when(autorizacionRepository.findByOrdenTrabajoIdAndTallerId(50L, 1L))
                .thenReturn(List.of(autorizacion));

        List<AutorizacionResponseDTO> lista = service.listarPorOrden(1L, 50L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).ordenTrabajoId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("listarPorOrden - Falla con ResourceNotFoundException si la orden no existe en el taller")
    void listarPorOrden_OrdenNoExisteEnTaller() {
        when(ordenTrabajoRepository.existsByIdAndTallerId(50L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorOrden(1L, 50L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Orden de trabajo no encontrada");

        verify(autorizacionRepository, never()).findByOrdenTrabajoIdAndTallerId(any(), any());
    }

    @Test
    @DisplayName("consultarPorIdYTaller - Éxito devuelve autorización aislada por tenant")
    void consultarPorIdYTaller_Exito() {
        when(autorizacionRepository.findByIdAndTallerId(100L, 1L)).thenReturn(Optional.of(autorizacion));

        AutorizacionResponseDTO respuesta = service.consultarPorIdYTaller(1L, 100L);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.id()).isEqualTo(100L);
    }
}