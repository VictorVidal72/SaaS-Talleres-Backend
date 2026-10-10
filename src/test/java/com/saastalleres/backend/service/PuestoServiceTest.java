package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.dto.puesto.PuestoCreateRequestDTO;
import com.saastalleres.backend.dto.puesto.PuestoMapper;
import com.saastalleres.backend.dto.puesto.PuestoResponseDTO;
import com.saastalleres.backend.repository.PuestoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuestoServiceTest {

    @Mock
    private PuestoRepository puestoRepository;

    @Mock
    private TallerRepository tallerRepository;

    @Spy
    private PuestoMapper puestoMapper;

    @InjectMocks
    private PuestoService puestoService;

    private Taller tallerEjemplo;

    @BeforeEach
    void setUp() {
        tallerEjemplo = new Taller();
        tallerEjemplo.setId(1L);
    }

    @Test
    @DisplayName("crearPuesto - Guarda y retorna DTO cuando los datos son válidos")
    void debeCrearPuestoExitosamente() {
        Long tallerId = 1L;
        PuestoCreateRequestDTO requestDTO = new PuestoCreateRequestDTO("Elevador 1");

        Puesto puestoGuardado = Puesto.builder()
                .nombre("Elevador 1")
                .activo(true)
                .taller(tallerEjemplo)
                .build();
        puestoGuardado.setId(10L);

        when(tallerRepository.findById(tallerId)).thenReturn(Optional.of(tallerEjemplo));
        when(puestoRepository.existsByTallerIdAndNombre(tallerId, "Elevador 1")).thenReturn(false);
        when(puestoRepository.save(any(Puesto.class))).thenReturn(puestoGuardado);

        PuestoResponseDTO resultado = puestoService.crearPuesto(tallerId, requestDTO);

        assertNotNull(resultado);
        assertEquals(10L, resultado.id());
        assertEquals("Elevador 1", resultado.nombre());
        assertTrue(resultado.activo());
        assertEquals(tallerId, resultado.tallerId());
        verify(puestoRepository).save(any(Puesto.class));
    }

    @Test
    @DisplayName("crearPuesto - Lanza NoSuchElementException si el taller no existe")
    void debeFallarSiTallerNoExiste() {
        Long tallerId = 99L;
        PuestoCreateRequestDTO requestDTO = new PuestoCreateRequestDTO("Elevador 1");

        when(tallerRepository.findById(tallerId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> puestoService.crearPuesto(tallerId, requestDTO));
        verify(puestoRepository, never()).save(any(Puesto.class));
    }

    @Test
    @DisplayName("crearPuesto - Lanza IllegalArgumentException si el nombre ya existe en el taller")
    void debeFallarSiNombreEstaDuplicadoEnElTaller() {
        Long tallerId = 1L;
        PuestoCreateRequestDTO requestDTO = new PuestoCreateRequestDTO("Elevador 1");

        when(tallerRepository.findById(tallerId)).thenReturn(Optional.of(tallerEjemplo));
        when(puestoRepository.existsByTallerIdAndNombre(tallerId, "Elevador 1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> puestoService.crearPuesto(tallerId, requestDTO));
        verify(puestoRepository, never()).save(any(Puesto.class));
    }

    @Test
    @DisplayName("listarTodosPorTaller - Retorna todos los puestos del taller")
    void debeListarTodosLosPuestosDelTaller() {
        Long tallerId = 1L;
        Puesto p1 = Puesto.builder().nombre("Elevador 1").activo(true).taller(tallerEjemplo).build();
        p1.setId(10L);
        Puesto p2 = Puesto.builder().nombre("Box Mantenimiento").activo(false).taller(tallerEjemplo).build();
        p2.setId(20L);

        when(puestoRepository.findByTallerId(tallerId)).thenReturn(List.of(p1, p2));

        List<PuestoResponseDTO> resultado = puestoService.listarTodosPorTaller(tallerId);

        assertEquals(2, resultado.size());
        verify(puestoRepository).findByTallerId(tallerId);
    }

    @Test
    @DisplayName("listarActivosPorTaller - Retorna únicamente puestos operativos")
    void debeListarSoloPuestosActivos() {
        Long tallerId = 1L;
        Puesto p1 = Puesto.builder().nombre("Elevador 1").activo(true).taller(tallerEjemplo).build();
        p1.setId(10L);

        when(puestoRepository.findByTallerIdAndActivoTrue(tallerId)).thenReturn(List.of(p1));

        List<PuestoResponseDTO> resultado = puestoService.listarActivosPorTaller(tallerId);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).activo());
        verify(puestoRepository).findByTallerIdAndActivoTrue(tallerId);
    }

    @Test
    @DisplayName("cambiarEstadoOperativo - Actualiza estado respetando aislamiento multi-tenant (RN-01)")
    void debeCambiarEstadoOperativo() {
        Long tallerId = 1L;
        Long puestoId = 10L;
        Puesto puestoExistente = Puesto.builder().nombre("Elevador 1").activo(true).taller(tallerEjemplo).build();
        puestoExistente.setId(puestoId);

        when(puestoRepository.findByIdAndTallerId(puestoId, tallerId)).thenReturn(Optional.of(puestoExistente));
        when(puestoRepository.save(puestoExistente)).thenReturn(puestoExistente);

        PuestoResponseDTO resultado = puestoService.cambiarEstadoOperativo(tallerId, puestoId, false);

        assertNotNull(resultado);
        assertFalse(resultado.activo());
        verify(puestoRepository).findByIdAndTallerId(puestoId, tallerId);
        verify(puestoRepository).save(puestoExistente);
    }

    @Test
    @DisplayName("cambiarEstadoOperativo - Lanza NoSuchElementException si el puesto pertenece a otro taller (RN-01)")
    void debeFallarSiPuestoNoPerteneceAlTaller() {
        Long tallerId = 1L;
        Long puestoAjenoId = 50L;

        when(puestoRepository.findByIdAndTallerId(puestoAjenoId, tallerId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> puestoService.cambiarEstadoOperativo(tallerId, puestoAjenoId, false));
        verify(puestoRepository, never()).save(any(Puesto.class));
    }
}