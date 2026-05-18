ALTER TABLE prestamos DROP CONSTRAINT IF EXISTS prestamos_estado_check;

ALTER TABLE prestamos ADD CONSTRAINT prestamos_estado_check 
CHECK (estado IN ('PENDIENTE', 'ACTIVO', 'DEVUELTO', 'VENCIDO'));