package springproject.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.Process_execution_Finish_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.request.Process_execution_QcApproval_Dto;
import springproject.model.dto.request.Process_execution_Start_Dto;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.service.Process_execution_Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/mask/process-executions")
@RequiredArgsConstructor
public class Process_execution_Controller {

    private final Process_execution_Service ps;
    private final springproject.datagenerate.Simulation_Service simulationService;

    // 입력·담당관리자·공정 상태 오류를 사유와 함께 반환
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "공정 요청을 처리할 수 없습니다." : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
    }

    // 전체조회 + 조건검색 + 페이징
    @GetMapping
    public Page_response<Process_execution_Dto> findAll(
            @ModelAttribute Process_execution_SearchDto searchDto,
            @RequestParam(name = "page", defaultValue = "0") int page
    ) {
        Page<Process_execution_Dto> result = ps.search(searchDto, page);
        return Page_response.from(result, Map.of());
    }
    
    // PK 개별조회
    @GetMapping("/{execution_id}")
    public Process_execution_Dto findOne(@PathVariable(name = "execution_id") Long execution_id) {
        return ps.findOne(execution_id);
    }

    // 공정 시작시 저장
    @PostMapping("")
    public Process_execution_Dto save(
        @RequestBody Process_execution_Start_Dto request) {
        Process_execution_Dto result = ps.save(request);
        simulationService.connectStartedProcess(result);
        return result;
    }

    // 공정 종료 
    @PatchMapping ("/{execution_id}/finish")
    public Process_execution_Finish_Dto finish(@PathVariable(name = "execution_id") Long execution_id,
            @RequestHeader(name = "X-Simulation-Request", defaultValue = "false") boolean simulationRequest){
        Process_execution_Finish_Dto result = ps.finish(execution_id);
        // 생성기의 자체 요청은 응답 수신 후 반영하므로 중복 콜백과 잠금 대기를 피한다.
        if (!simulationRequest) simulationService.connectCompletedProcess(result);
        return result;
    }

    // 알람 처리 후 다음 공정 진행 여부 재검사
    @PatchMapping("/{execution_id}/resume")
    public Process_execution_Finish_Dto resume(
            @PathVariable(name = "execution_id") Long executionId,
            @RequestHeader(name = "X-Simulation-Request", defaultValue = "false") boolean simulationRequest) {
        Process_execution_Finish_Dto result = ps.resume(executionId);
        if (!simulationRequest) simulationService.connectCompletedProcess(result);
        return result;
    }

    // 벌크 검사 최종 결과 승인
    @PatchMapping("/{execution_id}/qc-approval")
    public Process_execution_Dto approveBulkQc(
            @PathVariable(name = "execution_id") Long executionId,
            @RequestBody Process_execution_QcApproval_Dto request) {
        return ps.approveBulkQc(executionId, request);
    }

    /// 로그인한 4·5공정관리자의 벌크 불합격 LOT 폐기 확정
    @PatchMapping("/{execution_id}/discard")
    public Process_execution_Finish_Dto discardBulkQc(
            @PathVariable(name = "execution_id") Long executionId,
            HttpServletRequest request) {
        // 로그인 세션 조회
        HttpSession session = request.getSession(false);
        Object sessionUserId = session == null ? null : session.getAttribute("LOGIN_USER_ID");
        if (!(sessionUserId instanceof Integer)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인 후 폐기를 진행해 주세요.");
        }

        Integer loginUserId = (Integer) sessionUserId;
        // DB 폐기 처리 — 서비스 트랜잭션 완료 후 결과 반환
        Process_execution_Finish_Dto result = ps.discardBulkQc(executionId, loginUserId);
        // 생성기에도 폐기 결과 반영
        simulationService.connectCompletedProcess(result);
        return result;
    }
    
}
