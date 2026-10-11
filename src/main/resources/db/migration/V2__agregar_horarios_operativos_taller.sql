-- ============================================================================
-- V2__agregar_horarios_operativos_taller.sql
-- SaaS Talleres Mecánicos - Horarios Operativos Estructurados por Taller
-- ============================================================================

-- 1. AÑADIR COLUMNAS DE HORARIO OPERATIVO (MAÑANA Y TARDE)
-- Se definen valores por defecto para mantener retrocompatibilidad con registros existentes.
ALTER TABLE talleres
    ADD COLUMN hora_apertura_manana TIME NOT NULL DEFAULT '08:30:00',
    ADD COLUMN hora_cierre_manana   TIME NOT NULL DEFAULT '13:30:00',
    ADD COLUMN hora_apertura_tarde  TIME NOT NULL DEFAULT '15:30:00',
    ADD COLUMN hora_cierre_tarde    TIME NOT NULL DEFAULT '19:30:00';

-- 2. RESTRICCIONES DE INTEGRIDAD Y COHERENCIA HORARIA
-- chk_taller_horario_manana: la hora de cierre de mañana debe ser posterior a la de apertura.
ALTER TABLE talleres
    ADD CONSTRAINT chk_taller_horario_manana 
    CHECK (hora_cierre_manana > hora_apertura_manana);

-- chk_taller_horario_tarde: la hora de cierre de tarde debe ser posterior a la de apertura.
ALTER TABLE talleres
    ADD CONSTRAINT chk_taller_horario_tarde 
    CHECK (hora_cierre_tarde > hora_apertura_tarde);

-- chk_taller_intermedio: el turno de tarde no puede comenzar antes de que finalice el de mañana.
ALTER TABLE talleres
    ADD CONSTRAINT chk_taller_intermedio 
    CHECK (hora_apertura_tarde >= hora_cierre_manana);