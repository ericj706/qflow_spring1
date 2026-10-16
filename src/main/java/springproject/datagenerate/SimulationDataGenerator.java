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
    // 5공정은 검사 항목별 확률이 아니라 제품 한 개의 이상 발생 확률이다.
    @Value("${simulation.packaging-anomaly-rate:${simulation.inspection-failure-rate:0.01}}")
    private double inspectionFailureRate;
    @Value("${simulation.bulk-warning-rate:0.01}")
    private double bulkWarningRate;
    @Value("${simulation.bulk-failure-rate:0.01}")
    private double bulkFailureRate;

    // 시뮬레이션 LOT의 고유번호를 생성한다.
    public String nextBatchId() {
        return "SIM-" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 24);
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

    // 검사 한 건의 시나리오를 선택한다. 기본 정상 98%, 주의 1%, 이상 1%이며 최종 판정은 기존 Service가 한다.
    public Map<String, Object> generateBulkQc(String batchId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("qc_id", batchId + "-QC1");
        data.put("sample_time", LocalDateTime.now());
        validateRate(bulkWarningRate);
        validateRate(bulkFailureRate);
        if (bulkWarningRate + bulkFailureRate > 1) {
            throw new IllegalArgumentException("벌크 주의·이상 확률 합계는 1 이하여야 합니다.");
        }
        data.put("ph_measured", random(5.9, 6.1, 3));
        data.put("viscosity_measured", random(2300, 2700, 3));
        data.put("specific_gravity", random(1.01, 1.03, 4));
        data.put("appearance_code", "합격");
        data.put("microbubble_code", "합격");
        data.put("qc_notes_code", "시뮬레이션 검사");
        double scenario = ThreadLocalRandom.current().nextDouble();
        if (scenario < bulkWarningRate) {
            // 주의 시나리오는 수치 항목 하나만 주의 구간으로 생성한다.
            if (ThreadLocalRandom.current().nextBoolean()) {
                data.put("ph_measured", random(6.35, 6.45, 3));
            } else {
                data.put("viscosity_measured", random(3350, 3450, 3));
            }
        } else if (scenario < bulkWarningRate + bulkFailureRate) {
            switch (ThreadLocalRandom.current().nextInt(4)) {
                case 0 -> data.put("ph_measured", random(6.6, 6.8, 3));
                case 1 -> data.put("viscosity_measured", random(3600, 3800, 3));
                case 2 -> data.put("appearance_code", "불합격");
                default -> data.put("microbubble_code", "불합격");
            }
        }
        return data;
    }

    // 정상 제품은 모든 항목을 정상 범위에 둔다. 이상 제품만 원인 항목 하나를 선택한다.
    public Map<String, Object> generatePackaging(String batchId, int stageTick) {
        validateRate(inspectionFailureRate);
        boolean abnormal = ThreadLocalRandom.current().nextDouble() < inspectionFailureRate;
        // 충진량·상부 실링·하부 실링·금속검사·비전검사 중 원인 하나만 선택한다.
        int cause = abnormal ? ThreadLocalRandom.current().nextInt(5) : -1;
        BigDecimal sheet = random(2.98, 3.02, 3);
        BigDecimal pouch = random(5.98, 6.02, 3);
        BigDecimal essence = cause == 0 ? random(26.8, 27.0, 3) : random(25.95, 26.05, 3);
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
        data.put("upper_seal_temp_c", cause == 1 ? random(184.2, 185.0, 3) : random(182.0, 183.0, 3));
        data.put("lower_seal_temp_c", cause == 2 ? random(184.2, 185.0, 3) : random(182.0, 183.0, 3));
        data.put("seal_pressure_bar", random(2.5, 3.5, 3));
        data.put("n2_residual_o2_pct", random(0.1, 1.0, 3));
        // 충진량 이상은 총중량과 중량선별기에도 반영되는 동일 원인의 결과다.
        data.put("checkweigher_status", cause == 0 ? "불합격" : "합격");
        data.put("metal_detector_status", cause == 3 ? "불합격" : "합격");
        data.put("vision_inspection_status", cause == 4 ? "불합격" : "합격");
        data.put("note", "시뮬레이션 제품");
        return data;
    }

    // 지정 범위의 난수를 소수점 자릿수에 맞춰 생성한다.
    private BigDecimal random(double min, double max, int scale) {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(min, max))
            .setScale(scale, RoundingMode.HALF_UP);
    }

    // 확률 설정 오류를 검증한다.
    private void validateRate(double rate) {
        if (!Double.isFinite(rate) || rate < 0 || rate > 1) {
            throw new IllegalArgumentException("시뮬레이션 확률은 0~1이어야 합니다.");
        }
    }

    // 생성값에 DTO별 LOT·실행번호·작업자·출처와 재전송용 식별자를 붙인다.
    public Map<String, Object> preparePayload(
            String tableName, Map<String, Object> values, 
            String batchId, Long executionId, int userId) {
        Map<String, Object> data = new LinkedHashMap<>(values);
        data.put("userId", userId);
        if (!"sensor_telemetry".equals(tableName) && !"filling_packaging".equals(tableName)) data.put("batchId", batchId);
        if ("batches".equals(tableName)) data.put("recordSource", "시뮬레이션");
        else if ("sensor_telemetry".equals(tableName) || "bulk_qc".equals(tableName)) data.put("record_source", "시뮬레이션");
        if ("sensor_telemetry".equals(tableName)) {
            data.put("execution_id", executionId);
            data.put("source_sample_id", UUID.randomUUID().toString());
        } else if ("material_dispensing".equals(tableName) || "bulk_qc".equals(tableName) || "filling_packaging".equals(tableName)) data.put("executionId", executionId);
        return Map.copyOf(data);
    }

    // 기존 상태 API와 호환하며 제품 한 개의 이상 확률을 반환한다.
    public double inspectionFailureRate() { return inspectionFailureRate; }
    public double bulkWarningRate() { return bulkWarningRate; }
    public double bulkFailureRate() { return bulkFailureRate; }
}
