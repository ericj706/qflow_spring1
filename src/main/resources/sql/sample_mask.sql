INSERT INTO users (user_id, password_hash, user_name, role, is_active, created_at, updated_at) VALUES
(1, 'hash_admin', 'Admin User', 'ADMIN', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(2, 'hash_OP_PARK_01', 'Park Operator 1', 'OPERATOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(3, 'hash_OP_KIM_03', 'Kim Operator 3', 'OPERATOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(4, 'hash_OP_LEE_07', 'Lee Operator 7', 'OPERATOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(5, 'hash_OP_CHOI_02', 'Choi Operator 2', 'OPERATOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(6, 'hash_OP_JUNG_05', 'Jung Operator 5', 'OPERATOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(7, 'hash_OP_KANG_09', 'Kang Operator 9', 'OPERATOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(8, 'hash_QC_INSP_KIM', 'Kim QC Inspector', 'QC_INSPECTOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(9, 'hash_QC_INSP_PARK', 'Park QC Inspector', 'QC_INSPECTOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
(10, 'hash_QC_INSP_LEE', 'Lee QC Inspector', 'QC_INSPECTOR', TRUE, '2023-01-01 00:00:00', '2023-01-01 00:00:00');

INSERT INTO batches (
    batch_id, product_code, product_name, target_bulk_kg, actual_bulk_kg, 
    target_units, actual_units, defect_units, start_time, end_time, 
    status, user_id, tank_id, record_source
) VALUES
('LOT20230105-001', 'PRD-MP-HYA05', 'HYA05_SOOTHING_MASK', 500.0000, 501.5400, 3000, 2981, 19, '2023-01-05 08:30:00', '2023-01-05 13:08:30', 'BATCH_STATUS_COMPLETED', 1, 'TANK_MT_A01', 'CSV_IMPORT'),
('LOT20230105-002', 'PRD-MP-HYA05', 'HYA05_SOOTHING_MASK', 500.0000, 499.8000, 3000, 2975, 25, '2023-01-05 14:00:00', '2023-01-05 18:45:00', 'BATCH_STATUS_COMPLETED', 2, 'TANK_MT_A01', 'CSV_IMPORT'),
('LOT20230109-002', 'PRD-MP-HYA05', 'HYA05_SOOTHING_MASK', 500.0000, 502.1000, 3000, 2920, 80, '2023-01-09 06:00:00', '2023-01-09 11:15:00', 'BATCH_STATUS_COMPLETED', 3, 'TANK_MT_A02', 'CSV_IMPORT');

INSERT INTO material_dispensing (
    dispense_id, batch_id, material_code, material_name, raw_material_lot, 
    target_qty_kg, actual_qty_kg, user_id, dispensed_at, status
) VALUES
('DSP_LOT20230105-001_RM_WATER', 'LOT20230105-001', 'RM_WATER', '정제수', 'RM-LOT-20220610-01', 390.7250, 390.8495, 1, '2023-01-05 07:22:00', 'STATUS_DISPENSED_VERIFIED'),
('DSP_LOT20230105-001_RM_GLYC', 'LOT20230105-001', 'RM_GLYC', '글리세린', 'RM-LOT-20220815-03', 25.0000, 25.0120, 1, '2023-01-05 07:35:00', 'STATUS_DISPENSED_VERIFIED'),
('DSP_LOT20230105-001_RM_HA_MED', 'LOT20230105-001', 'RM_HA_MED', '중분자 히알루론산', 'RM-LOT-20220901-01', 1.5000, 1.4980, 1, '2023-01-05 07:48:00', 'STATUS_DISPENSED_VERIFIED');

INSERT INTO process_execution (
    batch_id, process_code, start_time, end_time, duration_min, status, record_source
) VALUES
('LOT20230105-001', 'OP_S01_SOLUBILIZE', '2023-01-05 08:30:00', '2023-01-05 09:02:00', 32.00, 'STEP_STATUS_COMPLETED', 'CSV_IMPORT'),
('LOT20230105-001', 'OP_S02_HEATING_MIX', '2023-01-05 09:05:00', '2023-01-05 10:15:00', 70.00, 'STEP_STATUS_COMPLETED', 'CSV_IMPORT'),
('LOT20230105-001', 'OP_S03_COOLING_FINISH', '2023-01-05 10:20:00', '2023-01-05 11:20:00', 60.00, 'STEP_STATUS_COMPLETED', 'CSV_IMPORT');

INSERT INTO sensor_telemetry (
    execution_id, `timestamp`, tank_temp_c, paddle_rpm, homomixer_rpm, 
    bulk_viscosity_cps, ph_level, motor_torque_pct, vacuum_kpa, cooling_valve_pct, 
    record_source, user_id
) VALUES
(1, '2023-01-05 08:30:00.000', 23.650, 36.00, 0.00, 18.000, 4.176, 13.50, 0.000, 0.00, 'CSV_IMPORT', 1),
(1, '2023-01-05 08:31:00.000', 25.100, 36.00, 0.00, 22.000, 4.210, 14.10, 0.000, 0.00, 'CSV_IMPORT', 1),
(2, '2023-01-05 09:05:00.000', 62.500, 40.00, 2500.00, 1200.000, 5.850, 45.20, -60.500, 0.00, 'CSV_IMPORT', 1);

INSERT INTO bulk_qc (
    qc_id, batch_id, sample_time, user_id, ph_measured, ph_criteria, 
    viscosity_measured, viscosity_criteria, specific_gravity, sg_criteria, 
    appearance_code, microbubble_code, microbial_cfu, overall_qc_result, 
    qc_notes_code, record_source
) VALUES
('QC_LOT20230105-001', 'LOT20230105-001', '2023-01-05 11:26:00', 7, 6.050, '5.50_6.50', 2498.000, '1500_3500', 1.0167, '1.000_1.030', 'APP_PASS_PALEBLUE', 'BUBBLE_PASS_ZERO', 0, 'QC_RESULT_PASS', 'NOTE_QC_OK_TRANSFERRED', 'CSV_IMPORT'),
('QC_LOT20230105-002', 'LOT20230105-002', '2023-01-05 16:50:00', 8, 6.120, '5.50_6.50', 2530.000, '1500_3500', 1.0172, '1.000_1.030', 'APP_PASS_PALEBLUE', 'BUBBLE_PASS_ZERO', 0, 'QC_RESULT_PASS', 'NOTE_QC_OK_TRANSFERRED', 'CSV_IMPORT');

INSERT INTO filling_packaging (
    pouch_id, batch_id, user_id, packaging_line, `timestamp`, 
    sheet_material_code, sheet_lot_no, sheet_dry_weight_g, 
    fill_weight_1st_g, fill_weight_2nd_g, essence_net_weight_g, 
    pouch_tare_weight_g, gross_total_weight_g, upper_seal_temp_c, 
    lower_seal_temp_c, seal_pressure_bar, n2_residual_o2_pct, 
    checkweigher_status, metal_detector_status, vision_inspection_status, 
    final_disposition, record_source
) VALUES
('PKG_LOT20230105-001_000001', 'LOT20230105-001', 1, 'LINE_PKG_01', '2023-01-05 12:21:00.000', 'SHT_VEGAN_TENCEL', 'SHT-LOT-202301-01', 3.070, 12.950, 13.130, 26.080, 6.000, 35.150, 182.900, 182.800, 4.870, 1.300, 'CW_PASS', 'MD_NORMAL', 'VIS_NORMAL', 'DISP_ACCEPTED', 'CSV_IMPORT'),
('PKG_LOT20230109-002_002845', 'LOT20230109-002', 3, 'LINE_PKG_02', '2023-01-09 06:22:01.000', 'SHT_VEGAN_TENCEL', 'SHT-LOT-202301-02', 3.050, 12.800, 12.900, 25.700, 6.000, 34.750, 181.500, 181.000, 4.800, 1.400, 'CW_PASS', 'MD_REJECT', 'VIS_NORMAL', 'DISP_REJECTED', 'CSV_IMPORT');

INSERT INTO anomaly_rule (
    process_code, anomaly_type, sensor_name, condition_type, 
    warning_min, warning_max, critical_min, critical_max, 
    status_value, duration_seconds, check_items, response_description, is_active
) VALUES
('OP_S02_HEATING_MIX', 'TEMP_OVERHEAT', 'tank_temp_c', 'RANGE', 75.000, 80.000, 80.000, 95.000, NULL, 30, '탱크 온도 센서, 냉각 밸브 작동 상태', '가열 히터 차단 및 냉각수 수동 유입 실행', true),
('LINE_PKG_02', 'METAL_DETECTED', 'metal_detector_status', 'EQUAL', NULL, NULL, NULL, NULL, 'MD_REJECT', 0, '포장라인 금속검출기 이송 라인', '금속 이물질 발견에 따른 해당 파우치 자동 리젝트 및 인터록 발생', true);

INSERT INTO anomaly_event (
    batch_id, pouch_id, rule_id, source_alarm_id, process_code, 
    anomaly_type, sensor_name, measured_value, severity, alarm_message, 
    occurred_at, resolved_at, duration_sec, action_status, action_note, 
    user_id, action_time, source_type
) VALUES
('LOT20230109-002', 'PKG_LOT20230109-002_002845', 2, 'ALM_00001', 'LINE_PKG_02', 'ERR_METAL_DETECTED', 'metal_detector_status', 1.000, 'ALM_SEV_CRIT', 'METAL CONTAMINATION DETECTED ON POUCH (PKG_LOT20230109-002_002845) - INTERLOCK REJECTED', '2023-01-09 06:22:01.498', '2023-01-09 06:22:13.498', 12, 'RESOLVED', '이물질 검출 파우치 폐기 조치 및 라인 정상화 완료', 3, '2023-01-09 06:25:00', 'ALARM_CSV');

INSERT INTO data_change_log (
    table_name, record_id, column_name, old_value, new_value, 
    change_type, user_id, change_reason, changed_at
) VALUES
('batches', 'LOT20230105-001', 'actual_bulk_kg', '500.0000', '501.5400', 'UPDATE', 1, '생산 완료 후 실측 벌크 중량 최종 반영', '2023-01-05 13:10:00'),
('bulk_qc', 'QC_LOT20230105-001', 'overall_qc_result', 'PENDING', 'QC_RESULT_PASS', 'UPDATE', 7, '벌크 미생물 시험 적합 판정 완료', '2023-01-05 11:30:00');

LOAD DATA LOCAL INFILE 'filling_packaging.csv'
INTO TABLE filling_packaging
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(pouch_id, batch_id, packaging_line, @v_timestamp, sheet_material_code, sheet_lot_no, sheet_dry_weight_g, fill_weight_1st_g, fill_weight_2nd_g, essence_net_weight_g, pouch_tare_weight_g, gross_total_weight_g, upper_seal_temp_c, lower_seal_temp_c, seal_pressure_bar, n2_residual_o2_pct, checkweigher_status, metal_detector_status, vision_inspection_status, final_disposition)
SET 
    `timestamp` = STR_TO_DATE(@v_timestamp, '%Y-%m-%dT%H:%i:%s'),
    user_id = 1,
    record_source = 'CSV_BULK_LOAD';