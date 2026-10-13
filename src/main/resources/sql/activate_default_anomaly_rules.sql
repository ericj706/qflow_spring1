-- Run once on an existing database to activate the 13 default rules.
-- This does not insert rows or change thresholds.
UPDATE anomaly_rule
SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE (process_code, anomaly_type, sensor_name, condition_type) IN (
    ('원료 칭량', '대량 원료 칭량 오차', 'dispensing_error_pct', 'WEIGHT_GT10_PCT'),
    ('원료 칭량', '일반 원료 칭량 오차', 'dispensing_error_pct', 'WEIGHT_1_10_PCT'),
    ('원료 칭량', '미량 원료 칭량 오차', 'dispensing_error_g', 'WEIGHT_LT1_G'),
    ('가열/혼합', '가열 유지 온도 이탈', 'tank_temp_c', 'HEAT_HOLD_RANGE'),
    ('가열/혼합', '가열 유지 교반속도 이탈', 'paddle_rpm', 'HEAT_HOLD_RANGE'),
    ('냉각', '기능성 원료 투입 온도 초과', 'tank_temp_c', 'BEFORE_HA_UPPER'),
    ('벌크 검사', '벌크 pH 이상', 'ph_measured', 'RANGE'),
    ('벌크 검사', '벌크 점도 이상', 'viscosity_measured', 'RANGE'),
    ('최종 포장 검사', '시트 중량 이상', 'sheet_dry_weight_g', 'RANGE'),
    ('최종 포장 검사', '파우치 자체중량 이상', 'pouch_tare_weight_g', 'RANGE'),
    ('최종 포장 검사', '에센스 순중량 이상', 'essence_net_weight_g', 'RANGE'),
    ('최종 포장 검사', '완제품 총중량 이상', 'gross_total_weight_g', 'RANGE'),
    ('최종 포장 검사', '실링온도 이상', 'seal_temp_c', 'SEAL_BOTH_RANGE')
);
