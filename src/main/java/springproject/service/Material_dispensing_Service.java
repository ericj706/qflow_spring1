package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Material_dispensing_Dto;
import springproject.model.dto.search.Material_dispensing_SearchDto;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Material_dispensing_Entity;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Material_dispensing_Repository;
import springproject.model.repository.Users_Repository;


@Service 
@RequiredArgsConstructor 
@Transactional(readOnly = true)
public class Material_dispensing_Service {
    private final Material_dispensing_Repository material_dispensing_Repository;
    
    private final Batches_Repository br;
    private final Users_Repository ur;

    // 전체조회 + 조건검색 + 페이징
    public Page<Material_dispensing_Dto> search(
            Material_dispensing_SearchDto searchDto,
            int page
    ) {
        // 페이지 번호 검증
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"페이지 번호는 0 이상이어야 합니다.");
        }

        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        // 시작일과 종료일 검증
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");
        }

        // 종료일에 하루를 더할 수 있는지 검증
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"종료일 범위를 확인해 주세요.");
        }

        // 담당자 번호 검증
        Integer userId = searchDto.getUserId();
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당자 번호는 1 이상이어야 합니다.");
        }

        // 생산 LOT 번호
        String batchId = searchDto.getBatchId();
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) {batchId = null;}
        }

        // 원료코드
        String materialCode = searchDto.getMaterialCode();
        if (materialCode != null) {
            materialCode = materialCode.trim();
            if (materialCode.isEmpty()) {materialCode = null;}
        }

        // 원료명
        String materialName = searchDto.getMaterialName();
        if (materialName != null) {
            materialName = materialName.trim();
            if (materialName.isEmpty()) {materialName = null;}
        }

        // 원료 LOT 번호
        String rawMaterialLot = searchDto.getRawMaterialLot();
        if (rawMaterialLot != null) {
            rawMaterialLot = rawMaterialLot.trim();
            if (rawMaterialLot.isEmpty()) {rawMaterialLot = null;}
        }

        // 칭량상태
        String status = searchDto.getStatus();
        if (status != null) {
            status = status.trim();
            if (status.isEmpty()) {status = null;}
        }

        // 시작일 00:00 이상
        LocalDateTime startAt = null;
        if (startDate != null) {
            startAt = startDate.atStartOfDay();
        }

        // 종료일 다음 날 00:00 미만
        // 종료일 당일의 모든 시간을 포함
        LocalDateTime endAtExclusive = null;
        if (endDate != null) {
            endAtExclusive = endDate.plusDays(1).atStartOfDay();}

        // 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY 사용
        Pageable pageable = PageRequest.of(page, 20);

        // 현재 페이지에 해당하는 데이터만 조회
        Page<Material_dispensing_Entity> entities =
            material_dispensing_Repository.search(
                        startAt,
                        endAtExclusive,
                        batchId,
                        materialCode,
                        materialName,
                        rawMaterialLot,
                        status,
                        userId,
                        pageable
                );

        // Entity → DTO 변환
        // 전체 건수와 페이지 정보는 유지
        return entities.map(Material_dispensing_Dto::from);
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
        return material_dispensing_Repository.search(
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
        return material_dispensing_Repository.findById(dispenseId)
                .map(Material_dispensing_Dto::from)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "존재하지 않는 ID"
                        )
                );
    }

    // ==========================================
    // 원료 칭량 등록
    // ==========================================
    @Transactional
    public Material_dispensing_Dto save(Material_dispensing_Dto dto) {

        // 칭량번호 중복 확인
        if (material_dispensing_Repository.existsById(dto.getDispenseId())) {
            throw new IllegalArgumentException(
                    "이미 존재하는 칭량 ID입니다."
            );
        }

        // 생산 LOT 조회
        Batches_Entity batchesEntity =
                br.findById(dto.getBatchId())
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 생산 LOT입니다."
                                )
                        );

        // 담당자 조회
        Users_Entity usersEntity = null;

        if (dto.getUserId() != null) {
            usersEntity =
                    ur.findById(dto.getUserId())
                            .orElseThrow(
                                    () -> new IllegalArgumentException(
                                            "존재하지 않는 담당자입니다."
                                    )
                            );
        }

        // DTO → Entity
        Material_dispensing_Entity entity =
                dto.toEntity(batchesEntity, usersEntity);

        // DB 저장
        Material_dispensing_Entity savedEntity =
                material_dispensing_Repository.save(entity);

        // Entity → DTO
        return Material_dispensing_Dto.from(savedEntity);
    }

    // ==========================================
    // 원료 칭량 수정
    // ==========================================
    @Transactional
    public Material_dispensing_Dto update(
            String dispenseId,
            Material_dispensing_Dto dto) {

        // 1. 수정할 원료 칭량 데이터 조회
        Material_dispensing_Entity entity =
                material_dispensing_Repository.findById(dispenseId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 칭량 ID입니다."
                                )
                        );

        // 2. 생산 LOT 조회
        Batches_Entity batchesEntity =
                br.findById(dto.getBatchId())
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 생산 LOT입니다."
                                )
                        );

        // 3. 담당자 조회
        Users_Entity usersEntity = null;

        if (dto.getUserId() != null) {
            usersEntity =
                    ur.findById(dto.getUserId())
                            .orElseThrow(
                                    () -> new IllegalArgumentException(
                                            "존재하지 않는 담당자입니다."
                                    )
                            );
        }

        // 4. 기존 데이터 수정
        entity.setBatchesEntity(batchesEntity);
        entity.setMaterialCode(dto.getMaterialCode());
        entity.setMaterialName(dto.getMaterialName());
        entity.setRawMaterialLot(dto.getRawMaterialLot());
        entity.setTargetQtyKg(dto.getTargetQtyKg());
        entity.setActualQtyKg(dto.getActualQtyKg());
        entity.setUsersEntity(usersEntity);
        entity.setDispensedAt(dto.getDispensedAt());
        entity.setStatus(dto.getStatus());

        // 5. DB 저장
        Material_dispensing_Entity updatedEntity =
                material_dispensing_Repository.save(entity);

        return Material_dispensing_Dto.from(updatedEntity);
    }
}