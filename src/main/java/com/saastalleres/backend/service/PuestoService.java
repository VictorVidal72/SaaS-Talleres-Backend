package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.dto.puesto.PuestoCreateRequestDTO;
import com.saastalleres.backend.dto.puesto.PuestoMapper;
import com.saastalleres.backend.dto.puesto.PuestoResponseDTO;
import com.saastalleres.backend.repository.PuestoRepository;
import com.saastalleres.backend.repository.TallerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Servicio de dominio responsable de la gestión de capacidad física
 * (elevadores y boxes de trabajo) con aislamiento multi-tenant estricto (RF-05, RN-01).
 */
@Service
@RequiredArgsConstructor
public class PuestoService {

    private final PuestoRepository puestoRepository;
    private final TallerRepository tallerRepository;
    private final PuestoMapper puestoMapper;

    /**
     * Da de alta un nuevo puesto físico en el taller garantizando unicidad de nombre (RF-05).
     */
    @Transactional
    public PuestoResponseDTO crearPuesto(Long tallerId, PuestoCreateRequestDTO requestDTO) {
        Taller taller = tallerRepository.findById(tallerId)
                .orElseThrow(() -> new NoSuchElementException("Taller no encontrado con id: " + tallerId));

        if (puestoRepository.existsByTallerIdAndNombre(tallerId, requestDTO.nombre())) {
            throw new IllegalArgumentException(
                    "Ya existe un puesto con el nombre '" + requestDTO.nombre() + "' en este taller");
        }

        Puesto puesto = puestoMapper.toEntity(requestDTO);
        puesto.setTaller(taller);

        Puesto puestoGuardado = puestoRepository.save(puesto);
        return puestoMapper.toResponseDTO(puestoGuardado);
    }

    /**
     * Obtiene todos los puestos físicos configurados por el taller para su panel de administración (RF-05).
     */
    @Transactional(readOnly = true)
    public List<PuestoResponseDTO> listarTodosPorTaller(Long tallerId) {
        return puestoRepository.findByTallerId(tallerId)
                .stream()
                .map(puestoMapper::toResponseDTO)
                .toList();
    }

    /**
     * Obtiene únicamente los puestos operativos para el motor de reservas y citas (RF-07, RN-02).
     */
    @Transactional(readOnly = true)
    public List<PuestoResponseDTO> listarActivosPorTaller(Long tallerId) {
        return puestoRepository.findByTallerIdAndActivoTrue(tallerId)
                .stream()
                .map(puestoMapper::toResponseDTO)
                .toList();
    }

    /**
     * Conmuta el estado operativo de un box (activo / mantenimiento).
     * Aplica RN-01 validando que el recurso pertenezca obligatoriamente al taller solicitante.
     */
    @Transactional
    public PuestoResponseDTO cambiarEstadoOperativo(Long tallerId, Long puestoId, boolean activo) {
        Puesto puesto = puestoRepository.findByIdAndTallerId(puestoId, tallerId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Puesto no encontrado con id " + puestoId + " para el taller " + tallerId));

        puesto.setActivo(activo);
        Puesto puestoActualizado = puestoRepository.save(puesto);
        return puestoMapper.toResponseDTO(puestoActualizado);
    }
}