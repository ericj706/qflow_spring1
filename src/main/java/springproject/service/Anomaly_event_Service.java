package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.model.dto.search.Anomaly_event_SearchDto;
import springproject.model.entity.Anomaly_event_Entity;
import springproject.model.repository.Anomaly_event_Repository;

@Service @RequiredArgsConstructor 
@Transactional (readOnly = true)
public class Anomaly_event_Service {
    private final Anomaly_event_Repository anomaly_event_Repository;

    // 전체조회 + 조건검색 + 페이징
    public Page<Anomaly_event_Dto> search(
            Anomaly_event_SearchDto searchDto,
            int page) {

        // 페이지 번호 확인
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"페이지 번호는 0 이상이어야 합니다.");
        }

        // 검색조건 꺼내기
        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        String severity = searchDto.getSeverity();
        String actionStatus = searchDto.getActionStatus();
        String batchId = searchDto.getBatchId();
        String batchIdKeyword = searchDto.getBatchIdKeyword();
        String processCode = searchDto.getProcessCode();
        String anomalyType = searchDto.getAnomalyType();
        Integer userId = searchDto.getUserId();

        // 검색기간 확인
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");
        }

        // 종료일에 하루를 더할 수 있는지 확인
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"종료일의 범위를 확인해 주세요.");
        }

        // 담당자 번호 확인
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"담당자 번호는 1 이상이어야 합니다.");
        }

        // 심각도
        if (severity != null) {
            severity = severity.trim();
            if (severity.isEmpty()) {severity = null;}
        }

        // 조치상태
        if (actionStatus != null) {
            actionStatus = actionStatus.trim();
            if (actionStatus.isEmpty()) {actionStatus = null;}
        }

        // LOT 번호: 정확히 일치하는 검색
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) {batchId = null;}
        }

        // LOT 번호 검색어: 기존 부분 검색
        if (batchIdKeyword != null) {
            batchIdKeyword = batchIdKeyword.trim();
            if (batchIdKeyword.isEmpty()) {batchIdKeyword = null;}
        }

        // 공정코드
        if (processCode != null) {
            processCode = processCode.trim();
            if (processCode.isEmpty()) {processCode = null;}
        }

        // 이상 유형
        if (anomalyType != null) {
            anomalyType = anomalyType.trim();
            if (anomalyType.isEmpty()) {anomalyType = null;}
        }

        // 시작일 당일 00:00 이상
        LocalDateTime startAt = null;
        if (startDate != null) {startAt = startDate.atStartOfDay();}

        // 종료일 다음 날 00:00 미만
        // 종료일 당일의 모든 시간을 포함
        LocalDateTime endAtExclusive = null;
        if (endDate != null) {endAtExclusive = endDate.plusDays(1).atStartOfDay();}

        // 한 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY 사용
        Pageable pageable = PageRequest.of(page, 20);
        Page<Anomaly_event_Entity> result =
                anomaly_event_Repository.search(
                        startAt,
                        endAtExclusive,
                        severity,
                        actionStatus,
                        batchId,
                        batchIdKeyword,
                        processCode,
                        anomalyType,
                        userId,
                        pageable
                );

        // 페이지 정보를 유지하면서 Entity → DTO 변환
        return result.map(Anomaly_event_Dto::from);
    }

    // 1) 개별조회
    public Anomaly_event_Dto findOne( Long anomaly_id){
        Optional<Anomaly_event_Entity> optional = anomaly_event_Repository.findById(anomaly_id);
        if (optional.isPresent()) {
            Anomaly_event_Entity entity = optional.get();
            return Anomaly_event_Dto.from(entity);
        }return null;
    }

    
}
