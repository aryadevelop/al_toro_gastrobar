-- =====================================================
-- SCRIPT DE LIMPIEZA DE NOTIFICACIONES PARA TESTS
--
-- Ejecutar antes de correr la colección de tests de Postman
-- para garantizar un estado limpio.
--
-- Uso: psql -U postgres -d altoro_db -f cleanup-notificaciones.sql
-- =====================================================

SET search_path TO restaurante, public;

-- Marcar todas las notificaciones ATENCION como ATENDIDA
-- para que los tests puedan crear nuevas solicitudes
UPDATE notificacion
SET notificacion_estado = 'ATENDIDA'
WHERE notificacion_tipo = 'ATENCION'
  AND notificacion_estado = 'ACTIVA';

-- Mensaje de confirmación
SELECT
  'Limpieza completada: ' || COUNT(*) || ' notificaciones ATENCION marcadas como ATENDIDA' AS resultado
FROM notificacion
WHERE notificacion_tipo = 'ATENCION';
