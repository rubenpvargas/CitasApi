-- Fixed catalogs are normalized reference tables. Transactional tables refer to
-- their stable numeric keys; API consumers receive the immutable codes.
INSERT INTO roles (code, name, description) VALUES
    ('USER', 'Usuario', 'Usuario final del laboratorio'),
    ('PROFESSIONAL', 'Profesional', 'Profesional ficticio del laboratorio'),
    ('ADMIN', 'Administrador', 'Administrador del laboratorio')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);

CREATE TABLE IF NOT EXISTS appointment_statuses (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(80) NOT NULL,
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_appointment_statuses_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS reschedule_request_statuses (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(80) NOT NULL,
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_reschedule_request_statuses_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS insurance_regimes (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(80) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_insurance_regimes_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS locations (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(180) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_locations_code UNIQUE (code)
);

INSERT INTO appointment_statuses (code, name, is_terminal) VALUES
    ('REQUESTED', 'Solicitada / pendiente de aprobacion', FALSE),
    ('APPROVED', 'Aprobada', FALSE),
    ('REJECTED', 'Rechazada', TRUE),
    ('CANCELLED', 'Cancelada', TRUE),
    ('COMPLETED', 'Atendida / completada', TRUE),
    ('NO_SHOW', 'No asistio', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_terminal = VALUES(is_terminal);

INSERT INTO reschedule_request_statuses (code, name, is_terminal) VALUES
    ('PENDING', 'Pendiente', FALSE),
    ('APPROVED', 'Aprobada', TRUE),
    ('REJECTED', 'Rechazada', TRUE),
    ('CANCELLED', 'Cancelada por el usuario', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_terminal = VALUES(is_terminal);

INSERT INTO insurance_regimes (code, name) VALUES
    ('CONTRIBUTIVO', 'Contributivo'),
    ('SUBSIDIADO', 'Subsidiado'),
    ('ESPECIAL', 'Especial'),
    ('EXCEPCION', 'Excepcion'),
    ('PARTICULAR', 'Particular')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO locations (code, name, address, city, department, active) VALUES
    ('HIC', 'Hospital Internacional de Colombia (HIC)', 'Km 7 Autopista Bucaramanga - Piedecuesta, Valle de Menzuli', 'Piedecuesta', 'Santander', TRUE),
    ('ICV', 'Fundacion Cardiovascular de Colombia - Instituto Cardiovascular (ICV)', 'Calle 155A No. 23-58, Urbanizacion El Bosque', 'Floridablanca', 'Santander', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), address = VALUES(address), city = VALUES(city),
    department = VALUES(department), active = VALUES(active);
