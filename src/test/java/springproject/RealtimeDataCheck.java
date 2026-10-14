package springproject;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.server.ResponseStatusException;
import springproject.model.dto.*;
import springproject.model.entity.*;
import springproject.model.repository.*;
import springproject.service.Sensor_telemetry_Service;
import springproject.service.Bulk_qc_Service;

/** 저장소 대역으로 센서·벌크 Entity 연결과 입력 검증을 확인한다. */
class RealtimeDataCheck {
    static void verify() {
        var batch = Batches_Entity.builder().batchId("TEST-LOT").status("진행중").build();
        var execution = Process_execution_Entity.builder().execution_id(10L)
            .batchesEntity(batch).process_code("가열/혼합").status("진행중").build();
        var user = Users_Entity.builder().userId(1).build();
        Map<String, Sensor_telemetry_Entity> sensorRows = new HashMap<>();
        Map<String, Bulk_qc_Entity> qcRows = new HashMap<>();
        var batches = repository(Batches_Repository.class, (name, args) -> Optional.of(batch));
        var executions = repository(Process_execution_Repository.class, (name, args) ->
            name.equals("findBatchIdByExecutionId") ? Optional.of(batch.getBatchId()) : Optional.of(execution));
        var users = repository(Users_Repository.class, (name, args) -> Optional.of(user));
        var sensors = repository(Sensor_telemetry_Repository.class, (name, args) -> {
            if (name.equals("findBySourceSampleId")) return Optional.ofNullable(sensorRows.get(args[0]));
            var row = (Sensor_telemetry_Entity) args[0]; row.setSensor_id(1L);
            sensorRows.put(row.getSource_sample_id(), row); return row;
        });
        var inspections = repository(Bulk_qc_Repository.class, (name, args) -> {
            if (name.equals("existsById")) return qcRows.containsKey(args[0]);
            var row = (Bulk_qc_Entity) args[0]; qcRows.put(row.getQc_id(), row); return row;
        });
        var sensorService = new Sensor_telemetry_Service(batches, executions, users, sensors);
        var bulkService = new Bulk_qc_Service(users, inspections, batches, executions);
        var sensor = Sensor_telemetry_Dto.builder().execution_id(10L).userId(1)
            .timestamp(LocalDateTime.now()).tank_temp_c(new BigDecimal("62.500"))
            .paddle_rpm(new BigDecimal("35.00")).source_sample_id("request-1").build();
        var saved = sensorService.save(sensor);
        check(saved.getExecution_id() == 10L && saved.getTank_temp_c().compareTo(sensor.getTank_temp_c()) == 0, "sensor DTO mapping");
        sensorService.save(sensor); check(sensorRows.size() == 1, "sensor idempotency");
        sensor.setTank_temp_c(new BigDecimal("63.0")); expect(409, () -> sensorService.save(sensor));
        sensor.setSource_sample_id("request-2");
        sensor.setTank_temp_c(new BigDecimal("62.5001")); expect(400, () -> sensorService.save(sensor));
        sensor.setTank_temp_c(new BigDecimal("62.5")); execution.setStatus("완료");
        expect(409, () -> sensorService.save(sensor)); execution.setStatus("진행중");
        execution.setProcess_code("원료 칭량"); expect(409, () -> sensorService.save(sensor));
        execution.setProcess_code("벌크 검사");
        var qc = Bulk_qc_Dto.builder().qc_id("QC-1").batchId("TEST-LOT").executionId(10L)
            .userId(1).sample_time(LocalDateTime.now()).ph_measured(new BigDecimal("6.000"))
            .viscosity_measured(new BigDecimal("2500.000")).build();
        bulkService.save(qc);
        check(qcRows.get("QC-1").getProcess_execution_Entity() == execution, "bulk execution FK");
        check(qcRows.get("QC-1").getUsersEntity() == user, "bulk user FK");
        check(qcRows.get("QC-1").getOverall_qc_result() == null, "no fabricated final judgment");
        expect(409, () -> bulkService.save(qc));
        qc.setQc_id("QC-2"); qc.setBatchId("OTHER"); expect(409, () -> bulkService.save(qc));
        System.out.println("PASS: sensor/bulk mapping without stage, foreign keys, duplicate protection, invalid process/precision/status rejection");
    }

    interface Call { Object invoke(String name, Object[] args); }
    @SuppressWarnings("unchecked")
    private static <T> T repository(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
            (proxy, method, args) -> call.invoke(method.getName(), args));
    }
    private static void expect(int status, Runnable action) {
        try { action.run(); throw new AssertionError("Expected HTTP " + status); }
        catch (ResponseStatusException error) { check(error.getStatusCode().value() == status, "HTTP status"); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
