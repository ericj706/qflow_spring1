package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.request.Anomaly_event_Action_Dto;
import springproject.model.dto.request.Anomaly_event_Confirm_Dto;
import springproject.model.dto.search.Anomaly_event_SearchDto;
import springproject.service.Anomaly_event_Service;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mask/anomaly-events")
public class Anomaly_event_Controller {

    private final Anomaly_event_Service anomaly_event_Service;

    // 1. 전체조회 + 조건검색 + 페이징 (기존 기능 유지)
    @GetMapping("")
    public Page_response<Anomaly_event_Dto> findaAll(
            @ModelAttribute Anomaly_event_SearchDto searchDto,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<Anomaly_event_Dto> result = anomaly_event_Service.search(searchDto, page);
        return Page_response.from(result, Map.of());
    }

    // 2. 팝업 전용 조회: 세션을 Service에 전달해 로그인/담당 공정을 검사
    @GetMapping(value = "/popup", produces = "application/json")
    public List<Anomaly_event_Dto> getPopupAnomalies(HttpSession session) {
        return anomaly_event_Service.findActivePopupAnomalies(session);
    }

    // 3. 개별조회 (기존 기능 유지)
    @GetMapping("/{anomaly_id}")
    public Anomaly_event_Dto findOne(@PathVariable(name = "anomaly_id") Long anomaly_id) {
        return anomaly_event_Service.findOne(anomaly_id);
    }

    // 4. 공정 실행별 알림 조회 (기존 기능 유지)
    @GetMapping("/executions/{executionId}")
    public List<Anomaly_event_Dto> findByExecutionId(
            @PathVariable(name = "executionId") Long executionId) {
        return anomaly_event_Service.findByExecutionId(executionId);
    }

    // 5. 알림 확인: 세션 사용자와 수동 입력 담당자를 Service에서 검증
    @PatchMapping("/{anomalyId}/confirm")
    public Anomaly_event_Dto confirm(
            @PathVariable(name = "anomalyId") Long anomalyId,
            @RequestBody Anomaly_event_Confirm_Dto request,
            HttpSession session) {
        return anomaly_event_Service.confirm(anomalyId, request, session);
    }

    // 6. 심각 알림 조치 완료: 기존 조치 로직 + 세션 권한 검증
    @PatchMapping("/{anomalyId}/action")
    public Anomaly_event_Dto completeAction(
            @PathVariable(name = "anomalyId") Long anomalyId,
            @RequestBody Anomaly_event_Action_Dto request,
            HttpSession session) {
        return anomaly_event_Service.completeAction(anomalyId, request, session);
    }
}
