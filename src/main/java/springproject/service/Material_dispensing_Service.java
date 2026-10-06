package springproject.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Material_dispensing_Dto;
import springproject.model.dto.search.Material_dispensing_SearchDto;
import springproject.model.repository.Material_dispensing_Repository;

@Service
@RequiredArgsConstructor
public class Material_dispensing_Service {

    private final Material_dispensing_Repository mr;


    // ==========================================
    // 기존 전체조회
    // ==========================================
    @Transactional(readOnly = true)
    public List<Material_dispensing_Dto> findAll() {

        return mr.findAll().stream()
                .map(Material_dispensing_Dto::from)
                .toList();
    }


    // ==========================================
    // 전체조회 + 조건검색 + 페이징
    // ==========================================
    @Transactional(readOnly = true)
    public Page<Material_dispensing_Dto> search(
            Material_dispensing_SearchDto searchDto,
            Pageable pageable) {

        // ------------------------------------------
        // 시작일
        // LocalDate -> LocalDateTime
        // ------------------------------------------
        LocalDateTime startAt = null;

        if (searchDto.getStartDate() != null) {

            startAt = searchDto
                    .getStartDate()
                    .atStartOfDay();
        }


        // ------------------------------------------
        // 종료일
        // 해당 날짜 전체를 포함하기 위해
        // 다음날 00:00 이전까지 조회
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
        return mr.search(
                startAt,
                endAtExclusive,

                // 생산 LOT 번호
                searchDto.getBatchId(),

                // 원료코드
                searchDto.getMaterialCode(),

                // 원료명
                searchDto.getMaterialName(),

                // 원료 LOT 번호
                searchDto.getRawMaterialLot(),

                // 칭량상태
                searchDto.getStatus(),

                // 담당자
                searchDto.getUserId(),

                // 페이징
                pageable
        )
        .map(Material_dispensing_Dto::from);
    }


    // ==========================================
    // 단건 조회
    // ==========================================
    @Transactional(readOnly = true)
    public Material_dispensing_Dto findOne(String dispenseId) {

        return mr.findById(dispenseId)
                .map(Material_dispensing_Dto::from)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "존재하지 않는 ID"
                        )
                );
    }
}