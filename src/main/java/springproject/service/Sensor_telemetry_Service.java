package springproject.service;

import java.math.BigDecimal;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Users_Repository;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Process_execution_Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Sensor_telemetry_Dto;
import springproject.model.dto.search.Sensor_telemetry_SearchDto;
import springproject.model.entity.Sensor_telemetry_Entity;
import springproject.model.repository.Sensor_telemetry_Repository;

@Service 
@RequiredArgsConstructor 
@Transactional (readOnly = true)
public class Sensor_telemetry_Service {
    private final Batches_Repository batches;
    private final Process_execution_Repository executions;
    private final Users_Repository users;

    private final Sensor_telemetry_Repository sr;

    // 전체조회 + 조건검색 + 페이징
    public Page<Sensor_telemetry_Dto> search(
            Sensor_telemetry_SearchDto searchDto,
            int page) {
        // 페이지 번호 확인
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"페이지 번호는 0 이상이어야 합니다.");
        }

        // 검색조건 꺼내기
        LocalDateTime startAt = searchDto.getStartAt();
        LocalDateTime endAt = searchDto.getEndAt();
        String batchId = searchDto.getBatchId();
        Long executionId = searchDto.getExecutionId();
        String processCode = searchDto.getProcessCode();
        Integer userId = searchDto.getUserId();

        // 시작시간과 종료시간 확인
        if (startAt != null&& endAt != null&& startAt.isAfter(endAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작시간은 종료시간보다 늦을 수 없습니다.");
        }

        // 공정 실행번호 확인
        if (executionId != null && executionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"공정 실행번호는 1 이상이어야 합니다.");
        }

        // 담당자 번호 확인
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당자 번호는 1 이상이어야 합니다.");
        }

        // LOT 번호: 앞뒤 공백 제거, 빈 문자열은 null 처리
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) {batchId = null;}
        }

        // 공정코드: 앞뒤 공백 제거, 빈 문자열은 null 처리
        if (processCode != null) {
            processCode = processCode.trim();
            if (processCode.isEmpty()) {processCode = null;}
        }

        // 한 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY에서 처리
        Pageable pageable = PageRequest.of(page, 20);

        // 조건에 맞는 데이터를 페이지 단위로 조회하고 DTO로 변환
        return sr.search(
                startAt,
                endAt,
                batchId,
                executionId,
                processCode,
                userId,
                pageable
        ).map(Sensor_telemetry_Dto::from);
    }

    // 개별조회 : PK(sensor_id)로 조회한 후 DTO 변환
    public Sensor_telemetry_Dto findOne(Long sensor_id){
        Sensor_telemetry_Entity entity =sr.findById(sensor_id).orElse(null);

        // 해당 PK의 데이터가 없으면 null 반환
        if(entity == null){
            return null;
        }
        return Sensor_telemetry_Dto.from(entity);
    }

    // 실제 진행 중인 공정에 센서 측정값을 연결해 저장한다.
    @Transactional
    public Sensor_telemetry_Dto save(Sensor_telemetry_Dto dto) {
        if (dto == null || dto.getTimestamp() == null || dto.getTank_temp_c() == null
            || dto.getPaddle_rpm() == null) {
            throw error(HttpStatus.BAD_REQUEST, "측정시간, 온도와 교반속도가 필요합니다.");
        }
        Process_execution_Entity execution = running(dto.getExecution_id());
        if (dto.getSource_sample_id() != null) {
            if (dto.getSource_sample_id().isBlank() || dto.getSource_sample_id().length() > 50) {
                throw error(HttpStatus.BAD_REQUEST, "측정 요청번호는 1~50자여야 합니다.");
            }
            var previous = sr.findBySourceSampleId(dto.getSource_sample_id());
            if (previous.isPresent()) {
                var old = previous.get();
                if (!old.getProcess_ExecutionEntity().getExecution_id().equals(dto.getExecution_id())) {
                    throw error(HttpStatus.CONFLICT, "다른 공정에서 사용한 측정 요청번호입니다.");
                }
                if (!java.util.Objects.equals(old.getUsersEntity().getUserId(), dto.getUserId())
                    || !same(old.getTank_temp_c(), dto.getTank_temp_c())
                    || !same(old.getPaddle_rpm(), dto.getPaddle_rpm())
                    || !same(old.getHomomixer_rpm(), dto.getHomomixer_rpm())
                    || !same(old.getPh_level(), dto.getPh_level())
                    || !same(old.getBulk_viscosity_cps(), dto.getBulk_viscosity_cps())
                    || !same(old.getMotor_torque_pct(), dto.getMotor_torque_pct())
                    || !same(old.getVacuum_kpa(), dto.getVacuum_kpa())
                    || !same(old.getCooling_valve_pct(), dto.getCooling_valve_pct())) {
                    throw error(HttpStatus.CONFLICT, "같은 측정 요청번호로 다른 값을 저장할 수 없습니다.");
                }
                return Sensor_telemetry_Dto.from(old);
            }
        }
        boolean heat = "가열/혼합".equals(execution.getProcess_code());
        boolean cool = "냉각".equals(execution.getProcess_code());
        if (!heat && !cool) {
            throw error(HttpStatus.CONFLICT, "가열/혼합 또는 냉각 공정에만 센서값을 저장할 수 있습니다.");
        }
        decimal(dto.getTank_temp_c(), 8, 3);
        decimal(dto.getPaddle_rpm(), 10, 2);
        decimal(dto.getHomomixer_rpm(), 10, 2);
        decimal(dto.getBulk_viscosity_cps(), 12, 3);
        decimal(dto.getPh_level(), 6, 3);
        decimal(dto.getMotor_torque_pct(), 6, 2);
        decimal(dto.getVacuum_kpa(), 8, 3);
        decimal(dto.getCooling_valve_pct(), 6, 2);
        if (dto.getRecord_source() != null && dto.getRecord_source().length() > 20) {
            throw error(HttpStatus.BAD_REQUEST, "데이터 출처는 20자 이하여야 합니다.");
        }
        return Sensor_telemetry_Dto.from(sr.save(dto.toEntity(execution, worker(dto.getUserId()))));
    }

    
    // 기존 공정 서비스와 같은 순서로 LOT·공정을 잠그고 진행 상태를 검사한다.
    private Process_execution_Entity running(Long id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "실제 공정 실행번호가 필요합니다.");
        String batchId = executions.findBatchIdByExecutionId(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "공정 실행 기록이 없습니다."));
        var batch = batches.findByIdForUpdate(batchId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "LOT가 없습니다."));
        var execution = executions.findByIdForUpdate(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "공정 실행 기록이 없습니다."));
        if (!"진행중".equals(batch.getStatus()) || !"진행중".equals(execution.getStatus())) {
            throw error(HttpStatus.CONFLICT, "진행 중인 LOT와 공정에만 데이터를 저장할 수 있습니다.");
        }
        return execution;
    }

    // 저장 요청의 담당자가 실제 존재하는지 확인한다.
    private Users_Entity worker(Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "작업자 번호가 필요합니다.");
        return users.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "작업자가 없습니다."));
    }

    // DB DECIMAL 컬럼의 자릿수를 초과하는 입력을 저장 전에 거절한다.
    private void decimal(BigDecimal value, int precision, int scale) {
        if (value != null && (value.stripTrailingZeros().scale() > scale
            || value.abs().compareTo(BigDecimal.TEN.pow(precision - scale)) >= 0)) {
            throw error(HttpStatus.BAD_REQUEST, "측정값의 소수점 또는 정수 자릿수를 확인해 주세요.");
        }
    }

    // 동일 요청의 수치 비교는 소수점 표현 자릿수 차이를 무시한다.
    private boolean same(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    // 입력·상태 오류에 알맞은 HTTP 응답을 만든다.
    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
