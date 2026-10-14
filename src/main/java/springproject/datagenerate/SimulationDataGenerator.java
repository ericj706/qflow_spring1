package springproject.datagenerate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulationDataGenerator {
    @Value("${simulation.inspection-failure-rate:0.01}")
    private double inspectionFailureRate;

    // 시뮬레이션 LOT의 고유번호를 생성한다.
    public String nextBatchId() {
        return "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    // 신규 LOT의 목표량·제품·탱크 정보를 만든다.
    public Map<String, Object> generateLot(int packagingUnits) {
        return Map.of("productCode", "SIM-MASK", "productName", "가상 마스크팩",
            "targetBulkKg", new BigDecimal("100.000"), "targetUnits", Math.max(1, packagingUnits),
            "tankId", "SIM-TANK-01", "status", "대기");
    }

    // 원료별 목표·실제 칭량값을 생성한다. 큰 원료·일반 원료·미량 원료를 차례로 생성한다.
    public Map<String, Object> generateMaterial(String batchId, int stageTick) {
        BigDecimal target = new BigDecimal(new String[]{"95.0000", "4.5000", "0.5000"}[stageTick - 1]);
        // 원료별 설정 범위에서 실제량을 직접 뽑는다. 목표량에 오차를 더하지 않는다.
        BigDecimal actual = switch (stageTick) {
            case 1 -> random(94.8, 95.2, 4);
            case 2 -> random(4.495, 4.505, 4);
            default -> random(0.4999, 0.5001, 4);
        };
        return Map.of("dispenseId", batchId + "-M" + stageTick,
            "materialCode", "SIM-MAT-" + stageTick, "materialName", "가상 원료 " + stageTick,
            "rawMaterialLot", "SIM-RAW-" + stageTick, "targetQtyKg", target,
            "actualQtyKg", actual,
            "dispensedAt", LocalDateTime.now(), "status", "완료");
    }

    // 시간에 따른 승온·냉각은 생략하고 유지 온도 범위에서만 측정값을 생성한다.
    public Map<String, Object> generateSensor(boolean heating) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", LocalDateTime.now());
        data.put("tank_temp_c", heating ? random(62.0, 63.0, 3) : random(37.5, 38.5, 3));
        data.put("paddle_rpm", random(31, 39, 2));
        data.put("homomixer_rpm", random(1400, 1600, 2));
        data.put("ph_level", random(5.6, 6.4, 3));
        data.put("bulk_viscosity_cps", random(1600, 3400, 3));
        data.put("motor_torque_pct", random(35, 55, 2));
        data.put("vacuum_kpa", random(-85, -75, 3));
        data.put("cooling_valve_pct", heating ? BigDecimal.ZERO : random(60, 90, 2));
        return data;
    }

    // LOT당 벌크 검사 측정값을 생성한다. 최종 합격·불합격 판정은 수행하지 않는다.
    public Map<String, Object> generateBulkQc(String batchId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("qc_id", batchId + "-QC1");
        data.put("sample_time", LocalDateTime.now());
        data.put("ph_measured", random(5.4, 6.6, 3));
        data.put("viscosity_measured", random(1400, 3600, 3));
        data.put("specific_gravity", random(1.01, 1.03, 4));
        data.put("appearance_code", randomInspectionResult());
        data.put("microbubble_code", randomInspectionResult());
        // microbial_cfu는 합·불 문자열이 아닌 미생물 측정값이다.
        data.put("microbial_cfu", Integer.toString(ThreadLocalRandom.current().nextInt(0, 11)));
        data.put("qc_notes_code", "시뮬레이션 검사");
        return data;
    }

    // 제품 한 개의 중량·실링 측정값을 생성하고 총중량을 구성 중량의 합으로 계산한다.
    public Map<String, Object> generatePackaging(String batchId, int stageTick) {
        BigDecimal sheet = random(2.8, 3.2, 3);
        BigDecimal pouch = random(5.85, 6.15, 3);
        BigDecimal essence = random(25.3, 26.7, 3);
        BigDecimal first = essence.divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_UP);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pouch_id", batchId + "-UNIT-" + stageTick);
        data.put("timestamp", LocalDateTime.now());
        data.put("packaging_line", "SIM-LINE-01");
        data.put("sheet_material_code", "SIM-SHEET-01");
        data.put("sheet_lot_no", batchId + "-SHEET");
        data.put("sheet_dry_weight_g", sheet);
        data.put("pouch_tare_weight_g", pouch);
        data.put("essence_net_weight_g", essence);
        data.put("fill_weight_1st_g", first);
        data.put("fill_weight_2nd_g", essence.subtract(first));
        data.put("gross_total_weight_g", sheet.add(pouch).add(essence));
        data.put("upper_seal_temp_c", random(180, 185, 3));
        data.put("lower_seal_temp_c", random(180, 185, 3));
        data.put("seal_pressure_bar", random(2.5, 3.5, 3));
        data.put("n2_residual_o2_pct", random(0.1, 1.0, 3));
        data.put("checkweigher_status", randomInspectionResult());
        data.put("metal_detector_status", randomInspectionResult());
        data.put("vision_inspection_status", randomInspectionResult());
        data.put("note", "시뮬레이션 제품");
        return data;
    }

    // 지정 범위의 난수를 소수점 자릿수에 맞춰 생성한다.
    private BigDecimal random(double min, double max, int scale) {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(min, max))
            .setScale(scale, RoundingMode.HALF_UP);
    }

    // 검사별로 독립적인 합·불 결과를 생성한다. 기본 불합격 확률은 검사 한 항목당 1%다.
    private String randomInspectionResult() {
        if (!Double.isFinite(inspectionFailureRate) || inspectionFailureRate < 0 || inspectionFailureRate > 1) {
            throw new IllegalArgumentException("simulation.inspection-failure-rate는 0~1이어야 합니다.");
        }
        return ThreadLocalRandom.current().nextDouble() < inspectionFailureRate ? "불합격" : "합격";
    }

    // 생성값에 DTO별 LOT·실행번호·작업자·출처와 재전송용 식별자를 붙인다.
    public Map<String, Object> preparePayload(String tableName, Map<String, Object> values, String batchId, Long executionId, int userId) {
        Map<String, Object> data = new LinkedHashMap<>(values);
        data.put("userId", userId);
        if (!"sensor_telemetry".equals(tableName) && !"filling_packaging".equals(tableName)) data.put("batchId", batchId);
        if ("batches".equals(tableName)) data.put("recordSource", "시뮬레이션");
        else if ("sensor_telemetry".equals(tableName) || "bulk_qc".equals(tableName)) data.put("record_source", "시뮬레이션");
        if ("sensor_telemetry".equals(tableName)) {
            data.put("execution_id", executionId);
            data.put("source_sample_id", UUID.randomUUID().toString());
        } else if ("bulk_qc".equals(tableName) || "filling_packaging".equals(tableName)) data.put("executionId", executionId);
        return Map.copyOf(data);
    }

    // 상태 조회에서 표시할 개별 검사 불합격 확률을 반환한다.
    public double inspectionFailureRate() { return inspectionFailureRate; }
}
