package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Data_change_log_Dto;
import springproject.model.dto.search.Data_change_log_SearchDto;
import springproject.model.entity.Data_change_log_Entity;
import springproject.model.repository.Data_change_log_Repository;

@Service @RequiredArgsConstructor 
@Transactional (readOnly = true)
public class Data_change_log_Service {
    private final Data_change_log_Repository data_change_log_Repository;
    // 1) 개별조회
    public Data_change_log_Dto findOne(Long change_id){
        Optional<Data_change_log_Entity> optional = data_change_log_Repository.findById(change_id);
        if(optional.isPresent()){
            Data_change_log_Entity entity = optional.get();
            return Data_change_log_Dto.from(entity);
        }
        return null;
    }

    // 2) 전체조회 + 조건검색 + 페이징
    public Page<Data_change_log_Dto> search(
            Data_change_log_SearchDto searchDto,
            int page) {
        // 페이지 번호 확인
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"페이지 번호는 0 이상이어야 합니다.");
        }

        // 검색조건 꺼내기
        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        String tableName = searchDto.getTableName();
        String recordId = searchDto.getRecordId();
        String recordIdKeyword = searchDto.getRecordIdKeyword();
        String columnName = searchDto.getColumnName();
        String changeType = searchDto.getChangeType();
        Integer userId = searchDto.getUserId();

        // 검색기간 확인
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");
        }

        // 종료일에 하루를 더할 수 있는지 확인
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"종료일의 범위를 확인해 주세요.");
        }

        // 작업자 번호 확인
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"작업자 번호는 1 이상이어야 합니다.");
        }

        // 앞뒤 공백 제거
        // 빈 문자열은 검색조건에서 제외하도록 null 처리
        if (tableName != null) {
            tableName = tableName.trim();
            if (tableName.isEmpty()) {tableName = null;}
        }

        if (recordId != null) {
            recordId = recordId.trim();
            if (recordId.isEmpty()) {recordId = null;}
        }

        if (recordIdKeyword != null) {
            recordIdKeyword = recordIdKeyword.trim();
            if (recordIdKeyword.isEmpty()) {recordIdKeyword = null;}
        }

        if (columnName != null) {
            columnName = columnName.trim();
            if (columnName.isEmpty()) {columnName = null;}
        }

        if (changeType != null) {
            changeType = changeType.trim();
            if (changeType.isEmpty()) {changeType = null;
            } else {
                // update 등으로 입력해도 UPDATE로 검색
                changeType = changeType.toUpperCase(Locale.ROOT);
                if (!"등록".equals(changeType) && !"수정".equals(changeType) && !"삭제".equals(changeType)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,"변경 유형은 INSERT, UPDATE, DELETE 중 하나여야 합니다."
                    );
                }
            }
        }

        // 시작일 당일 00:00 이상
        LocalDateTime startAt = null;
        if (startDate != null) {startAt = startDate.atStartOfDay();}

        // 종료일 다음 날 00:00 미만
        // 종료일 당일의 모든 시간을 포함
        LocalDateTime endAtExclusive = null;
        if (endDate != null) {endAtExclusive = endDate.plusDays(1).atStartOfDay();}

        // 한 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY에서 처리
        Pageable pageable = PageRequest.of(page, 20);
        Page<Data_change_log_Entity> result =
                data_change_log_Repository.search(
                        startAt,
                        endAtExclusive,
                        tableName,
                        recordId,
                        recordIdKeyword,
                        columnName,
                        changeType,
                        userId,
                        pageable
                );

        // 페이지 정보를 유지하면서 Entity → DTO 변환
        return result.map(Data_change_log_Dto::from);
    }


}
