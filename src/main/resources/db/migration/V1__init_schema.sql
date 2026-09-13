-- ============================================================================
-- V1__init_schema.sql
-- SaaS Talleres Mecánicos - Inicialización de Esquema Relacional
-- ============================================================================

-- 0. HABILITAR EXTENSIONES POSTGRESQL 16
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ============================================================================
-- BLOQUE 1: INFRAESTRUCTURA Y MAESTROS
-- ============================================================================

-- 1. TABLA: TALLERES (Inquilino Raíz / Tenant)
CREATE TABLE talleres (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_comercial VARCHAR(150) NOT NULL,
    cif VARCHAR(20) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    telefono VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL,
    direccion TEXT NOT NULL,
    ciudad VARCHAR(100) NOT NULL DEFAULT 'Sanlúcar de Barrameda',
    horario TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. TABLA: USUARIOS (Personal Interno con Acceso RBAC)
CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    taller_id BIGINT REFERENCES talleres(id) ON DELETE RESTRICT,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. TABLA: PUESTOS (Elevadores / Boxes de Trabajo)
CREATE TABLE puestos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    taller_id BIGINT NOT NULL REFERENCES talleres(id) ON DELETE CASCADE,
    nombre VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. TABLA: VEHICULOS (Flota Global y Contacto del Conductor)
CREATE TABLE vehiculos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    matricula VARCHAR(20) NOT NULL UNIQUE,
    marca VARCHAR(50),
    modelo VARCHAR(50),
    color VARCHAR(30),
    nombre_contacto VARCHAR(100) NOT NULL,
    telefono_contacto VARCHAR(20) NOT NULL,
    email_contacto VARCHAR(150),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. TABLA INTERMEDIA: TALLERES_VEHICULOS (Relación N:M)
CREATE TABLE talleres_vehiculos (
    taller_id BIGINT NOT NULL REFERENCES talleres(id) ON DELETE CASCADE,
    vehiculo_id BIGINT NOT NULL REFERENCES vehiculos(id) ON DELETE RESTRICT,
    primera_visita TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_visita TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (taller_id, vehiculo_id)
);

-- ============================================================================
-- BLOQUE 2: TRANSACCIONAL Y OPERATIVO
-- ============================================================================

-- 6. TABLA: CITAS (Agenda con Restricción Anti-Solapes)
CREATE TABLE citas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    taller_id BIGINT NOT NULL REFERENCES talleres(id) ON DELETE CASCADE,
    puesto_id BIGINT REFERENCES puestos(id) ON DELETE RESTRICT,
    vehiculo_id BIGINT NOT NULL REFERENCES vehiculos(id) ON DELETE RESTRICT,
    fecha_inicio TIMESTAMPTZ NOT NULL,
    fecha_fin TIMESTAMPTZ NOT NULL,
    tipo_servicio VARCHAR(30) NOT NULL,
    motivo TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_CONFIRMACION',
    motivo_rechazo_o_propuesta TEXT,
    origen VARCHAR(20) NOT NULL DEFAULT 'WEB',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_citas_rango_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_citas_tipo_servicio CHECK (tipo_servicio IN ('ESTANDAR', 'DIAGNOSIS')),
    CONSTRAINT chk_citas_estado CHECK (estado IN ('PENDIENTE_CONFIRMACION', 'CONFIRMADA', 'RECHAZADA', 'PROPUESTA_TALLER', 'CANCELADA')),
    CONSTRAINT chk_citas_origen CHECK (origen IN ('WEB', 'MANUAL')),
    CONSTRAINT uq_no_solapamiento_puesto EXCLUDE USING gist (
        puesto_id WITH =,
        tstzrange(fecha_inicio, fecha_fin) WITH &&
    ) WHERE (estado = 'CONFIRMADA' AND puesto_id IS NOT NULL)
);

-- 7. TABLA: ORDENES_TRABAJO (Estancia Física y Ciclo de Reparación)
CREATE TABLE ordenes_trabajo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    taller_id BIGINT NOT NULL REFERENCES talleres(id) ON DELETE CASCADE,
    vehiculo_id BIGINT NOT NULL REFERENCES vehiculos(id) ON DELETE RESTRICT,
    cita_id BIGINT UNIQUE REFERENCES citas(id) ON DELETE SET NULL,
    fecha_llegada TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega TIMESTAMPTZ,
    estado VARCHAR(30) NOT NULL DEFAULT 'RECEPCIONADO',
    diagnostico TEXT,
    kilometraje_entrada INTEGER NOT NULL,
    coste_base NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ordenes_kilometraje CHECK (kilometraje_entrada >= 0),
    CONSTRAINT chk_ordenes_coste_base CHECK (coste_base >= 0.00),
    CONSTRAINT chk_ordenes_estado CHECK (estado IN ('RECEPCIONADO', 'ESPERANDO_PIEZA', 'PIEZA_RECIBIDA', 'EN_REPARACION', 'LISTO_PARA_ENTREGA', 'ENTREGADO'))
);

-- 8. TABLA: AUTORIZACIONES_OPERACION (Micro-Aprobaciones Imprevistas)
CREATE TABLE autorizaciones_operacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    taller_id BIGINT NOT NULL REFERENCES talleres(id) ON DELETE CASCADE,
    orden_trabajo_id BIGINT NOT NULL REFERENCES ordenes_trabajo(id) ON DELETE CASCADE,
    descripcion VARCHAR(255) NOT NULL,
    coste_extra NUMERIC(10, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_resolucion TIMESTAMPTZ,
    ip_consentimiento VARCHAR(45),
    user_agent_consentimiento TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_autorizaciones_coste_extra CHECK (coste_extra >= 0.00),
    CONSTRAINT chk_autorizaciones_estado CHECK (estado IN ('PENDIENTE', 'AUTORIZADO', 'RECHAZADO'))
);

-- ============================================================================
-- ÍNDICES DE RENDIMIENTO Y MULTI-TENANT (RN-01)
-- ============================================================================
CREATE INDEX idx_usuarios_taller ON usuarios(taller_id);
CREATE INDEX idx_puestos_taller ON puestos(taller_id);
CREATE INDEX idx_citas_taller ON citas(taller_id);
CREATE INDEX idx_citas_vehiculo ON citas(vehiculo_id);
CREATE INDEX idx_citas_puesto ON citas(puesto_id);
CREATE INDEX idx_citas_token ON citas(token_uuid);

CREATE INDEX idx_ordenes_taller ON ordenes_trabajo(taller_id);
CREATE INDEX idx_ordenes_vehiculo ON ordenes_trabajo(vehiculo_id);
CREATE INDEX idx_ordenes_cita ON ordenes_trabajo(cita_id);
CREATE INDEX idx_ordenes_token ON ordenes_trabajo(token_uuid);

CREATE INDEX idx_autorizaciones_taller ON autorizaciones_operacion(taller_id);
CREATE INDEX idx_autorizaciones_orden ON autorizaciones_operacion(orden_trabajo_id);
CREATE INDEX idx_autorizaciones_token ON autorizaciones_operacion(token_uuid);