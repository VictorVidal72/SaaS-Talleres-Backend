package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.TallerVehiculo;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.dto.vehiculo.VehiculoCreateRequestDTO;
import com.saastalleres.backend.dto.vehiculo.VehiculoMapper;
import com.saastalleres.backend.dto.vehiculo.VehiculoResponseDTO;
import com.saastalleres.backend.repository.TallerRepository;
import com.saastalleres.backend.repository.TallerVehiculoRepository;
import com.saastalleres.backend.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private TallerVehiculoRepository tallerVehiculoRepository;

    @Mock
    private TallerRepository tallerRepository;

    @Mock
    private VehiculoMapper vehiculoMapper;

    @InjectMocks
    private VehiculoService vehiculoService;

    private Taller tallerEjemplo;
    private Vehiculo vehiculoEjemplo;
    private VehiculoCreateRequestDTO requestDTO;
    private VehiculoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        tallerEjemplo = Taller.builder()
            .nombreComercial("Talleres Sanlúcar Motor")
            .slug("talleres-sanlucar-motor")
            .build();
        tallerEjemplo.setId(1L);

        vehiculoEjemplo = Vehiculo.builder()
            .matricula("1234BBB")
            .marca("SEAT")
            .modelo("Ibiza")
            .color("Blanco")
            .nombreContacto("Juan Pérez")
            .telefonoContacto("+34600112233")
            .emailContacto("juan.perez@example.com")
            .build();
        vehiculoEjemplo.setId(100L);

        requestDTO = new VehiculoCreateRequestDTO(
                "1234BBB",
                "SEAT",
                "Ibiza",
                "Blanco",
                "Juan Pérez",
                "+34600112233",
                "juan.perez@example.com"
        );

        responseDTO = new VehiculoResponseDTO(
                100L,
                "1234BBB",
                "SEAT",
                "Ibiza",
                "Blanco",
                "Juan Pérez",
                "+34600112233",
                "juan.perez@example.com",
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Debe registrar un nuevo vehículo y vincularlo al taller correctamente")
    void debeRegistrarNuevoVehiculoYVincular() {
        Long tallerId = 1L;

        when(tallerRepository.findById(tallerId)).thenReturn(Optional.of(tallerEjemplo));
        when(vehiculoRepository.findByMatricula("1234BBB")).thenReturn(Optional.empty());
        when(vehiculoMapper.toEntity(requestDTO)).thenReturn(vehiculoEjemplo);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(vehiculoEjemplo);
        when(tallerVehiculoRepository.existsByTallerIdAndVehiculoId(tallerId, vehiculoEjemplo.getId())).thenReturn(false);
        when(vehiculoMapper.toResponseDTO(vehiculoEjemplo)).thenReturn(responseDTO);

        VehiculoResponseDTO resultado = vehiculoService.registrarOActualizar(tallerId, requestDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.matricula()).isEqualTo("1234BBB");
        verify(vehiculoRepository, times(1)).save(any(Vehiculo.class));
        verify(tallerVehiculoRepository, times(1)).save(any(TallerVehiculo.class));
    }

    @Test
    @DisplayName("Debe actualizar datos de contacto si el vehículo ya existe sin duplicarlo")
    void debeActualizarContactoSiVehiculoExiste() {
        Long tallerId = 1L;
        Vehiculo vehiculoExistente = Vehiculo.builder()
                .matricula("1234BBB")
                .nombreContacto("Antiguo Nombre")
                .telefonoContacto("+34699999999")
                .build();
        vehiculoExistente.setId(100L);

        when(tallerRepository.findById(tallerId)).thenReturn(Optional.of(tallerEjemplo));
        when(vehiculoRepository.findByMatricula("1234BBB")).thenReturn(Optional.of(vehiculoExistente));
        when(vehiculoRepository.save(vehiculoExistente)).thenReturn(vehiculoExistente);
        when(tallerVehiculoRepository.existsByTallerIdAndVehiculoId(tallerId, 100L)).thenReturn(true);
        when(vehiculoMapper.toResponseDTO(vehiculoExistente)).thenReturn(responseDTO);

        VehiculoResponseDTO resultado = vehiculoService.registrarOActualizar(tallerId, requestDTO);

        assertThat(resultado).isNotNull();
        assertThat(vehiculoExistente.getNombreContacto()).isEqualTo("Juan Pérez");
        assertThat(vehiculoExistente.getTelefonoContacto()).isEqualTo("+34600112233");
        verify(tallerVehiculoRepository, never()).save(any(TallerVehiculo.class));
    }

    @Test
    @DisplayName("RN-01: Debe retornar vacío si el vehículo existe pero no está vinculado a ese taller")
    void debeRetornarVacioSiVehiculoNoPerteneceAlTaller() {
        Long tallerId = 1L;
        String matricula = "1234BBB";

        when(vehiculoRepository.findByMatricula(matricula)).thenReturn(Optional.of(vehiculoEjemplo));
        when(tallerVehiculoRepository.existsByTallerIdAndVehiculoId(tallerId, vehiculoEjemplo.getId())).thenReturn(false);

        Optional<VehiculoResponseDTO> resultado = vehiculoService.buscarPorMatriculaEnTaller(tallerId, matricula);

        assertThat(resultado).isEmpty();
        verify(vehiculoMapper, never()).toResponseDTO(any());
    }

    @Test
    @DisplayName("Lanza excepción si el taller no existe en base de datos")
    void debeLanzarExcepcionSiTallerNoExiste() {
        Long tallerInexistenteId = 999L;
        when(tallerRepository.findById(tallerInexistenteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculoService.registrarOActualizar(tallerInexistenteId, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Taller no encontrado");

        verify(vehiculoRepository, never()).save(any());
    }
}