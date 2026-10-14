package springproject.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Measurement_result_Dto;
import springproject.model.entity.Anomaly_rule_Entity;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.entity.Material_dispensing_Entity;
import springproject.model.entity.Sensor_telemetry_Entity;
import springproject.model.repository.Anomaly_rule_Repository;
import springproject.model.repository.Bulk_qc_Repository;
import springproject.model.repository.Material_dispensing_Repository;
import springproject.model.repository.Sensor_telemetry_Repository;

@Service 
@RequiredArgsConstructor 
public class Measurement_check_Service {
    private final Material_dispensing_Repository material_dispensing_Repository;
    private final Anomaly_rule_Repository anomaly_rule_Repository;
    private final Bulk_qc_Repository bulk_qc_Repository;
    private final Sensor_telemetry_Repository sensor_telemetry_Repository;

    // 측정값을 규칙과 비교하여 정상 / 주의 / 심각 반환
    public String checkSeverity(Anomaly_rule_Entity anomalyRule, BigDecimal measuredValue){
        if(anomalyRule == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "적용할 이상 감지 규칙이 없습니다.");
        }
        if(!Boolean.TRUE.equals(anomalyRule.getIsActive())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"비활성화된 규칙으로는 판정할 수 없습니다.");
        }
        if(measuredValue == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"판정할 측정값이 없습니다");
        }

        String conditionType = anomalyRule.getConditionType();
        if(conditionType == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"규칙의 조건 유형이 없습니다");
        }

        // 수치 판정을 지원하는 조건 유형 확인
        switch (conditionType) {
            case "RANGE":
            case "WEIGHT_GT10_PCT":
            case "WEIGHT_1_10_PCT":
            case "WEIGHT_LT1_G":
            case "HEAT_HOLD_RANGE":
            case "BEFORE_HA_UPPER":
            case "SEAL_BOTH_RANGE":
                break;
            default:
                throw new ResponseStatusException(HttpStatus.CONFLICT,"지원하지 않는 수치 판정 조건입니다."+conditionType);
        }
        
        BigDecimal criticalMin = anomalyRule.getCriticalMin();
        BigDecimal criticalMax = anomalyRule.getCriticalMax();
        BigDecimal warningMin = anomalyRule.getWarningMin();
        BigDecimal warningMax = anomalyRule.getWarningMax();
        // 규칙의 상한·하한 설정이 올바른지 확인
        validateRuleRange(criticalMin,criticalMax,warningMin,warningMax);
        // 1. 합격범위를 벗어나면 심각
        if (isOutOfRange(measuredValue, criticalMin, criticalMax)) {return "심각";}
        // 2. 합격범위 안이지만 주의 기준을 벗어나면 주의
        if (isOutOfRange(measuredValue, warningMin, warningMax)) {return "주의";}
        // 3. 나머지는 정상
        return "정상";
    }

    // 범위 이탈 여부 확인
    // 하한 미만 또는 상한 초과이면 true
    // 경계값과 같으면 범위 안으로 처리
    // null인 경계는 비교하지 않음
    private boolean isOutOfRange(BigDecimal measuredValue, BigDecimal lowerLimit, BigDecimal upperLimit){
        if(lowerLimit != null && measuredValue.compareTo(lowerLimit) < 0){return true;}
        if(upperLimit != null && measuredValue.compareTo(upperLimit) > 0){return true;}
        return false;
    }

    // 규칙의 상한/하한 설정 검증
    // 규칙의 상한·하한 설정 검증
    private void validateRuleRange(BigDecimal criticalMin,BigDecimal criticalMax,BigDecimal warningMin,BigDecimal warningMax) {
        // 합격범위는 적어도 한쪽 경계가 있어야 함
        if (criticalMin == null && criticalMax == null) {
            throw createRuleError("심각 하한 또는 상한이 필요합니다.");
        }
        // 심각 하한이 심각 상한보다 큰 경우
        if (criticalMin != null && criticalMax != null && criticalMin.compareTo(criticalMax) > 0) {
            throw createRuleError("심각 하한은 상한보다 클 수 없습니다.");
        }

        // 주의 하한이 주의 상한보다 큰 경우
        // 기존 criticalMax 비교를 warningMax로 수정
        if (warningMin != null && warningMax != null && warningMin.compareTo(warningMax) > 0) {
            throw createRuleError("주의 하한은 상한보다 클 수 없습니다.");
        }
        // 주의 하한은 합격범위 안에 있어야 함
        if (warningMin != null && isOutOfRange(warningMin,criticalMin,criticalMax)) {
            throw createRuleError("주의 하한은 합격범위 안에 있어야 합니다.");
        }
        // 주의 상한은 합격범위 안에 있어야 함
        if (warningMax != null && isOutOfRange(warningMax,criticalMin,criticalMax)) {
            throw createRuleError("주의 상한은 합격범위 안에 있어야 합니다.");
        }
    }

    // 규칙 설정 오류 생성
    private ResponseStatusException createRuleError(String message){
        return new ResponseStatusException(HttpStatus.CONFLICT,"이상 감지 규칙 설정 오류: "+message);
    }

    // 저장된 원료 칭량 기족 한 건을 판정  1. 원료칭량
    @Transactional (readOnly = true)
    public Measurement_result_Dto checkMaterial(String dispenseId){
        // 1. 칭량번호 확인
        if(dispenseId == null || dispenseId.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "칭량번호를 입력해 주세요.");
        }
        // 2. 저장된 칭량 기록 조회
        Material_dispensing_Entity material = material_dispensing_Repository.findById(dispenseId).orElse(null);
        if (material == null) {
           throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 칭량 기록이 없습니다.");
        }
        if ("삭제".equals(material.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"삭제된 칭량 기록은 판정할 수 없습니다.");
        }
        // 3. 목표량·실제량 확인
        BigDecimal targetQty = material.getTargetQtyKg();
        BigDecimal actualQty = material.getActualQtyKg();
        if (targetQty == null || targetQty.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"목표 투입량은 0보다 커야 합니다.");
        }
        if (actualQty == null || actualQty.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"실제 투입량은 0 이상이어야 합니다.");
        }
        // 4. 목표 무게에 맞는 규칙과 비교 단위 선택
        String conditionType; String sensorName; String unit; BigDecimal comparedValue;
        // 실제량 - 목표량
        // 부족하면 음수, 초과하면 양수
        BigDecimal difference = actualQty.subtract(targetQty);
        if (targetQty.compareTo(BigDecimal.ONE) < 0) {
            // 1kg 미만: 오차 무게(g)
            conditionType = "WEIGHT_LT1_G";
            sensorName = "dispensing_error_g";
            unit = "g";
            comparedValue = difference.multiply(new BigDecimal("1000"));
        } else {
            // 1kg 이상: 오차율(%)
            sensorName = "dispensing_error_pct"; unit = "%";
            if (targetQty.compareTo(new BigDecimal("10")) > 0) {
                conditionType = "WEIGHT_GT10_PCT";
            } else {
                conditionType = "WEIGHT_1_10_PCT";
            }
            // (실제량 - 목표량) / 목표량 × 100
            // 판정 전에 소수점 3자리로 줄이지 않도록 계산 정밀도 확보
            comparedValue = difference.multiply(new BigDecimal("100")).divide(targetQty, 12, RoundingMode.HALF_UP);
        }
        // 5. 해당 무게 구간의 활성 규칙 조회
        List<Anomaly_rule_Entity> rules = anomaly_rule_Repository.findByProcessCodeAndSensorNameAndConditionTypeAndIsActiveTrue("원료 칭량",sensorName,conditionType);
        if (rules.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"활성화된 원료 칭량 규칙이 없습니다: " + conditionType);
        }
        if (rules.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"같은 조건의 활성 규칙이 여러 개입니다: " + conditionType);
        }
        Anomaly_rule_Entity rule = rules.get(0);
        // 6. 기존 공통 메소드로 정상·주의·심각 판정
        String severity = checkSeverity(rule, comparedValue);
        // 7. 판정 결과 반환
        return Measurement_result_Dto.builder()
                .sourceTable("material_dispensing")
                .sourceRecordId(material.getDispenseId())
                .ruleId(rule.getRuleId())
                .processCode("원료 칭량")
                .sensorName(sensorName)
                .measuredValue(comparedValue)
                .unit(unit)
                .severity(severity)
                .build();
    }

    // 측정항목 한 개의 활성 규칙을 조회하고 판정 결과 생성
    private Measurement_result_Dto checkMeasurement(String sourceTable,
                                                    String sourceRecordId,
                                                    String processCode,
                                                    String sensorName,
                                                    String conditionType,
                                                    BigDecimal measuredValue,
                                                    String unit){
        if(measuredValue == null){throw new ResponseStatusException(HttpStatus.CONFLICT,"저장된 측정값이 없습니다: "+sensorName);}
        List<Anomaly_rule_Entity> rules = anomaly_rule_Repository.findByProcessCodeAndSensorNameAndConditionTypeAndIsActiveTrue(processCode, sensorName, conditionType);
        if(rules.isEmpty()){throw new ResponseStatusException(HttpStatus.CONFLICT,"활성 규칙이 없습니다: "+processCode+"\\"+sensorName);}
        if (rules.size() > 1) {throw new ResponseStatusException(HttpStatus.CONFLICT,"같은 조건의 활성 규칙이 여러 개입니다: "+ processCode + " / " + sensorName);}        
        Anomaly_rule_Entity rule = rules.get(0);
        String severity = checkSeverity(rule, measuredValue);
        return Measurement_result_Dto.builder()
                .sourceTable(sourceTable)
                .sourceRecordId(sourceRecordId)
                .ruleId(rule.getRuleId())
                .processCode(processCode)
                .sensorName(sensorName)
                .measuredValue(measuredValue)
                .unit(unit)
                .severity(severity).build();                                       
    }

    // 저장된 벌크 검사 한 건의 pH·점도를 각각 판정 4. 벌크검사
    @Transactional(readOnly = true)
    public List<Measurement_result_Dto> checkBulkQc(String qcId) {
        // 1. 검사번호 확인
        if (qcId == null || qcId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"검사번호를 입력해 주세요.");
        }
        // 2. 검사 기록 조회
        Bulk_qc_Entity bulkQc =bulk_qc_Repository.findById(qcId).orElse(null);
        if (bulkQc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 벌크 검사 기록이 없습니다.");
        }
        // 3. pH 판정
        Measurement_result_Dto phResult = checkMeasurement(
                "bulk_qc",bulkQc.getQc_id(),"벌크 검사","ph_measured","RANGE",bulkQc.getPh_measured(),"pH");
        // 4. 점도 판정
        Measurement_result_Dto viscosityResult = checkMeasurement(
                "bulk_qc",bulkQc.getQc_id(),"벌크 검사","viscosity_measured","RANGE",bulkQc.getViscosity_measured(),"cP");
        // 5. 항목별 판정 결과 반환
        return List.of(phResult, viscosityResult);
    }

    // 공정명 + tank_temp_c 온도 규칙을 선택하고 판정  2.가열/혼합 , 3. 냉각
    private Measurement_result_Dto checkTankTemperature(Sensor_telemetry_Entity sensor, String processCode){
        List<Anomaly_rule_Entity> rules = anomaly_rule_Repository.findByProcessCodeAndSensorNameAndIsActiveTrue(processCode, "tank_temp_c");
        if(rules.isEmpty()){throw new ResponseStatusException(HttpStatus.CONFLICT,processCode + " 공정의 활성 온도 규칙이 없습니다.");}
        if (rules.size() > 1) {throw new ResponseStatusException(HttpStatus.CONFLICT,processCode + " 공정의 활성 온도 규칙은 한 개여야 합니다.");}
        Anomaly_rule_Entity rule = rules.get(0); // 규칙 꺼내기 조건들을 통과하면 조회되는 조건은 결국하나만 조회
        // 이번 방식은 단계별 규칙이 아닌 일반 수치 범위 규칙 사용
        if(!"RANGE".equals(rule.getConditionType())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"공정 종료 온도 기준을 확인하고 조건 유형을 RANGE로 설정해 주세요.");}
        BigDecimal temperature = sensor.getTank_temp_c();
        if (temperature == null) {throw new ResponseStatusException(HttpStatus.CONFLICT,"센서 기록에 탱크 온도가 없습니다.");}
        String severity = checkSeverity(rule, temperature);
        return Measurement_result_Dto.builder()
            .sourceTable("sensor_telemetry")
            .sourceRecordId(sensor.getSensor_id().toString())
            .ruleId(rule.getRuleId())
            .processCode(processCode)
            .sensorName("tank_temp_c")
            .measuredValue(temperature)
            .unit("℃")
            .severity(severity)
            .build();
    }

    // 센서 기록 조회 및 연결된 공정 확인
    private Sensor_telemetry_Entity findSensor(Long sensorId,String processCode) {
        if (sensorId == null || sensorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 센서 측정번호를 입력해 주세요.");
        }
        Sensor_telemetry_Entity sensor = sensor_telemetry_Repository.findById(sensorId).orElse(null);
        if (sensor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 센서 측정 기록이 없습니다.");
        }
        if (sensor.getProcess_ExecutionEntity() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"센서 기록에 공정 실행이 연결되어 있지 않습니다.");
        }
        String savedProcessCode = sensor.getProcess_ExecutionEntity().getProcess_code();
        if (!processCode.equals(savedProcessCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,processCode + " 공정의 센서 기록이 아닙니다.");
        }
        return sensor;
    }

    // 2공정: 가열/혼합 온도·패들 RPM 판정
    @Transactional(readOnly = true)
    public List<Measurement_result_Dto> checkHeatingMixing(Long sensorId) {
        Sensor_telemetry_Entity sensor = findSensor(sensorId, "가열/혼합");
        // 탱크 온도 판정
        Measurement_result_Dto temperatureResult = checkTankTemperature(sensor, "가열/혼합");
        // 패들 RPM 판정
        Measurement_result_Dto rpmResult = checkMeasurement(
                                                    "sensor_telemetry",
                                                    sensor.getSensor_id().toString(),
                                                    "가열/혼합",
                                                    "paddle_rpm",
                                                    "RANGE",
                                                    sensor.getPaddle_rpm(),
                                                    "rpm");
        return List.of(temperatureResult, rpmResult);
    }

    // 3공정: 냉각 온도 판정
    @Transactional(readOnly = true)
    public List<Measurement_result_Dto> checkCooling(Long sensorId) {
        Sensor_telemetry_Entity sensor = findSensor(sensorId, "냉각");
        Measurement_result_Dto temperatureResult = checkTankTemperature(sensor, "냉각");
        return List.of(temperatureResult);
    }



    
}
