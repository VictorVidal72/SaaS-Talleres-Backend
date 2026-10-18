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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de dominio y orquestación para el ciclo de vida de las órdenes de trabajo,
 * recepción física de vehículos (check-in) y tablero Kanban operativo.
 */
@Service
@RequiredArgsConstructor
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final TallerRepository tallerRepository;
    private final VehiculoRepository vehiculoRepository;
    private final CitaRepository citaRepository;
    private final OrdenTrabajoMapper ordenTrabajoMapper;

    // =========================================================================
    // 1. RECEPCIÓN Y APERTURA DE ORDEN (CHECK-IN)
    // =========================================================================

    /**
     * Registra la recepción física del vehículo en el taller y abre una nueva orden de trabajo.
     * Si la orden se origina desde una cita previa, valida la coherencia y transiciona la cita a FINALIZADA.
     */
    @Transactional
    public OrdenTrabajoResponseDTO crearOrdenTrabajo(Long tallerId, OrdenTrabajoCreateRequestDTO dto) {
        Taller taller = validarYObtenerTaller(tallerId);
        Vehiculo vehiculo = validarYObtenerVehiculo(dto.vehiculoId());

        Cita citaAsociada = null;
        if (dto.citaId() != null) {
            citaAsociada = vincularYValidarCita(tallerId, dto.citaId(), vehiculo.getId());
        }

        OrdenTrabajo orden = OrdenTrabajo.builder()
                .taller(taller)
                .vehiculo(vehiculo)
                .cita(citaAsociada)
                .kilometrajeEntrada(dto.kilometrajeEntrada())
                .diagnostico(dto.diagnostico())
                .costeBase(dto.costeBase())
                .estado(EstadoOrdenTrabajo.RECEPCIONADO)
                .fechaLlegada(OffsetDateTime.now())
                .build();

        OrdenTrabajo ordenGuardada = ordenTrabajoRepository.save(orden);

        // Al recepcionar físicamente el vehículo, la reserva en agenda concluye su ciclo
        if (citaAsociada != null) {
            citaAsociada.setEstado(EstadoCita.FINALIZADA);
            citaRepository.save(citaAsociada);
        }

        return ordenTrabajoMapper.toResponseDTO(ordenGuardada);
    }

    // =========================================================================
    // 2. CICLO DE VIDA OPERATIVO (TABLERO KANBAN)
    // =========================================================================

    /**
     * Transiciona el estado de la orden de trabajo en el tablero Kanban y actualiza notas técnicas.
     * Si pasa a ENTREGADO, sella la fecha de entrega física (RN-08).
     */
    @Transactional
    public OrdenTrabajoResponseDTO actualizarEstado(Long tallerId, Long ordenId, OrdenTrabajoEstadoUpdateRequestDTO dto) {
        validarYObtenerTaller(tallerId);
        OrdenTrabajo orden = ordenTrabajoRepository.findByIdAndTallerId(ordenId, tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de trabajo no encontrada con ID: " + ordenId));

        if (orden.getEstado().isEsEstadoFinal()) {
            throw new BusinessRuleException("No se puede modificar una orden de trabajo que ya ha sido entregada");
        }

        orden.setEstado(dto.nuevoEstado());

        if (dto.diagnostico() != null && !dto.diagnostico().isBlank()) {
            orden.setDiagnostico(dto.diagnostico());
        }

        if (dto.nuevoEstado() == EstadoOrdenTrabajo.ENTREGADO) {
            orden.setFechaEntrega(OffsetDateTime.now());
        }

        OrdenTrabajo ordenActualizada = ordenTrabajoRepository.save(orden);
        return ordenTrabajoMapper.toResponseDTO(ordenActualizada);
    }

    // =========================================================================
    // 3. CONSULTAS INTERNAS DEL TALLER (MULTI-TENANT RN-01)
    // =========================================================================

    @Transactional(readOnly = true)
    public OrdenTrabajoResponseDTO obtenerPorId(Long tallerId, Long ordenId) {
        validarYObtenerTaller(tallerId);
        return ordenTrabajoRepository.findByIdAndTallerId(ordenId, tallerId)
                .map(ordenTrabajoMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de trabajo no encontrada con ID: " + ordenId));
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponseDTO> listarPorTaller(Long tallerId) {
        validarYObtenerTaller(tallerId);
        return ordenTrabajoRepository.findByTallerId(tallerId)
                .stream()
                .map(ordenTrabajoMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponseDTO> listarPorEstados(Long tallerId, Collection<EstadoOrdenTrabajo> estados) {
        validarYObtenerTaller(tallerId);
        return ordenTrabajoRepository.findByTallerIdAndEstadoIn(tallerId, estados)
                .stream()
                .map(ordenTrabajoMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponseDTO> listarHistorialVehiculo(Long tallerId, Long vehiculoId) {
        validarYObtenerTaller(tallerId);
        validarYObtenerVehiculo(vehiculoId);
        return ordenTrabajoRepository.findByTallerIdAndVehiculoId(tallerId, vehiculoId)
                .stream()
                .map(ordenTrabajoMapper::toResponseDTO)
                .toList();
    }

    // =========================================================================
    // 4. CANAL PÚBLICO / SEGUIMIENTO CLIENTE (RN-03, RN-08)
    // =========================================================================

    /**
     * Consulta pública del avance de reparación mediante el token UUID seguro indexado.
     */
    @Transactional(readOnly = true)
    public OrdenTrabajoResponseDTO obtenerPorTokenUuid(UUID tokenUuid) {
        return ordenTrabajoRepository.findByTokenUuid(tokenUuid)
                .map(ordenTrabajoMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada para el token de seguimiento indicado"));
    }

    // =========================================================================
    // MÉTODOS AUXILIARES Y VALIDACIONES DEFENSIVAS
    // =========================================================================

    private Taller validarYObtenerTaller(Long tallerId) {
        return tallerRepository.findById(tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Taller no encontrado con ID: " + tallerId));
    }

    private Vehiculo validarYObtenerVehiculo(Long vehiculoId) {
        return vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con ID: " + vehiculoId));
    }

    private Cita vincularYValidarCita(Long tallerId, Long citaId, Long vehiculoId) {
        Cita cita = citaRepository.findByIdAndTallerId(citaId, tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId + " para este taller"));

        if (!cita.getVehiculo().getId().equals(vehiculoId)) {
            throw new BusinessRuleException("El vehículo especificado no coincide con el vehículo asignado a la cita previa");
        }

        if (ordenTrabajoRepository.findByCitaId(citaId).isPresent()) {
            throw new BusinessRuleException("Ya existe una orden de trabajo activa asociada a esta cita");
        }

        return cita;
    }
}