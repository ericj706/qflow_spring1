package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // 2) 전체조회
        public List<Anomaly_event_Dto> search(Anomaly_event_SearchDto searchDto){
            LocalDate startDate = searchDto.getStartDate();
            LocalDate endDate = searchDto.getEndDate();

            // 기간 검증
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");}

            // 담당자 번호 검증
            Integer userId = searchDto.getUserId();
            if (userId != null && userId <= 0) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당자 번호는 1 이상이어야 합니다.");}
            
             // 심각도
            String severity = searchDto.getSeverity();
            if (severity != null) {severity = severity.trim();
                if (severity.isEmpty()) {severity = null;}}

            // 조치상태
            String actionStatus = searchDto.getActionStatus();
            if (actionStatus != null) {actionStatus = actionStatus.trim();
                if (actionStatus.isEmpty()) {actionStatus = null;}}

            // LOT 번호 검색어
            String batchIdKeyword = searchDto.getBatchIdKeyword();
            if (batchIdKeyword != null) {batchIdKeyword = batchIdKeyword.trim();
                if (batchIdKeyword.isEmpty()) {batchIdKeyword = null;}}
    
            // 공정코드
            String processCode = searchDto.getProcessCode();
            if (processCode != null) {processCode = processCode.trim();
                if (processCode.isEmpty()) {processCode = null;}}

            // 이상 유형
            String anomalyType = searchDto.getAnomalyType();
            if (anomalyType != null) {anomalyType = anomalyType.trim();
                if (anomalyType.isEmpty()) {anomalyType = null;}}

            // 시작일 00:00 이상
            LocalDateTime startAt = null;
            if (startDate != null) {startAt = startDate.atStartOfDay();}

            // 종료일 다음 날 00:00 미만
            // 종료일 당일의 모든 시간을 포함
            LocalDateTime endAtExclusive = null;
            if (endDate != null) {endAtExclusive = endDate.plusDays(1).atStartOfDay();}

            // Repository 조건검색 호출
            List<Anomaly_event_Entity> entities = anomaly_event_Repository.search(
                                                    startAt,
                                                    endAtExclusive,
                                                    severity,
                                                    actionStatus,
                                                    batchIdKeyword,
                                                    processCode,
                                                    anomalyType,
                                                    userId
                                            );

            // Entity 목록 → DTO 목록
            return entities.stream().map(Anomaly_event_Dto::from).toList();
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
