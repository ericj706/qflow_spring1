package springproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.*;
import org.springframework.web.client.RestClient;
import springproject.datagenerate.Simulation_Service;
import springproject.datagenerate.SimulationApiClient;
import springproject.datagenerate.SimulationDataGenerator;

/** 실제 DB 대신 로컬 테스트 HTTP 서버로 저장·공정 API 계약과 대기·재시도를 검증한다. */
public class SimulationApiCheck {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String[] PROCESSES = {"원료 칭량", "가열/혼합", "냉각", "벌크 검사", "최종 포장 검사"};
    private static final Map<String, Map<String, Object>> rows = new HashMap<>();
    private static final Set<String> sensorIds = new HashSet<>();
    private static final List<Map<String, Object>> sensors = new ArrayList<>();
    private static Map<String, Object> batch;
    private static int process;
    private static boolean completed, approved, judged, failSensorOnce = true;

    public static void main(String[] args) throws Exception {
        RealtimeDataCheck.verify();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mask", exchange -> {
            int status = 200;
            Object response = Map.of();
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            try {
                Map<String, Object> body = method.equals("POST") || method.equals("PUT")
                    ? JSON.readValue(exchange.getRequestBody(), Map.class) : Map.of();
                if (path.equals("/mask/batches") && method.equals("POST")) {
                    check("대기".equals(body.get("status")), "LOT must start waiting");
                    batch = new LinkedHashMap<>(body); response = batch;
                } else if (path.startsWith("/mask/batches/")) {
                    if (batch == null) status = 404;
                    else { if (method.equals("PUT")) batch = new LinkedHashMap<>(body); response = batch; }
                } else if (path.equals("/mask/process-executions") && method.equals("GET")) {
                    response = Map.of("content", process == 0 ? List.of() : List.of(execution()));
                } else if (path.equals("/mask/process-executions") && method.equals("POST")) {
                    check("원료 칭량".equals(body.get("processCode")), "only first stage is directly started");
                    process = 1; batch.put("status", "진행중"); response = execution();
                } else if (path.startsWith("/mask/process-executions/")) {
                    if (method.equals("GET")) response = execution();
                    else if (process == 5 && !judged) { status = 409; response = Map.of("message", "제품 판정 대기"); }
                    else {
                        completed = true;
                        Map<String, Object> finished = execution();
                        if (process == 4 && !approved) {
                            response = Map.of("finishedExecution", finished, "nextProcessStatus", "검사 확인 대기", "message", "최종 검사 결과를 승인해 주세요.");
                        } else if (process == 5) {
                            batch.put("status", "완료");
                            response = Map.of("finishedExecution", finished, "nextProcessStatus", "LOT 완료", "message", "LOT 완료");
                        } else {
                            process++; completed = false;
                            response = Map.of("finishedExecution", finished, "nextExecution", execution(), "nextProcessStatus", "다음 공정 시작", "message", "다음 공정 시작");
                        }
                    }
                } else if (path.equals("/mask/sensor-telemetries")) {
                    check(((Number) body.get("execution_id")).intValue() == process, "actual execution id");
                    check(body.containsKey("tank_temp_c") && !body.containsKey("stage"), "sensor fields without stage");
                    double temp = ((Number) body.get("tank_temp_c")).doubleValue();
                    check(process == 2 ? temp >= 62.0 && temp <= 63.0 : temp >= 37.5 && temp <= 38.5,
                        "only maintained temperatures, no ramp or cooling curve");
                    if (sensorIds.add((String) body.get("source_sample_id"))) sensors.add(body);
                    if (failSensorOnce) { failSensorOnce = false; status = 500; }
                    response = body;
                } else if (method.equals("POST")) {
                    String key = path.equals("/mask/material-dispensing") ? "dispenseId"
                        : path.equals("/mask/bulk-qc") ? "qc_id" : "pouch_id";
                    if (path.equals("/mask/filling-packagings")) {
                        check(!body.containsKey("batchId") && !body.containsKey("record_source"), "packaging request DTO fields");
                        check(!body.containsKey("final_disposition"), "no fabricated final decision");
                    }
                    rows.put(path + "/" + body.get(key), body); response = body;
                } else {
                    response = rows.get(path);
                    if (response == null) { status = 404; response = Map.of(); }
                }
            } catch (Throwable error) {
                status = 500; response = Map.of("testFailure", error.toString());
            }
            byte[] bytes = JSON.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            var generator = new SimulationDataGenerator();
            var client = new SimulationApiClient(RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort());
            Simulation_Service simulation = new Simulation_Service(client, generator);
            set(simulation, "enabled", true); set(simulation, "sensorTicks", 2);
            set(simulation, "packagingUnits", 3); set(simulation, "lotDelayMs", 60000L);
            set(simulation, "userId", 1); set(generator, "inspectionFailureRate", 0.01);
            simulation.ready();
            for (int i = 0; i < 30 && simulation.status().get("lastError") == null; i++) simulation.tick();
            check(simulation.status().get("lastError") != null, "HTTP failure pauses generation");
            check(simulation.status().get("pendingData") != null, "failed request is retained");
            simulation.start();
            for (int i = 0; i < 35; i++) simulation.tick();
            check(simulation.status().get("lastError") == null, "retry succeeds");
            check(sensors.size() == 4, "sensor retry has no duplicate data");
            check(process == 4 && simulation.status().get("waitingReason") != null, "bulk approval blocks progression");
            check(rows.keySet().stream().noneMatch(key -> key.contains("filling-packagings")), "no early packaging data");
            approved = true;
            for (int i = 0; i < 12; i++) simulation.tick();
            check(process == 5 && simulation.status().get("waitingReason") != null, "unjudged products block finish");
            check(rows.keySet().stream().filter(key -> key.contains("material-dispensing")).count() == 3, "three materials saved");
            check(rows.keySet().stream().filter(key -> key.contains("bulk-qc")).count() == 1, "one bulk inspection saved");
            check(rows.keySet().stream().filter(key -> key.contains("filling-packagings")).count() == 3, "three products saved");
            judged = true; simulation.tick();
            check(simulation.status().get("batchId") == null && "완료".equals(batch.get("status")), "LOT completes through API");
            int count = rows.size(); simulation.tick(); check(rows.size() == count, "next LOT delay");
            simulation.stop(); simulation.tick(); check(rows.size() == count, "stop prevents writes");
            System.out.println("PASS: REST payloads, real execution IDs, failed-write retry, approval wait, all stages, LOT completion, stop");
        } finally { server.stop(0); }
    }

    private static Map<String, Object> execution() {
        return Map.of("execution_id", process, "batchId", batch.get("batchId"),
            "process_code", PROCESSES[process - 1], "status", completed ? "완료" : "진행중");
    }

    private static void set(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
