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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de la gestión de flota y recepción de vehículos.
 * Implementa RN-01 (Aislamiento Multi-Tenant), idempotencia en el alta
 * y soporte para el modelo de acceso sin contraseñas (RF-03c).
 */
@Service
@RequiredArgsConstructor
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final TallerVehiculoRepository tallerVehiculoRepository;
    private final TallerRepository tallerRepository;
    private final VehiculoMapper vehiculoMapper;

    /**
     * Recepciona o da de alta un vehículo en el taller autenticado (RF-03 / RF-08).
     * Si la matrícula ya existe globalmente en la plataforma, actualiza los datos
     * de contacto vigentes y asegura el vínculo multi-tenant en TallerVehiculo (RN-01).
     */
    @Transactional
    public VehiculoResponseDTO registrarOActualizar(Long tallerId, VehiculoCreateRequestDTO dto) {
        Taller taller = tallerRepository.findById(tallerId)
                .orElseThrow(() -> new IllegalArgumentException("Taller no encontrado con ID: " + tallerId));

        // 1. Obtener vehículo existente o instanciar y persistir uno nuevo
        Vehiculo vehiculo = vehiculoRepository.findByMatricula(dto.matricula())
                .map(existente -> actualizarDatosContacto(existente, dto))
                .orElseGet(() -> vehiculoRepository.save(vehiculoMapper.toEntity(dto)));

        // 2. Garantizar la vinculación multi-tenant con este taller (RN-01)
        vincularTallerSiNoExiste(taller, vehiculo);

        // 3. Devolver contrato de salida inmutable desacoplado
        return vehiculoMapper.toResponseDTO(vehiculo);
    }

    /**
     * Búsqueda de vehículo por matrícula restringida al taller actual (RN-01).
     */
    @Transactional(readOnly = true)
    public Optional<VehiculoResponseDTO> buscarPorMatriculaEnTaller(Long tallerId, String matricula) {
        String matriculaNormalizada = normalizarTexto(matricula);

        return vehiculoRepository.findByMatricula(matriculaNormalizada)
                .filter(v -> tallerVehiculoRepository.existsByTallerIdAndVehiculoId(tallerId, v.getId()))
                .map(vehiculoMapper::toResponseDTO);
    }

    /**
     * Soporte a RF-03c: Validación de identidad sin credenciales (matrícula + teléfono)
     * para recuperar el enlace de seguimiento activo.
     */
    @Transactional(readOnly = true)
    public Optional<VehiculoResponseDTO> buscarPorMatriculaYTelefonoEnTaller(Long tallerId, String matricula, String telefono) {
        String matriculaNormalizada = normalizarTexto(matricula);
        String telefonoNormalizado = normalizarTexto(telefono);

        return vehiculoRepository.findByMatricula(matriculaNormalizada)
                .filter(v -> v.getTelefonoContacto() != null && v.getTelefonoContacto().equals(telefonoNormalizado))
                .filter(v -> tallerVehiculoRepository.existsByTallerIdAndVehiculoId(tallerId, v.getId()))
                .map(vehiculoMapper::toResponseDTO);
    }

    /**
     * Lista todos los vehículos vinculados al taller autenticado (RN-01).
     */
    @Transactional(readOnly = true)
    public List<VehiculoResponseDTO> listarFlotaTaller(Long tallerId) {
        return tallerVehiculoRepository.findByTallerId(tallerId).stream()
                .map(TallerVehiculo::getVehiculo)
                .map(vehiculoMapper::toResponseDTO)
                .toList();
    }

    private Vehiculo actualizarDatosContacto(Vehiculo vehiculo, VehiculoCreateRequestDTO dto) {
        vehiculo.setNombreContacto(dto.nombreContacto());
        vehiculo.setTelefonoContacto(dto.telefonoContacto());
        vehiculo.setEmailContacto(dto.emailContacto());

        if (dto.marca() != null && !dto.marca().isBlank()) {
            vehiculo.setMarca(dto.marca());
        }
        if (dto.modelo() != null && !dto.modelo().isBlank()) {
            vehiculo.setModelo(dto.modelo());
        }
        if (dto.color() != null && !dto.color().isBlank()) {
            vehiculo.setColor(dto.color());
        }

        return vehiculoRepository.save(vehiculo);
    }

    private void vincularTallerSiNoExiste(Taller taller, Vehiculo vehiculo) {
        boolean yaVinculado = tallerVehiculoRepository.existsByTallerIdAndVehiculoId(taller.getId(), vehiculo.getId());
        if (!yaVinculado) {
            TallerVehiculo asociacion = TallerVehiculo.builder()
                    .taller(taller)
                    .vehiculo(vehiculo)
                    .build();
            tallerVehiculoRepository.save(asociacion);
        }
    }

    private String normalizarTexto(String valor) {
        return valor != null ? valor.trim().toUpperCase() : "";
    }
}