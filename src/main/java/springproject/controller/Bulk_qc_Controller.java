package springproject.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.PutExchange;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Bulk_qc_SearchDto;
import springproject.service.Bulk_qc_Service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController @RequestMapping ("/mask/bulk-qc")
@RequiredArgsConstructor 

public class Bulk_qc_Controller {
    private final Bulk_qc_Service bulk_qc_Service;

    // 전체조회 + 조건검색 + 페이징 + 합/불집계
    @GetMapping
    public Page_response<Bulk_qc_Dto> findAll(
            @ModelAttribute Bulk_qc_SearchDto searchDto,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<Bulk_qc_Dto> result = bulk_qc_Service.search(searchDto, page);
        Map<String, Long> summary = bulk_qc_Service.getSummary();
        return Page_response.from(result, summary);
    }
    
    // 개별조회
    @GetMapping("/{qc_id}")
    public ResponseEntity<?> findOne(@PathVariable(name = "qc_id") String qc_id) {
        try {
            Bulk_qc_Dto dto = bulk_qc_Service.findOne(qc_id);
            return ResponseEntity.ok(dto); // PASS든 FAIL이든 ID만 맞으면 무조건 200 OK 반환
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("존재하지 않는 ID입니다.");
        }
    }
    

    // 최종 검사결과 및 비고 수정
    @PatchMapping("/{qc_id}/result")
    public ResponseEntity<?> updateQcResult(
            @PathVariable(name = "qc_id") String qc_id,
            @RequestBody Bulk_qc_Dto dto) {
        bulk_qc_Service.updateQcResult(qc_id, dto);
        return ResponseEntity.ok("성공적으로 변경되었습니다.");
    }
    
    // 삭제
    @DeleteMapping ("/{qc_id}/result")
    public ResponseEntity<?> deleteResult(@PathVariable ("qc_id") String qc_id) {
        try {
            Bulk_qc_Dto updatedDto = bulk_qc_Service.deleteResult(qc_id);
            return ResponseEntity.ok(updatedDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("초기화 처리 중 오류 발생 :" +e.getMessage());
        }
    }
    
}
