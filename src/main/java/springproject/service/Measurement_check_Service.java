package springproject.service;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import springproject.model.entity.Anomaly_rule_Entity;

@Service 
public class Measurement_check_Service {
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
    private void validateRuleRange(BigDecimal criticalMin,BigDecimal criticalMax,BigDecimal warningMin,BigDecimal warningMax){
        // 합격범위는 적어도 한쪽 경계가 있어야 함
        if(criticalMin == null && criticalMax ==null){throw createRuleError("심각 하한 또는 상한이 필요");}
        // 심각 하한이 상한보다 큰 경우
        if(criticalMin != null && criticalMax != null && criticalMin.compareTo(criticalMax) > 0){
            throw createRuleError("심각 하한은 상한보다 클 수 없습니다");
        }
        // 주의 하한이 상한보다 큰 경우
        if(warningMin != null &&warningMax != null && warningMin.compareTo(criticalMax) > 0){
            throw createRuleError("주의 하한은 상한보다 클 수 없습니다.");
        }
        // 주의 하한은 합격범위 안에 있어야 함
        if(warningMin != null && isOutOfRange(warningMin, criticalMin, criticalMax)){
            throw createRuleError("주의 하한은 합격범위 안에 있어야 합니다.");
        }
    }

    // 규칙 설정 오류 생성
    private ResponseStatusException createRuleError(String message){
        return new ResponseStatusException(HttpStatus.CONFLICT,"이상 감지 규칙 설정 오류: "+message);
    }

    
}
