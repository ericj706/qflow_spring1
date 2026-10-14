package springproject.datagenerate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import springproject.model.dto.Process_execution_Dto;

@Service
public class Simulation_Service {
    private final SimulationApiClient apiClient;
    private final SimulationDataGenerator generator;

    // 통신과 생성 기능을 주입받고 이 서비스는 공정 상태만 관리한다.
    public Simulation_Service(SimulationApiClient apiClient, SimulationDataGenerator generator) {
        this.apiClient = apiClient;
        this.generator = generator;
    }
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
    @Value("${simulation.user-id:1}")
    private int userId;
    @Value("${simulation.process-duration-seconds:20}")
    private long processDurationSeconds;

    private Clock clock = Clock.systemDefaultZone();
    private LocalDateTime processStartedAt;

    private String batchId;
    private int processIndex;
    private int stageTick;
    private long sequence;
    private Instant nextLotAt = Instant.MIN;
    private Long executionId;
    private boolean ready;
    private String waitingReason;
    private String lastError;
    private SimulationSample pending;
    private BigDecimal materialTotal = BigDecimal.ZERO;
    private boolean lotPrepared;
    private String completedBatchId;

    // 서버가 HTTP 요청을 받을 수 있는 시점부터 생성 스케줄을 허용한다.
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void ready() { ready = true; }

    // 저장 성공한 데이터와 실제 DB 실행번호를 최근 조회 기록으로 보관한다.
    public record SimulationSample(long sequence, String tableName, String batchId,
        Long executionId, String processCode, LocalDateTime generatedAt,
        Map<String, Object> data) {}

    // 매 주기마다 생성값을 REST로 저장한다. 실패 시 자동 중지하며 동일 요청을 보관한다.
    public synchronized void tick() {
        if (!enabled || !ready) return;
        try { generateTick(); }
        catch (RuntimeException error) {
            enabled = false;
            lastError = apiClient.describeError(error);
        }
    }

    // 대기 상태에서는 실제 공정 상태를 조회하며 다음 실행이 승인될 때까지 생성하지 않는다.
    private void generateTick() {
        if (pending != null) { savePending(); return; }
        if (batchId == null) {
            if (!lotPrepared && !clock.instant().isBefore(nextLotAt)) createLot();
            return;
        }
        // 시작 Controller가 실행번호를 연결할 때까지 DB 조회·측정을 하지 않는다.
        if (executionId == null) return;
        int limit = stageLimit();
        long elapsed = elapsedMillis();
        long duration = Math.max(1, processDurationSeconds) * 1000;
        // 20초 동안 목표 건수를 분산 생성하고, 지연된 주기는 누락 없이 따라잡는다.
        int due = (int) Math.min(limit, (double) Math.max(0, elapsed) / duration * limit);
        while (stageTick < due) generateSample();
        if (elapsed >= duration && stageTick >= limit) finishProcess();
    }

    // 현재 공정의 데이터를 한 건 생성·저장한다.
    private void generateSample() {
        stageTick++;
        switch (processIndex) {
            case 0 -> {
                var data = generator.generateMaterial(batchId, stageTick);
                materialTotal = materialTotal.add((BigDecimal) data.get("actualQtyKg"));
                emit("material_dispensing", data);
            }
            case 1, 2 -> emit("sensor_telemetry", generator.generateSensor(processIndex == 1));
            case 3 -> emit("bulk_qc", generator.generateBulkQc(batchId));
            case 4 -> emit("filling_packaging", generator.generatePackaging(batchId, stageTick));
            default -> throw new IllegalStateException("알 수 없는 공정");
        }
    }

    // 실제 공정 시작 시각을 기준으로 경과시간을 계산한다.
    private long elapsedMillis() {
        return processStartedAt == null ? 0 : Math.max(0, Duration.between(processStartedAt, LocalDateTime.now(clock)).toMillis());
    }

    // 공정별로 생성할 측정 건수를 결정한다.
    private int stageLimit() {
        return switch (processIndex) {
            case 0 -> 3;
            case 1, 2 -> Math.max(2, sensorTicks);
            case 3 -> 1;
            default -> Math.max(1, packagingUnits);
        };
    }

    // LOT 하나만 대기 상태로 등록한다. 공정 시작은 화면의 버튼을 기다린다.
    private void createLot() {
        apiClient.validateUser(userId);
        lotPrepared = true;
        batchId = generator.nextBatchId();
        processIndex = 0;
        stageTick = 0;
        executionId = null;
        processStartedAt = null;
        materialTotal = BigDecimal.ZERO;
        emit("batches", generator.generateLot(packagingUnits));
        waitingReason = "공정 시작 버튼을 눌러 주세요.";
    }

    // 시작 Controller가 커밋 성공 후 호출한다. 다른 LOT와 중복 연결은 무시한다.
    public synchronized void connectStartedProcess(Process_execution_Dto execution) {
        if (batchId == null || executionId != null || execution == null
            || !batchId.equals(execution.getBatchId()) || !"진행중".equals(execution.getStatus())) return;
        useExecution(execution);
    }

    // 서버가 반환한 실제 실행번호와 공정명을 생성기의 현재 상태에 반영한다.
    private void useExecution(Process_execution_Dto execution) {
        executionId = execution.getExecution_id();
        processIndex = List.of(PROCESSES).indexOf(execution.getProcess_code());
        if (processIndex < 0) throw new IllegalStateException("알 수 없는 실제 공정명");
        stageTick = 0;
        processStartedAt = execution.getStart_time() == null ? LocalDateTime.now(clock) : execution.getStart_time();
        waitingReason = null;
        remember(new SimulationSample(++sequence, "process_execution", batchId, executionId,
            PROCESSES[processIndex], LocalDateTime.now(), Map.of("execution_id", executionId, "status", execution.getStatus())));
    }

    // 종료 응답에 따라 다음 공정·승인 대기·LOT 완료 상태로 전환한다.
    private void finishProcess() {
        if (processIndex == 0) apiClient.updateActualBulk(batchId, materialTotal);
        var completion = apiClient.finishOrResume(executionId);
        if (completion.result() == null) {
            waitingReason = completion.waitingReason();
            return;
        }
        var result = completion.result();
        remember(new SimulationSample(++sequence, "process_execution", batchId, executionId,
            PROCESSES[processIndex], LocalDateTime.now(), Map.of("status", result.getFinishedExecution().getStatus(), "message", result.getMessage())));
        if (result.getNextExecution() != null) useExecution(result.getNextExecution());
        else if ("LOT 완료".equals(result.getNextProcessStatus())) {
            completedBatchId = batchId;
            batchId = null;
            executionId = null;
            processStartedAt = null;
            waitingReason = null;
            nextLotAt = clock.instant().plusMillis(Math.max(0, lotDelayMs));
            // 완료 후 다음 LOT 하나만 준비하고, 새 공정 시작 버튼을 기다린다.
            lotPrepared = false;
        } else waitingReason = result.getMessage();
    }

    // 요청을 한 번 구성해 보관하고 저장을 시도한다.
    private void emit(String tableName, Map<String, Object> values) {
        var data = generator.preparePayload(tableName, values, batchId, executionId, userId);
        pending = new SimulationSample(++sequence, tableName, batchId, executionId,
            PROCESSES[processIndex], LocalDateTime.now(), data);
        savePending();
    }

    // 같은 요청을 재사용하며 저장 성공 후에만 미저장 상태를 해제한다.
    private void savePending() {
        apiClient.save(pending.tableName(), pending.data());
        remember(pending);
        pending = null;
        lastError = null;
    }

    // 실제 저장에 성공한 요청·공정 결과만 최근 기록에 보관한다.
    private void remember(SimulationSample sample) {
        history.addLast(sample);
        while (history.size() > HISTORY_LIMIT) history.removeFirst();
    }

    // 수동으로 생성을 시작하거나 저장 실패 후 보관된 요청을 다시 시도한다.
    public synchronized Map<String, Object> start() {
        // 완료 후 명시적으로 생성기를 다시 시작할 때만 새 LOT 등록을 허용한다.
        if (batchId == null) lotPrepared = false;
        enabled = true;
        lastError = null;
        return status();
    }

    // DB 상태를 삭제하지 않고 다음 생성 주기부터 정지한다.
    public synchronized Map<String, Object> stop() {
        enabled = false;
        return status();
    }

    // 현재 실행 상태를 반환한다. 생성 데이터 조회는 공정을 진행시키지 않는다.
    public synchronized Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("batchId", batchId);
        result.put("completedBatchId", completedBatchId);
        result.put("executionId", executionId);
        result.put("waitingReason", waitingReason);
        result.put("lastError", lastError);
        result.put("pendingData", pending);
        result.put("processCode", batchId == null ? null : PROCESSES[processIndex]);
        result.put("stageTick", stageTick);
        result.put("processDurationSeconds", Math.max(1, processDurationSeconds));
        result.put("processStartedAt", processStartedAt);
        result.put("elapsedSeconds", elapsedMillis() / 1000);
        result.put("generatedCount", sequence);
        result.put("retainedCount", history.size());
        result.put("lotDelayMs", lotDelayMs);
        result.put("sensorTicks", Math.max(2, sensorTicks));
        result.put("packagingUnits", Math.max(1, packagingUnits));
        result.put("inspectionFailureRate", generator.inspectionFailureRate());
        return result;
    }

    // 최근 생성 데이터의 복사본을 반환해 외부에서 내부 목록을 변경하지 못하게 한다.
    public synchronized List<SimulationSample> samples() {
        return List.copyOf(history);
    }
}
