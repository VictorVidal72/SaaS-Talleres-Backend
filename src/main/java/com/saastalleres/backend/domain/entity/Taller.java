package com.saastalleres.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Entidad de dominio que representa al Taller (Inquilino / Tenant).
 * Raíz del aislamiento multi-tenant del sistema.
 */
@Entity
@Table(name = "talleres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taller extends BaseEntity {

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "cif", nullable = false, unique = true, length = 20)
    private String cif;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "direccion", nullable = false, columnDefinition = "TEXT")
    private String direccion;

    @Column(name = "ciudad", nullable = false, length = 100)
    @Builder.Default
    private String ciudad = "Sanlúcar de Barrameda";

    @Column(name = "horario", columnDefinition = "TEXT")
    private String horario;

    // Horarios operativos estructurados para el cálculo de disponibilidad
    @Column(name = "hora_apertura_manana", nullable = false)
    @Builder.Default
    private LocalTime horaAperturaManana = LocalTime.of(8, 30);

    @Column(name = "hora_cierre_manana", nullable = false)
    @Builder.Default
    private LocalTime horaCierreManana = LocalTime.of(13, 30);

    @Column(name = "hora_apertura_tarde", nullable = false)
    @Builder.Default
    private LocalTime horaAperturaTarde = LocalTime.of(15, 30);

    @Column(name = "hora_cierre_tarde", nullable = false)
    @Builder.Default
    private LocalTime horaCierreTarde = LocalTime.of(19, 30);

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}