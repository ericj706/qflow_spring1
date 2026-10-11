package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Filling_packaging_Dto;
import springproject.model.dto.chart.Chart_Dto;
import springproject.model.dto.chart.Dashboard_Stats_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Filling_packaging_SearchDto;
import springproject.service.Filling_packaging_Service;

@RestController @RequestMapping ("/mask/filling-packagings")
@RequiredArgsConstructor 
public class Filling_packaging_Controller {
    private final Filling_packaging_Service filling_packaging_Service;
    
     // 전체조회 + 조건검색 + 페이징
    @GetMapping
    public Page_response<Filling_packaging_Dto> findAll(
            @ModelAttribute Filling_packaging_SearchDto searchDto,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<Filling_packaging_Dto> result = filling_packaging_Service.search(searchDto, page);
        Map<String, Long> summary = filling_packaging_Service.getSummary();
        return Page_response.from(result, summary);
    }
    

    // 개별 조회 (pouch_id)
    @GetMapping("/{pouch_id}")
    public ResponseEntity<?> findOne(@PathVariable(name = "pouch_id") String pouch_id) {
        try {
            Filling_packaging_Dto dto = filling_packaging_Service.findOne(pouch_id);
            return ResponseEntity.ok(dto); 
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("조회 중 오류가 발생했습니다.");
        }
    }
    
    // 차트조회
    @GetMapping("/summary")
    public List<Chart_Dto> getChartSummary(
            @RequestParam(name = "groupBy", defaultValue = "daily") String groupBy,
            @RequestParam(name = "batchId", required = false, defaultValue = "") String batchId,
            @RequestParam(name = "startDate", required = false, defaultValue = "") String startDate,
            @RequestParam(name = "endDate", required = false, defaultValue = "") String endDate) {
        return filling_packaging_Service.getChartSummary(groupBy, batchId, startDate, endDate);
    }

    // 대시보드 KPI 통합 조회 (6개 API → 1개 쿼리)
    @GetMapping("/dashboard-stats")
    public Dashboard_Stats_Dto getDashboardStats(
            @RequestParam(name = "batchId") String batchId) {
        return filling_packaging_Service.getDashboardStats(batchId);
    }

    // 최종결과 및 비고 수정
    @PutMapping("/{pouch_id}/final-result")
    public ResponseEntity<?> updateResult(
            @PathVariable (name = "pouch_id") String pouch_id,
            @RequestBody Filling_packaging_Dto dto){
        try {
            Filling_packaging_Dto updatedDto = filling_packaging_Service.updateResult(pouch_id, dto);
            return ResponseEntity.ok(updatedDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("충진/포장 최종 결과 수정 중 오류가 발생했습니다.");
        }
    }

    // 최종결과 및 비고 삭제 
    @DeleteMapping("/{pouch_id}/final-result")
    public ResponseEntity<?> deleteResult(@PathVariable (name = "pouch_id") String pouch_id) {
        try {
            filling_packaging_Service.deleteResult(pouch_id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("충진/포장 최종 결과 삭제 중 오류가 발생했습니다.");
        }
    }


}
