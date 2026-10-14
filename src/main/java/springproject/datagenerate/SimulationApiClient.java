package springproject.datagenerate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import springproject.model.dto.Batches_Dto;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.Process_execution_Finish_Dto;

@Component
public class SimulationApiClient {
    private final RestClient api;
    // 기존 REST API를 호출하는 클라이언트에 연결·응답 제한시간을 설정한다.
    public SimulationApiClient(RestClient.Builder builder,
        @Value("${simulation.api-base-url:http://localhost:${server.port:8080}}") String baseUrl) {
        var http = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));
        api = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    // 첫 공정만 기존 시작 API로 생성한다. 응답 유실 시 조회로 이미 생성된 실행을 복구한다.
    public Process_execution_Dto startOrFindProcess(String batchId, String firstProcess) {
        Map<?, ?> page = api.get().uri("/mask/process-executions?batchId={batchId}", batchId).retrieve().body(Map.class);
        Process_execution_Dto execution = null;
        if (page != null && page.get("content") instanceof List<?> content && !content.isEmpty()) {
            var row = (Map<?, ?>) content.get(0);
            execution = api.get().uri("/mask/process-executions/{id}", row.get("execution_id"))
                .retrieve().body(Process_execution_Dto.class);
        } else {
            execution = api.post().uri("/mask/process-executions").body(Map.of("batchId", batchId, "processCode", firstProcess))
                .retrieve().body(Process_execution_Dto.class);
        }
        return java.util.Objects.requireNonNull(execution, "공정 시작 응답이 없습니다.");
    }

    // 원료 실측량 합계를 기존 LOT 수정 API로 반영한다.
    public void updateActualBulk(String batchId, BigDecimal total) {
        Batches_Dto batch = api.get().uri("/mask/batches/{id}", batchId).retrieve().body(Batches_Dto.class);
        java.util.Objects.requireNonNull(batch).setActualBulkKg(total);
        api.put().uri("/mask/batches/{id}", batchId).body(batch).retrieve().toBodilessEntity();
    }

    // 정상 종료 결과와 HTTP 409 진행 보류 응답을 구분해 반환한다.
    public record Completion(Process_execution_Finish_Dto result, String waitingReason) {}

    // 실제 공정 상태에 따라 종료 또는 재개 API를 호출한다.
    public Completion finishOrResume(Long executionId) {
        Process_execution_Dto current = api.get().uri("/mask/process-executions/{id}", executionId)
            .retrieve().body(Process_execution_Dto.class);
        String operation = "완료".equals(java.util.Objects.requireNonNull(current).getStatus()) ? "resume" : "finish";
        try {
            var result = api.patch().uri("/mask/process-executions/{id}/" + operation, executionId)
                .retrieve().body(Process_execution_Finish_Dto.class);
            return new Completion(java.util.Objects.requireNonNull(result), null);
        } catch (RestClientResponseException response) {
            if (response.getStatusCode().value() != 409) throw response;
            return new Completion(null, response.getResponseBodyAsString());
        }
    }

    // 원료·검사·제품의 고유번호 조회와 센서 요청번호로 재전송 중복 저장을 방지한다.
    public void save(String tableName, Map<String, Object> data) {
        String path = switch (tableName) {
            case "batches" -> "/mask/batches";
            case "material_dispensing" -> "/mask/material-dispensing";
            case "sensor_telemetry" -> "/mask/sensor-telemetries";
            case "bulk_qc" -> "/mask/bulk-qc";
            case "filling_packaging" -> "/mask/filling-packagings";
            default -> throw new IllegalStateException("지원하지 않는 저장 테이블");
        };
        String key = switch (tableName) {
            case "batches" -> "batchId";
            case "material_dispensing" -> "dispenseId";
            case "bulk_qc" -> "qc_id";
            case "filling_packaging" -> "pouch_id";
            default -> null;
        };
        boolean exists = false;
        if (key != null) {
            try { api.get().uri(path + "/{id}", data.get(key)).retrieve().toBodilessEntity(); exists = true; }
            catch (RestClientResponseException response) { if (response.getStatusCode().value() != 404) throw response; }
        }
        if (!exists) api.post().uri(path).body(data).retrieve().toBodilessEntity();
    }

    // 통신·저장 오류를 생성기 상태 조회에 표시할 메시지로 변환한다.
    public String describeError(RuntimeException error) {
        return error instanceof RestClientResponseException response
            ? "HTTP " + response.getStatusCode().value() + ": " + response.getResponseBodyAsString()
            : error.getMessage();
    }
}
