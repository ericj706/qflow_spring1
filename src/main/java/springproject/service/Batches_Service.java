package springproject.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Batches_Dto;
import springproject.model.dto.search.Batches_SearchDto;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Users_Entity;
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
                        () -> new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "존재하지 않는 배치 ID")
                );
    }

    // ==========================================
    // LOT 등록
    // ==========================================
    @Transactional
    public Batches_Dto save(Batches_Dto dto) {
        if (dto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"등록할 내용을 입력해 주세요.");
        }
        if (dto.getStatus() != null && "폐기".equals(dto.getStatus().trim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"폐기 상태로 LOT를 등록할 수 없습니다.");
        }

    // 1. 동일한 LOT 번호가 이미 존재하는지 확인
       if (br.existsById(dto.getBatchId())) {
            throw new IllegalArgumentException(
                "이미 존재하는 배치 ID입니다."
        );
   }

   // 2. 담당자 조회
   Users_Entity usersEntity = null;

   if (dto.getUserId() != null) {
        usersEntity = ur.findById(dto.getUserId())
                .orElseThrow(
                    () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "존재하지 않는 담당자입니다. users 테이블과 작업자 번호를 확인해 주세요.")
                );
}

   // 3. DTO → Entity 변환
   Batches_Entity entity = dto.toEntity(usersEntity);

   // 4. DB 저장
   Batches_Entity savedEntity = br.save(entity);

   // 5. Entity → DTO 변환 후 반환
   return Batches_Dto.from(savedEntity);
   }
   // ==========================================
   // LOT 수정
   // ==========================================
   @Transactional
   public Batches_Dto update(String batchId, Batches_Dto dto) {
        if(dto== null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정할 내용을 입력해 주세요.");
        }
        // 폐기 처리와 동시에 수정되지 않도록 같은 LOT 잠금 사용
        Batches_Entity entity = br.findByIdForUpdate(batchId).orElse(null);
        if (entity == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"존재하지 않는 LOT입니다.");
        }

        // 이미 폐끼된 LOT 담당자 / 상태를 포함한 정보 변경 금지
        if("폐기".equals(entity.getStatus())){
                throw new ResponseStatusException(HttpStatus.CONFLICT,"페기된 LOT는 수정할 수 없습니다.");
        }
        // 일반 수정 기능으로 폐기 처리하는 것은 금지
        if(dto.getStatus() != null && "폐기".equals(dto.getStatus().trim())){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "LOT 폐기는 확정 기능 이용해 주세요.");
        }
        // 담당자 조회
        Users_Entity users_Entity = null;
        if(dto.getUserId() != null){
                users_Entity = ur.findById(dto.getUserId()).orElse(null);
                if(users_Entity == null){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 담당자 입니다.");}
        }
        // 기존 LOT 정보 수정
        entity.setProductCode(dto.getProductCode());
        entity.setProductName(dto.getProductName());
        entity.setTargetBulkKg(dto.getTargetBulkKg());
        entity.setActualBulkKg(dto.getActualBulkKg());
        entity.setTargetUnits(dto.getTargetUnits());
        entity.setActualUnits(dto.getActualUnits());
        entity.setDefectUnits(dto.getDefectUnits());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setStatus(dto.getStatus());
        entity.setUsersEntity(users_Entity);
        entity.setTankId(dto.getTankId());
        entity.setRecordSource(dto.getRecordSource());

        return Batches_Dto.from(br.save(entity));


  }


}
