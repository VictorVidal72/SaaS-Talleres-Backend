package com.saastalleres.backend.service;

import com.saastalleres.backend.domain.entity.Cita;
import com.saastalleres.backend.domain.entity.Puesto;
import com.saastalleres.backend.domain.entity.Taller;
import com.saastalleres.backend.domain.entity.TallerVehiculo;
import com.saastalleres.backend.domain.entity.Vehiculo;
import com.saastalleres.backend.domain.enums.EstadoCita;
import com.saastalleres.backend.domain.enums.TipoServicio;
import com.saastalleres.backend.dto.cita.CitaInternaCreateRequestDTO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de dominio y orquestación para el ciclo de vida completo de citas,
 * cálculo de disponibilidad pública, colisiones de elevadores y agenda multi-tenant.
 */
@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final TallerRepository tallerRepository;
    private final PuestoRepository puestoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final TallerVehiculoRepository tallerVehiculoRepository;
    private final CitaMapper citaMapper;

    // Estados donde el vehículo ya se encuentra en trámite activo en el taller
    private static final List<EstadoCita> ESTADOS_CITA_ACTIVA = List.of(
            EstadoCita.PENDIENTE_CONFIRMACION,
            EstadoCita.CONFIRMADA,
            EstadoCita.PROPUESTA_TALLER
    );

    // Estados que bloquean físicamente el elevador/puesto
    private static final List<EstadoCita> ESTADOS_OCUPACION_PUESTO = List.of(
            EstadoCita.CONFIRMADA
    );

    // Zona horaria de operación para los talleres
    private static final ZoneId ZONA_HORARIA = ZoneId.of("Europe/Madrid");

    // =========================================================================
    // 1. CANAL PÚBLICO (CONDUCTOR / CLIENTE FINAL VÍA WEB Y TOKEN UUID)
    // =========================================================================

    /**
     * Calcula los huecos libres en un día concreto para el servicio solicitado.
     * Evalúa la capacidad total de elevadores frente a las citas ya confirmadas,
     * utilizando los horarios de apertura y cierre propios de cada taller.
     */
    @Transactional(readOnly = true)
    public DisponibilidadDiaDTO obtenerDisponibilidadPublica(Long tallerId, LocalDate fecha, TipoServicio tipoServicio) {
        Taller taller = validarYObtenerTaller(tallerId);

        List<Puesto> puestos = puestoRepository.findByTallerId(tallerId);
        int totalPuestos = puestos.size();

        // Sin puestos configurados en el taller no es posible aceptar citas
        if (totalPuestos == 0) {
            return new DisponibilidadDiaDTO(fecha, List.of());
        }

        // Consultar citas confirmadas del día
        OffsetDateTime inicioDia = fecha.atStartOfDay(ZONA_HORARIA).toOffsetDateTime();
        OffsetDateTime finDia = fecha.atTime(LocalTime.MAX).atZone(ZONA_HORARIA).toOffsetDateTime();
        
        List<Cita> citasDelDia = citaRepository.findByTallerIdAndFechaInicioBetweenAndEstadoIn(tallerId, inicioDia, finDia, ESTADOS_OCUPACION_PUESTO);

        int duracionMinutos = tipoServicio.getDuracionMinutos();
        List<LocalTime> horasDisponibles = new ArrayList<>();

        // Proyectar franjas horarias configuradas para este taller concreto
        proyectarFranja(fecha, taller.getHoraAperturaManana(), taller.getHoraCierreManana(), duracionMinutos, totalPuestos, citasDelDia, horasDisponibles);
        proyectarFranja(fecha, taller.getHoraAperturaTarde(), taller.getHoraCierreTarde(), duracionMinutos, totalPuestos, citasDelDia, horasDisponibles);

        return new DisponibilidadDiaDTO(fecha, horasDisponibles);
    }

    /**
     * Procesa la solicitud pública de cita desde el portal web del cliente.
     * Nace en PENDIENTE_CONFIRMACION, sin puesto asignado y con token UUID.
     */
    @Transactional
    public CitaResponseDTO crearCitaPublica(Long tallerId, CitaPublicaCreateRequestDTO dto) {
        Taller taller = validarYObtenerTaller(tallerId);

        // Barrera Anti-Spam
        validarVehiculoSinCitaActiva(tallerId, dto.matricula());

        Vehiculo vehiculo = resolverOActualizarVehiculo(dto.matricula(), dto.nombreCliente(), dto.telefonoCliente());
        registrarOActualizarVisita(taller, vehiculo);

        Cita nuevaCita = citaMapper.toEntity(dto, taller, vehiculo);
        return citaMapper.toResponseDTO(citaRepository.save(nuevaCita));
    }

    /**
     * Consulta el estado de la cita en tiempo real mediante el token UUID.
     */
    @Transactional(readOnly = true)
    public CitaResponseDTO obtenerCitaPorToken(UUID tokenUuid) {
        Cita cita = citaRepository.findByTokenUuid(tokenUuid)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna cita con el token proporcionado"));

        return citaMapper.toResponseDTO(cita);
    }

    /**
     * Permite al conductor cancelar su cita desde la web móvil mediante su token UUID.
     */
    @Transactional
    public CitaResponseDTO cancelarCitaPorToken(UUID tokenUuid, String motivoCancelacion) {
        Cita cita = citaRepository.findByTokenUuid(tokenUuid)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna cita con el token proporcionado"));

        return ejecutarCancelacion(cita, motivoCancelacion);
    }

    // =========================================================================
    // 2. CANAL PRIVADO (PANEL DE GESTIÓN DEL TALLER)
    // =========================================================================

    /**
     * Creación de cita interna desde el panel del taller (teléfono / mostrador).
     */
    @Transactional
    public CitaResponseDTO crearCitaInterna(Long tallerId, CitaInternaCreateRequestDTO dto) {
        Taller taller = validarYObtenerTaller(tallerId);

        OffsetDateTime fechaFinCalculada = dto.tipoServicio().calcularFin(dto.fechaInicio());
        Puesto puesto = null;

        if (dto.puestoId() != null) {
            puesto = validarYObtenerPuestoLibre(tallerId, dto.puestoId(), dto.fechaInicio(), fechaFinCalculada);
        }

        Vehiculo vehiculo = resolverOActualizarVehiculo(dto.matricula(), dto.nombreCliente(), dto.telefonoCliente());
        registrarOActualizarVisita(taller, vehiculo);

        Cita nuevaCita = citaMapper.toEntity(dto, taller, vehiculo, puesto);
        return citaMapper.toResponseDTO(citaRepository.save(nuevaCita));
    }

    /**
     * Confirma una cita pública asignándole un elevador/puesto libre.
     */
    @Transactional
    public CitaResponseDTO confirmarCitaPublica(Long tallerId, Long citaId, Long puestoId) {
        Cita cita = validarYObtenerCita(tallerId, citaId);

        if (cita.getEstado() != EstadoCita.PENDIENTE_CONFIRMACION) {
            throw new BusinessRuleException("Solo se pueden confirmar citas en estado PENDIENTE_CONFIRMACION");
        }

        Puesto puesto = validarYObtenerPuestoLibre(tallerId, puestoId, cita.getFechaInicio(), cita.getFechaFin());

        cita.setPuesto(puesto);
        cita.setEstado(EstadoCita.CONFIRMADA);

        return citaMapper.toResponseDTO(citaRepository.save(cita));
    }

    /**
     * Cancela una cita desde el panel privado de gestión del taller.
     */
    @Transactional
    public CitaResponseDTO cancelarCita(Long tallerId, Long citaId, String motivoCancelacion) {
        Cita cita = validarYObtenerCita(tallerId, citaId);
        return ejecutarCancelacion(cita, motivoCancelacion);
    }

    /**
     * Lista las citas de un taller dentro de un rango temporal para pintar el calendario.
     */
    @Transactional(readOnly = true)
    public List<CitaResponseDTO> listarCitasPorRango(Long tallerId, OffsetDateTime inicio, OffsetDateTime fin) {
        validarYObtenerTaller(tallerId);

        return citaRepository.findByTallerIdAndFechaInicioBetween(tallerId, inicio, fin)
                .stream()
                .map(citaMapper::toResponseDTO)
                .toList();
    }

    // =========================================================================
    // MÉTODOS PRIVADOS REUTILIZABLES (DRY & MULTI-TENANT)
    // =========================================================================

    private void proyectarFranja(
            LocalDate fecha,
            LocalTime apertura,
            LocalTime cierre,
            int duracionMinutos,
            int totalPuestos,
            List<Cita> citasDelDia,
            List<LocalTime> horasDisponibles
    ) {
        LocalTime slotActual = apertura;

        while (!slotActual.plusMinutes(duracionMinutos).isAfter(cierre)) {
            OffsetDateTime slotInicio = fecha.atTime(slotActual).atZone(ZONA_HORARIA).toOffsetDateTime();
            OffsetDateTime slotFin = slotInicio.plusMinutes(duracionMinutos);

            // Contar citas que colisionan en este intervalo temporal
            long citasSolapadas = citasDelDia.stream()
                    .filter(c -> c.getFechaInicio().isBefore(slotFin) && c.getFechaFin().isAfter(slotInicio))
                    .count();

            // Si hay menos citas que elevadores, el hueco está disponible
            if (citasSolapadas < totalPuestos) {
                horasDisponibles.add(slotActual);
            }

            slotActual = slotActual.plusMinutes(duracionMinutos);
        }
    }

    private CitaResponseDTO ejecutarCancelacion(Cita cita, String motivoCancelacion) {
        if (cita.getEstado().esFinal()) {
            throw new BusinessRuleException("No se puede cancelar una cita que ya se encuentra en estado " + cita.getEstado());
        }

        cita.setEstado(EstadoCita.CANCELADA);
        if (motivoCancelacion != null && !motivoCancelacion.isBlank()) {
            cita.setMotivo(motivoCancelacion);
        }

        return citaMapper.toResponseDTO(citaRepository.save(cita));
    }

    private Taller validarYObtenerTaller(Long tallerId) {
        return tallerRepository.findById(tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Taller no encontrado con ID: " + tallerId));
    }

    private Cita validarYObtenerCita(Long tallerId, Long citaId) {
        return citaRepository.findByIdAndTallerId(citaId, tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId + " en este taller"));
    }

    private void validarVehiculoSinCitaActiva(Long tallerId, String matricula) {
        boolean tieneCitaActiva = citaRepository.existsByTallerIdAndVehiculoMatriculaAndEstadoIn(
                tallerId,
                matricula,
                ESTADOS_CITA_ACTIVA
        );

        if (tieneCitaActiva) {
            throw new BusinessRuleException(
                    "El vehículo con matrícula " + matricula + " ya cuenta con una cita activa o en trámite en este taller"
            );
        }
    }

    private Puesto validarYObtenerPuestoLibre(Long tallerId, Long puestoId, OffsetDateTime inicio, OffsetDateTime fin) {
        Puesto puesto = puestoRepository.findByIdAndTallerId(puestoId, tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Puesto no encontrado con ID: " + puestoId + " en este taller"));

        boolean puestoOcupado = citaRepository
                .existsByTallerIdAndPuestoIdAndEstadoInAndFechaInicioLessThanAndFechaFinGreaterThan(
                        tallerId,
                        puestoId,
                        ESTADOS_OCUPACION_PUESTO,
                        fin,
                        inicio
                );

        if (puestoOcupado) {
            throw new BusinessRuleException("El elevador/puesto seleccionado ya está ocupado en la franja horaria solicitada");
        }

        return puesto;
    }

    private Vehiculo resolverOActualizarVehiculo(String matricula, String nombreContacto, String telefonoContacto) {
        return vehiculoRepository.findByMatricula(matricula)
                .map(v -> {
                    v.setNombreContacto(nombreContacto);
                    v.setTelefonoContacto(telefonoContacto);
                    return vehiculoRepository.save(v);
                })
                .orElseGet(() -> {
                    Vehiculo nuevoVehiculo = Vehiculo.builder()
                            .matricula(matricula)
                            .nombreContacto(nombreContacto)
                            .telefonoContacto(telefonoContacto)
                            .build();
                    return vehiculoRepository.save(nuevoVehiculo);
                });
    }

    private void registrarOActualizarVisita(Taller taller, Vehiculo vehiculo) {
        tallerVehiculoRepository.findByTallerIdAndVehiculoId(taller.getId(), vehiculo.getId())
                .ifPresentOrElse(
                        expediente -> {
                            expediente.setUltimaVisita(OffsetDateTime.now());
                            tallerVehiculoRepository.save(expediente);
                        },
                        () -> {
                            TallerVehiculo nuevoExpediente = TallerVehiculo.builder()
                                    .taller(taller)
                                    .vehiculo(vehiculo)
                                    .primeraVisita(OffsetDateTime.now())
                                    .ultimaVisita(OffsetDateTime.now())
                                    .build();
                            tallerVehiculoRepository.save(nuevoExpediente);
                        }
                );
    }
}