package springproject.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Batches_Dto;
import springproject.model.dto.search.Batches_SearchDto;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Users_Repository;

@Service
@RequiredArgsConstructor
public class Batches_Service {

    private final Batches_Repository br;
    private final Users_Repository ur;


    // ==========================================
    // 전체조회 + 조건검색 + 페이징
    // ==========================================
    @Transactional(readOnly = true)
    public Page<Batches_Dto> search(
            Batches_SearchDto searchDto,
            Pageable pageable) {

        // ------------------------------------------
        // 조회 시작일
        // LocalDate → LocalDateTime
        // 예: 2023-01-01 → 2023-01-01 00:00:00
        // ------------------------------------------
        LocalDateTime startAt = null;

        if (searchDto.getStartDate() != null) {

            startAt = searchDto
                    .getStartDate()
                    .atStartOfDay();
        }


        // ------------------------------------------
        // 조회 종료일
        // 해당 날짜 하루 전체를 포함하기 위해
        // 다음날 00:00 이전까지 검색
        //
        // 예:
        // endDate = 2023-01-05
        // → endAtExclusive = 2023-01-06 00:00:00
        // ------------------------------------------
        LocalDateTime endAtExclusive = null;

        if (searchDto.getEndDate() != null) {

            endAtExclusive = searchDto
                    .getEndDate()
                    .plusDays(1)
                    .atStartOfDay();
        }


        // ------------------------------------------
        // Repository 조건검색 + 페이징
        // ------------------------------------------
        return br.search(
                startAt,
                endAtExclusive,

                // LOT 번호 정확히 일치
                searchDto.getBatchId(),

                // LOT 번호 또는 제품명 부분검색
                searchDto.getKeyword(),

                // 제품코드
                searchDto.getProductCode(),

                // 생산상태
                searchDto.getStatus(),

                // 담당자 번호
                searchDto.getUserId(),

                // 제조 탱크번호
                searchDto.getTankId(),

                // page, size
                pageable
        )
        .map(Batches_Dto::from);
    }


    // ==========================================
    // 단건 조회
    // ==========================================
    @Transactional(readOnly = true)
    public Batches_Dto findOne(String batchId) {

        return br.findById(batchId)
                .map(Batches_Dto::from)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "존재하지 않는 배치 ID"
                        )
                );
    }
}