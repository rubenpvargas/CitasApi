-- Repeatable-friendly fixed identifiers make the clean-container demo stable.
INSERT INTO users(id,first_name,last_name,document_type,document_number,email,phone,password_hash,active,created_at,updated_at) VALUES
 (9001,'Admin','Demo','CC','990000001','admin@demo.invalid','3000000001','$2y$10$QAPT/bPvvEILB0ovqykfTuwSBznwY2p0rJhZJguneKjk2dr7VQFeG',TRUE,NOW(6),NOW(6)),
 (9002,'Valentina','Herrera','CC','990000002','profesional@demo.invalid','3000000002','$2y$10$QAPT/bPvvEILB0ovqykfTuwSBznwY2p0rJhZJguneKjk2dr7VQFeG',TRUE,NOW(6),NOW(6)),
 (9003,'Ana','Martinez','CC','990000003','paciente@demo.invalid','3000000003','$2y$10$QAPT/bPvvEILB0ovqykfTuwSBznwY2p0rJhZJguneKjk2dr7VQFeG',TRUE,NOW(6),NOW(6))
ON DUPLICATE KEY UPDATE active=TRUE;
INSERT IGNORE INTO user_roles(user_id,role_id) SELECT 9001,id FROM roles WHERE code='ADMIN';
INSERT IGNORE INTO user_roles(user_id,role_id) SELECT 9002,id FROM roles WHERE code='PROFESSIONAL';
INSERT IGNORE INTO user_roles(user_id,role_id) SELECT 9003,id FROM roles WHERE code='USER';
INSERT INTO eps_plans(eps_id,regime_id,code,name,active) SELECT e.id,r.id,'DEMO-CONTRIB','Plan Contributivo Demo',TRUE FROM eps e JOIN insurance_regimes r ON r.code='CONTRIBUTIVO' WHERE e.code='EPS_DEMO_A'
ON DUPLICATE KEY UPDATE active=TRUE;
INSERT IGNORE INTO professionals(id,user_id,professional_code,license_number,active,created_at,updated_at)
VALUES(9001,9002,'PROF-DEMO-001','RM-DEMO-0001',TRUE,NOW(6),NOW(6));
INSERT IGNORE INTO professional_specialties(professional_id,specialty_id,is_primary,active)
SELECT 9001,id,TRUE,TRUE FROM specialties WHERE code='MEDICINA_GENERAL';
INSERT IGNORE INTO professional_specialties(professional_id,specialty_id,is_primary,active)
SELECT 9001,id,FALSE,TRUE FROM specialties WHERE code='CARDIOLOGIA_ADULTO';
INSERT IGNORE INTO professional_locations(professional_id,location_id,active)
SELECT 9001,id,TRUE FROM locations WHERE code='HIC';
INSERT IGNORE INTO availability_blocks(id,professional_id,location_id,available_date,start_time,end_time,active,created_at,updated_at)
SELECT 9001,9001,l.id,DATE_ADD(CURRENT_DATE,INTERVAL 1 DAY),'08:00:00','12:00:00',TRUE,NOW(6),NOW(6) FROM locations l WHERE l.code='HIC';
INSERT IGNORE INTO availability_blocks(id,professional_id,location_id,available_date,start_time,end_time,active,created_at,updated_at)
SELECT 9002,9001,l.id,DATE_ADD(CURRENT_DATE,INTERVAL 2 DAY),'14:00:00','17:00:00',TRUE,NOW(6),NOW(6) FROM locations l WHERE l.code='HIC';
INSERT IGNORE INTO professional_slots(availability_block_id,start_at,end_at)
SELECT b.id,TIMESTAMP(b.available_date,ADDTIME(b.start_time,SEC_TO_TIME(s.n*1800))),TIMESTAMP(b.available_date,ADDTIME(b.start_time,SEC_TO_TIME((s.n+1)*1800)))
FROM availability_blocks b JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11) s
WHERE b.id IN (9001,9002) AND ADDTIME(b.start_time,SEC_TO_TIME((s.n+1)*1800)) <= b.end_time;
