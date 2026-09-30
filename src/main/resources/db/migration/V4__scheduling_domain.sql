-- Normalized scheduling model. Every relationship is represented by a FK or
-- bridge table; reservations are serialized by locking the affected slots.
CREATE TABLE IF NOT EXISTS eps (
    id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_eps_code (code)
);
CREATE TABLE IF NOT EXISTS eps_plans (
    id BIGINT NOT NULL AUTO_INCREMENT, eps_id BIGINT NOT NULL, regime_id SMALLINT UNSIGNED NOT NULL,
    code VARCHAR(50) NOT NULL, name VARCHAR(160) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id), UNIQUE KEY uk_eps_plans_code (eps_id, code),
    CONSTRAINT fk_eps_plans_eps FOREIGN KEY (eps_id) REFERENCES eps(id),
    CONSTRAINT fk_eps_plans_regime FOREIGN KEY (regime_id) REFERENCES insurance_regimes(id)
);
CREATE TABLE IF NOT EXISTS user_insurance_affiliations (
    id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, plan_id BIGINT NOT NULL,
    membership_number VARCHAR(80) NOT NULL, is_current BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE NULL, valid_to DATE NULL, created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_user_affiliation (user_id, plan_id, membership_number),
    INDEX ix_user_affiliation_current (user_id, is_current),
    CONSTRAINT fk_affiliation_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_affiliation_plan FOREIGN KEY (plan_id) REFERENCES eps_plans(id)
);
CREATE TABLE IF NOT EXISTS specialties (
    id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(60) NOT NULL, name VARCHAR(160) NOT NULL,
    appointment_duration_minutes SMALLINT UNSIGNED NOT NULL, is_general BOOLEAN NOT NULL DEFAULT FALSE,
    requires_admin_approval BOOLEAN NOT NULL DEFAULT TRUE, active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id), UNIQUE KEY uk_specialties_code (code), UNIQUE KEY uk_specialties_name (name),
    CONSTRAINT ck_specialty_duration CHECK (appointment_duration_minutes IN (30, 60))
);
CREATE TABLE IF NOT EXISTS professionals (
    id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, professional_code VARCHAR(50) NOT NULL,
    license_number VARCHAR(80) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_professional_user (user_id), UNIQUE KEY uk_professional_code (professional_code),
    UNIQUE KEY uk_professional_license (license_number),
    CONSTRAINT fk_professional_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS professional_specialties (
    professional_id BIGINT NOT NULL, specialty_id BIGINT NOT NULL, is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE, PRIMARY KEY (professional_id, specialty_id),
    CONSTRAINT fk_prof_specialty_professional FOREIGN KEY (professional_id) REFERENCES professionals(id),
    CONSTRAINT fk_prof_specialty_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
);
CREATE TABLE IF NOT EXISTS professional_locations (
    professional_id BIGINT NOT NULL, location_id SMALLINT UNSIGNED NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE, PRIMARY KEY (professional_id, location_id),
    CONSTRAINT fk_prof_location_professional FOREIGN KEY (professional_id) REFERENCES professionals(id),
    CONSTRAINT fk_prof_location_location FOREIGN KEY (location_id) REFERENCES locations(id)
);
CREATE TABLE IF NOT EXISTS availability_blocks (
    id BIGINT NOT NULL AUTO_INCREMENT, professional_id BIGINT NOT NULL, location_id SMALLINT UNSIGNED NOT NULL,
    available_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id), INDEX ix_blocks_professional_date (professional_id, available_date, start_time),
    CONSTRAINT ck_block_time CHECK (end_time > start_time),
    CONSTRAINT fk_block_professional FOREIGN KEY (professional_id) REFERENCES professionals(id),
    CONSTRAINT fk_block_location FOREIGN KEY (location_id) REFERENCES locations(id)
);
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_user_id BIGINT NOT NULL, professional_id BIGINT NOT NULL,
    location_id SMALLINT UNSIGNED NOT NULL, specialty_id BIGINT NOT NULL, insurance_affiliation_id BIGINT NULL,
    status_id SMALLINT UNSIGNED NOT NULL, reason VARCHAR(500) NULL,
    scheduled_start_at DATETIME(6) NOT NULL, scheduled_end_at DATETIME(6) NOT NULL,
    created_by_user_id BIGINT NOT NULL, approved_by_user_id BIGINT NULL, approved_at DATETIME(6) NULL,
    created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id), INDEX ix_appointments_patient_date (patient_user_id, scheduled_start_at),
    INDEX ix_appointments_professional_date (professional_id, scheduled_start_at), INDEX ix_appointments_status (status_id),
    CONSTRAINT ck_appointment_time CHECK (scheduled_end_at > scheduled_start_at),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_user_id) REFERENCES users(id),
    CONSTRAINT fk_appointment_professional FOREIGN KEY (professional_id) REFERENCES professionals(id),
    CONSTRAINT fk_appointment_location FOREIGN KEY (location_id) REFERENCES locations(id),
    CONSTRAINT fk_appointment_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id),
    CONSTRAINT fk_appointment_affiliation FOREIGN KEY (insurance_affiliation_id) REFERENCES user_insurance_affiliations(id),
    CONSTRAINT fk_appointment_status FOREIGN KEY (status_id) REFERENCES appointment_statuses(id),
    CONSTRAINT fk_appointment_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_appointment_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS professional_slots (
    id BIGINT NOT NULL AUTO_INCREMENT, availability_block_id BIGINT NOT NULL,
    start_at DATETIME(6) NOT NULL, end_at DATETIME(6) NOT NULL, appointment_id BIGINT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_slot_block_start (availability_block_id, start_at), INDEX ix_slot_start (start_at),
    CONSTRAINT fk_slot_block FOREIGN KEY (availability_block_id) REFERENCES availability_blocks(id),
    CONSTRAINT fk_slot_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);
CREATE TABLE IF NOT EXISTS appointment_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT, appointment_id BIGINT NOT NULL, status_id SMALLINT UNSIGNED NOT NULL,
    changed_by_user_id BIGINT NULL, change_source VARCHAR(20) NOT NULL, reason VARCHAR(500) NULL,
    changed_at TIMESTAMP(6) NOT NULL, PRIMARY KEY (id), INDEX ix_status_history_appointment (appointment_id, changed_at),
    CONSTRAINT ck_history_source CHECK (change_source IN ('SYSTEM', 'USER', 'ADMIN')),
    CONSTRAINT fk_history_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_history_status FOREIGN KEY (status_id) REFERENCES appointment_statuses(id),
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS reschedule_requests (
    id BIGINT NOT NULL AUTO_INCREMENT, appointment_id BIGINT NOT NULL, requested_by_user_id BIGINT NOT NULL,
    requested_location_id SMALLINT UNSIGNED NOT NULL, status_id SMALLINT UNSIGNED NOT NULL,
    previous_start_at DATETIME(6) NOT NULL, previous_end_at DATETIME(6) NOT NULL,
    requested_start_at DATETIME(6) NOT NULL, requested_end_at DATETIME(6) NOT NULL,
    decision_reason VARCHAR(500) NULL, decided_by_user_id BIGINT NULL, decided_at DATETIME(6) NULL,
    created_at TIMESTAMP(6) NOT NULL, PRIMARY KEY (id), INDEX ix_reschedule_status (status_id), INDEX ix_reschedule_appointment (appointment_id),
    CONSTRAINT fk_reschedule_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_reschedule_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_reschedule_location FOREIGN KEY (requested_location_id) REFERENCES locations(id),
    CONSTRAINT fk_reschedule_status FOREIGN KEY (status_id) REFERENCES reschedule_request_statuses(id),
    CONSTRAINT fk_reschedule_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES users(id)
);
-- Synthetic catalogs only; no real patient or professional data is stored.
INSERT INTO eps(code,name,active,created_at,updated_at) VALUES
 ('EPS_DEMO_A','EPS Demo Salud',TRUE,NOW(6),NOW(6)), ('EPS_DEMO_B','EPS Demo Familiar',TRUE,NOW(6),NOW(6))
ON DUPLICATE KEY UPDATE name=VALUES(name), active=VALUES(active);
INSERT INTO specialties(code,name,appointment_duration_minutes,is_general,requires_admin_approval,active) VALUES
 ('MEDICINA_GENERAL','Medicina General',30,TRUE,FALSE,TRUE),
 ('CARDIOLOGIA_ADULTO','Cardiologia Adulto',30,FALSE,TRUE,TRUE),
 ('ORTOPEDIA_TRAUMATOLOGIA','Ortopedia y Traumatologia',60,FALSE,TRUE,TRUE),
 ('MEDICINA_INTERNA','Medicina Interna',30,FALSE,TRUE,TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name), appointment_duration_minutes=VALUES(appointment_duration_minutes), active=VALUES(active);
