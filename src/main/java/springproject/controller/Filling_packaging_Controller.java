package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import springproject.model.dto.Filling_packaging_Dto;
import springproject.model.dto.chart.Chart_Dto;
import springproject.model.dto.chart.Dashboard_Stats_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.request.Filling_packaging_Result_Dto;
import springproject.model.dto.request.Filling_packaging_Save_Dto;
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
        Map<String, Long> summary = filling_packaging_Service.getSummary(searchDto);
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

    // 포장·검사 제품 신규 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Filling_packaging_Dto save(@RequestBody Filling_packaging_Save_Dto request) {
        return filling_packaging_Service.save(request);
    }

    // 제품의 검사 결과 수정
    @PatchMapping("/{pouch_id}/result")
    public Filling_packaging_Dto updateResult(
            @PathVariable(name = "pouch_id") String pouchId,
            @RequestBody Filling_packaging_Result_Dto request) {
        return filling_packaging_Service.updateResult(pouchId, request);
    }

    // 로그인한 4·5공정 관리자의 제품별 폐기 확정
    @PatchMapping("/{pouch_id}/discard")
    public Filling_packaging_Dto discardProduct(
            @PathVariable(name = "pouch_id") String pouchId,
            HttpServletRequest request) {
        // 기존 로그인 세션 조회
        HttpSession session = request.getSession(false);
        Object sessionUserId = session == null ? null : session.getAttribute("LOGIN_USER_ID");
        if (!(sessionUserId instanceof Integer)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인 후 제품 폐기를 진행해 주세요.");
        }
        Integer loginUserId = (Integer) sessionUserId;

        // 서비스에서 권한·제품 판정·공정 상태를 확인한 뒤 폐기 확정
        return filling_packaging_Service.discardProduct(pouchId,loginUserId);
    }
    
    // 공정 실행번호별 제품 목록
    @GetMapping("/executions/{executionId}")
    public Page_response<Filling_packaging_Dto> findByExecutionId(
            @PathVariable(name = "executionId") Long executionId,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<Filling_packaging_Dto> result = filling_packaging_Service.findByExecutionId(executionId,page);
        return Page_response.from(result, Map.of());
    }

}
