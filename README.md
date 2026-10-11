# SaaS-Talleres-Backend

API REST multi-tenant en Java 21 y Spring Boot 4 para la plataforma SaaS de gestion operativa, agendamiento concurrente de elevadores y presupuestos digitales en talleres mecanicos.

## Estado tecnico

Sincronizado documentalmente con `HEAD` tras la implementacion del modulo Citas del 11 de octubre de 2026.

| Modulo | Estado |
| --- | --- |
| Autenticacion JWT | Implementado |
| Vehiculos | Implementado |
| Puestos | Implementado |
| Citas | Implementado |
| Ordenes de trabajo | Persistencia modelada; API pendiente |
| Autorizaciones | Persistencia modelada; API pendiente |
| Presupuestos y notificaciones | Proyectado |

## Documentacion

La documentacion tecnica principal se mantiene en `SaaS-Talleres-Backend.wiki/`:

- `Home.md`
- `01-Requisitos-y-Reglas-de-Negocio.md`
- `02-Arquitectura-del-Sistema-y-Decisiones.md`
- `03-Modelo-de-Datos-y-Persistencia.md`
- `04-Diseno-de-API-y-Contratos.md`
- `05-Modulos-Implementados-y-Calidad.md`

El registro de cambios se mantiene en `CHANGELOG.md`.
