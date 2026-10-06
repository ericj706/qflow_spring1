package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Filling_packaging_Dto;
import springproject.model.dto.chart.Chart_Dto;
import springproject.model.dto.search.Filling_packaging_SearchDto;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.repository.Filling_packaging_Repository;

@Service 
@RequiredArgsConstructor 
public class Filling_packaging_Service {
    private final Filling_packaging_Repository filling_packaging_Repository;

    // 전체조회 + 조건검색 + 페이징
    public Page<Filling_packaging_Dto> search(
            Filling_packaging_SearchDto searchDto,
            int page) {
        // 페이지 번호 확인
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }

        // 검색조건 꺼내기
        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        String batchId = searchDto.getBatchId();
        String packagingLine = searchDto.getPackagingLine();
        String finalDisposition = searchDto.getFinalDisposition();
        String checkweigherStatus = searchDto.getCheckweigherStatus();
        String metalDetectorStatus = searchDto.getMetalDetectorStatus();
        String visionInspectionStatus = searchDto.getVisionInspectionStatus();
        Integer userId = searchDto.getUserId();

        // 검색기간 확인
        if (startDate != null&& endDate != null&& startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");
        }

        // 종료일에 하루를 더할 수 있는지 확인
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"종료일의 범위를 확인해 주세요.");
        }

        // 담당자 번호 확인
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당자 번호는 1 이상이어야 합니다.");
        }

        // 문자열 앞뒤 공백 제거
        // 빈 문자열은 검색조건에서 제외하도록 null 처리
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) {batchId = null;}
        }
        if (packagingLine != null) {
            packagingLine = packagingLine.trim();
            if (packagingLine.isEmpty()) {packagingLine = null;}
        }
        if (finalDisposition != null) {
            finalDisposition = finalDisposition.trim();
            if (finalDisposition.isEmpty()) {finalDisposition = null;}
        }
        if (checkweigherStatus != null) {
            checkweigherStatus = checkweigherStatus.trim();
            if (checkweigherStatus.isEmpty()) {checkweigherStatus = null;}
        }
        if (metalDetectorStatus != null) {
            metalDetectorStatus = metalDetectorStatus.trim();
            if (metalDetectorStatus.isEmpty()) {metalDetectorStatus = null;}
        }
        if (visionInspectionStatus != null) {
            visionInspectionStatus = visionInspectionStatus.trim();
            if (visionInspectionStatus.isEmpty()) {visionInspectionStatus = null;}
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
        Page<Filling_packaging_Entity> result =
                filling_packaging_Repository.search(
                        startAt,
                        endAtExclusive,
                        batchId,
                        packagingLine,
                        finalDisposition,
                        checkweigherStatus,
                        metalDetectorStatus,
                        visionInspectionStatus,
                        userId,
                        pageable
                );

        // 페이지 정보를 유지하면서 Entity → DTO 변환
        return result.map(Filling_packaging_Dto::from);
    }

    // 개별조회
    public Filling_packaging_Dto findOne(String pouch_id){
        Optional<Filling_packaging_Entity> optional = filling_packaging_Repository.findById(pouch_id);
        if(optional.isPresent()){
            Filling_packaging_Entity entity = optional.get();
            return Filling_packaging_Dto.from(entity);
        }
        return null;
    }

    // 차트조회
    public List<Chart_Dto> getChartSummary(String groupBy, String batchId, String startDate, String endDate) {
        // if ((startDate == null || startDate.trim().isEmpty()) && 
        //     (endDate == null || endDate.trim().isEmpty())) {
        //     LocalDate today = LocalDate.now();
        //     endDate = today.toString();
        //     startDate = today.minusDays(30).toString(); // 처음 페이지 진입시 30일단위만 차트 조회
        // }
        
        if ("15min".equalsIgnoreCase(groupBy) || "15분별".equalsIgnoreCase(groupBy)) {
            if (batchId == null || batchId.isEmpty()) {
                return Collections.emptyList(); // batchId가 없으면 빈 리스트 반환
            }
            return filling_packaging_Repository.find15minSummary(batchId, startDate, endDate);
        } else if ("hourly".equalsIgnoreCase(groupBy)) {
            return filling_packaging_Repository.findHourlySummary(startDate, endDate);
        } else if ("lot".equalsIgnoreCase(groupBy)) {
            return filling_packaging_Repository.findLotSummary(startDate, endDate);
        } else {
            return filling_packaging_Repository.findDailySummary(startDate, endDate);
        }
    }
        
}
