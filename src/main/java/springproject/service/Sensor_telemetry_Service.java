package springproject.service;

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
}
