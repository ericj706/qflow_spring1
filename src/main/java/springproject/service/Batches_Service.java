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

        // 시작일
        LocalDateTime startAt = null;

        if (searchDto.getStartDate() != null) {

            startAt = searchDto
                    .getStartDate()
                    .atStartOfDay();
        }


        // 종료일
        LocalDateTime endAtExclusive = null;

        if (searchDto.getEndDate() != null) {

            endAtExclusive = searchDto
                    .getEndDate()
                    .plusDays(1)
                    .atStartOfDay();
        }


        // Repository 검색
        return br.search(
                startAt,
                endAtExclusive,
                searchDto.getProductCode(),
                searchDto.getStatus(),
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