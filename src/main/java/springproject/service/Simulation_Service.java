package springproject.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Simulation_Service {
    private static final String[] PROCESSES = {"원료 칭량", "가열/혼합", "냉각", "벌크 검사", "최종 포장 검사"};
    private static final int HISTORY_LIMIT = 200;
    private final ArrayDeque<SimulationSample> history = new ArrayDeque<>();

    @Value("${simulation.enabled:true}")
    private boolean enabled;
    @Value("${simulation.lot-delay-ms:10000}")
    private long lotDelayMs;
    @Value("${simulation.sensor-ticks:10}")
    private int sensorTicks;
    @Value("${simulation.packaging-units:10}")
    private int packagingUnits;

    private String batchId;
    private String executionKey;
    private int processIndex;
    private int stageTick;
    private long sequence;
    private Instant nextLotAt = Instant.MIN;

    // DB의 execution_id가 아닌 가상 실행 식별자를 포함하는 생성 데이터 형식이다.
    public record SimulationSample(long sequence, String tableName, String batchId,
        String simulationExecutionKey, String processCode, LocalDateTime generatedAt,
        Map<String, Object> data) {}

    // 스케줄마다 현재 공정 데이터를 생성한다. DB 저장·HTTP 전송·이상 판정은 수행하지 않는다.
    public synchronized void tick() {
        if (!enabled) return;
        if (batchId == null) {
            if (!Instant.now().isBefore(nextLotAt)) createLot();
            return;
        }
        stageTick++;
        switch (processIndex) {
            case 0 -> generateMaterial();
            case 1, 2 -> generateSensor();
            case 3 -> generateBulkQc();
            case 4 -> generatePackaging();
            default -> throw new IllegalStateException("알 수 없는 가상 공정");
        }
        int limit = switch (processIndex) {
            case 0 -> 3;
            case 1, 2 -> Math.max(2, sensorTicks);
            case 3 -> 1;
            default -> Math.max(1, packagingUnits);
        };
        if (stageTick >= limit) finishProcess();
    }

    // 고유한 LOT를 만들고 원료 칭량 공정을 시작한다.
    private void createLot() {
        batchId = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        processIndex = 0;
        stageTick = 0;
        executionKey = null;
        emit("batches", Map.of("productCode", "SIM-MASK", "productName", "가상 마스크팩",
            "targetBulkKg", new BigDecimal("100.000"), "targetUnits", Math.max(1, packagingUnits),
            "tankId", "SIM-TANK-01", "status", "진행중", "startTime", LocalDateTime.now()));
        startProcess();
    }

    // 현재 공정의 가상 실행 식별자를 생성하고 시작 기록을 남긴다.
    private void startProcess() {
        executionKey = batchId + "-P" + (processIndex + 1);
        stageTick = 0;
        emit("process_execution", Map.of("status", "진행중", "start_time", LocalDateTime.now()));
    }

    // 원료별 목표·실제 칭량값을 생성한다. 큰 원료·일반 원료·미량 원료를 차례로 생성한다.
    private void generateMaterial() {
        BigDecimal target = new BigDecimal(new String[]{"95.0000", "4.5000", "0.5000"}[stageTick - 1]);
        BigDecimal deviation = stageTick == 3 ? random(-0.0001, 0.0001, 4)
            : target.multiply(random(-0.003, 0.003, 6));
        emit("material_dispensing", Map.of("dispenseId", batchId + "-M" + stageTick,
            "materialCode", "SIM-MAT-" + stageTick, "materialName", "가상 원료 " + stageTick,
            "rawMaterialLot", "SIM-RAW-" + stageTick, "targetQtyKg", target,
            "actualQtyKg", target.add(deviation).setScale(4, RoundingMode.HALF_UP),
            "dispensedAt", LocalDateTime.now(), "status", "완료"));
    }

    // 가열은 승온 후 유지, 냉각은 온도 감소 후 투입 직전 상태로 측정값을 생성한다.
    private void generateSensor() {
        int ticks = Math.max(2, sensorTicks);
        double progress = Math.min(1.0, (double) stageTick / ticks);
        boolean heating = processIndex == 1;
        double temp = heating ? 25 + 37.5 * Math.min(1, progress * 2) : 62.5 - 24.5 * progress;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", LocalDateTime.now());
        data.put("stage", heating ? (progress < 0.5 ? "HEAT_RAMP" : "HEAT_HOLD")
            : (stageTick == ticks ? "BEFORE_HA" : "COOLING"));
        // tank_temp_c와 stage는 추후 수신 DTO/Entity에 추가할 예정인 필드이다.
        data.put("tank_temp_c", BigDecimal.valueOf(temp).add(random(-0.5, 0.5, 3)));
        data.put("paddle_rpm", random(31, 39, 2));
        data.put("homomixer_rpm", random(1400, 1600, 2));
        data.put("ph_level", random(5.6, 6.4, 3));
        data.put("bulk_viscosity_cps", random(1600, 3400, 3));
        data.put("motor_torque_pct", random(35, 55, 2));
        data.put("vacuum_kpa", random(-85, -75, 3));
        data.put("cooling_valve_pct", heating ? BigDecimal.ZERO : random(60, 90, 2));
        emit("sensor_telemetry", data);
    }

    // LOT당 벌크 검사 측정값을 생성한다. 최종 합격·불합격 판정은 수행하지 않는다.
    private void generateBulkQc() {
        emit("bulk_qc", Map.of("qc_id", batchId + "-QC1", "sample_time", LocalDateTime.now(),
            "ph_measured", random(5.4, 6.6, 3), "viscosity_measured", random(1400, 3600, 3),
            "specific_gravity", random(1.01, 1.03, 4), "ph_criteria", "5.50~6.50",
            "viscosity_criteria", "1500~3500", "overall_qc_result", "미판정"));
    }

    // 제품 한 개의 중량·실링 측정값을 생성하고 총중량을 구성 중량의 합으로 계산한다.
    private void generatePackaging() {
        BigDecimal sheet = random(2.8, 3.2, 3);
        BigDecimal pouch = random(5.85, 6.15, 3);
        BigDecimal essence = random(25.3, 26.7, 3);
        BigDecimal first = essence.divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_UP);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pouch_id", batchId + "-UNIT-" + stageTick);
        data.put("timestamp", LocalDateTime.now());
        data.put("packaging_line", "SIM-LINE-01");
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
        data.put("final_disposition", "미판정");
        emit("filling_packaging", data);
    }

    // 가상 공정을 종료하고 다음 공정으로 이동한다. 실제 검사 승인·알람 대기는 반영하지 않는다.
    private void finishProcess() {
        emit("process_execution", Map.of("status", "완료", "end_time", LocalDateTime.now()));
        if (processIndex < PROCESSES.length - 1) {
            processIndex++;
            startProcess();
        } else {
            emit("batches", Map.of("status", "완료", "endTime", LocalDateTime.now()));
            batchId = null;
            executionKey = null;
            nextLotAt = Instant.now().plusMillis(Math.max(0, lotDelayMs));
        }
    }

    // 지정 범위의 난수를 소수점 자릿수에 맞춰 생성한다.
    private BigDecimal random(double min, double max, int scale) {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(min, max))
            .setScale(scale, RoundingMode.HALF_UP);
    }

    // 생성 데이터를 메모리에 보관하고 오래된 데이터를 제거해 메모리 사용량을 제한한다.
    private void emit(String tableName, Map<String, Object> values) {
        Map<String, Object> data = new LinkedHashMap<>(values);
        data.put("recordSource", "시뮬레이션");
        history.addLast(new SimulationSample(++sequence, tableName, batchId, executionKey,
            PROCESSES[processIndex], LocalDateTime.now(), Map.copyOf(data)));
        while (history.size() > HISTORY_LIMIT) history.removeFirst();
    }

    // 현재 실행 상태를 반환한다. 생성 데이터 조회는 공정을 진행시키지 않는다.
    public synchronized Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("batchId", batchId);
        result.put("simulationExecutionKey", executionKey);
        result.put("processCode", batchId == null ? null : PROCESSES[processIndex]);
        result.put("stageTick", stageTick);
        result.put("generatedCount", sequence);
        result.put("retainedCount", history.size());
        result.put("lotDelayMs", lotDelayMs);
        result.put("sensorTicks", Math.max(2, sensorTicks));
        result.put("packagingUnits", Math.max(1, packagingUnits));
        return result;
    }

    // 최근 생성 데이터의 복사본을 반환해 외부에서 내부 목록을 변경하지 못하게 한다.
    public synchronized List<SimulationSample> samples() {
        return List.copyOf(history);
    }
}
