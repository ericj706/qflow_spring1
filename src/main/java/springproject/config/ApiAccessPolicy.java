package springproject.config;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.server.ResponseStatusException;
import springproject.model.dto.Material_dispensing_Dto;
import springproject.model.dto.Sensor_telemetry_Dto;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.model.dto.request.Filling_packaging_Save_Dto;
import springproject.model.dto.request.Filling_packaging_Result_Dto;
import springproject.model.dto.request.Process_execution_QcApproval_Dto;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Process_execution_Repository;
import springproject.model.repository.Users_Repository;

/** 모든 업무 API의 인증과 공정별 변경 권한을 한 곳에서 검사한다. */
@ControllerAdvice(basePackages = "springproject.controller")
@RequiredArgsConstructor
public class ApiAccessPolicy extends RequestBodyAdviceAdapter implements HandlerInterceptor {
    private static final String ACTOR = ApiAccessPolicy.class.getName() + ".actor";
    private static final String INTERNAL = ApiAccessPolicy.class.getName() + ".internal";
    private static final Set<String> ROLES = Set.of("최고관리자", "1공정관리자", "2공정관리자", "3공정관리자", "4공정관리자", "5공정관리자");
    private static final Map<String, String> PROCESS_ROLES = Map.of(
        "원료 칭량", "1공정관리자", "가열/혼합", "2공정관리자", "냉각", "3공정관리자",
        "벌크 검사", "4공정관리자", "최종 포장 검사", "5공정관리자");
    private final Users_Repository users;
    private final Process_execution_Repository executions;
    private final SimulationRequestAuth simulationAuth;
    @Value("${app.frontend-origin:http://localhost:5173}")
    private String frontendOrigin;

    // 세션의 번호로 매 요청마다 DB를 확인한다. 브라우저의 role·userId는 권한 근거로 사용하지 않는다.
    public Users_Entity requireUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute("LOGIN_USER_ID");
        if (!(id instanceof Integer userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        Users_Entity user = users.findById(userId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 계정이 없습니다."));
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getRole() == null || !ROLES.contains(user.getRole())) forbidden();
        return user;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equals(request.getMethod())) return true;
        if (!(handler instanceof HandlerMethod method)) return true;
        String controller = method.getBeanType().getSimpleName();
        String operation = method.getMethod().getName();
        // 공개 X-Simulation-Request 헤더만으로 인증을 생략하지 않는다.
        if (simulationAuth.authenticate(request)) {
            if (!internalOperation(controller, operation)) forbidden();
            request.setAttribute(INTERNAL, true);
            return true;
        }
        // 쿠키가 있는 브라우저의 타 출처 변경 요청도 서버에서 거절한다.
        String origin = request.getHeader("Origin");
        boolean readOnly = Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
        if (!readOnly && origin != null && !origin.equals(frontendOrigin)) forbidden();
        if (controller.equals("Users_Controller") && operation.equals("login") && request.getMethod().equals("POST")) return true;
        Users_Entity user = requireUser(request);
        request.setAttribute(ACTOR, user);
        String role = user.getRole();
        if (controller.equals("Anomaly_rule_Controller")) {
            allow(role, "최고관리자");
            return true;
        }
        if (controller.equals("Simulation_Controller") && !operation.equals("status")) {
            allow(role, "최고관리자");
            return true;
        }
        if (request.getMethod().equals("GET") || request.getMethod().equals("HEAD")) return true;

        switch (controller) {
            case "Users_Controller" -> { if (!operation.equals("logout")) forbidden(); }
            case "Batches_Controller" -> allow(role, "최고관리자", "1공정관리자");
            case "Material_dispensing_Controller" -> allow(role, "1공정관리자");
            case "Sensor_telemetry_Controller" -> allow(role, "2공정관리자", "3공정관리자");
            case "Bulk_qc_Controller" -> allow(role, "4공정관리자");
            case "Filling_packaging_Controller" -> {
                if (operation.equals("discardProduct")) allow(role, "4공정관리자", "5공정관리자");
                else allow(role, "5공정관리자");
            }
            case "Process_execution_Controller" -> {
                if ("true".equalsIgnoreCase(request.getHeader("X-Simulation-Request"))) forbidden();
                if (operation.equals("save")) allow(role, "1공정관리자");
                else if (operation.equals("approveBulkQc")) allow(role, "4공정관리자");
                else if (operation.equals("discardBulkQc")) allow(role, "4공정관리자", "5공정관리자");
                else if (operation.equals("finish") || operation.equals("resume")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                    try { allowExecution(role, Long.valueOf(variables == null ? "" : variables.get("execution_id"))); }
                    catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 실행번호를 입력해 주세요."); }
                } else forbidden();
            }
            // 알람은 기존 Service가 본인 공정·본인 계정·최고관리자 전용 알림 권한을 상세 검사한다.
            case "Anomaly_event_Controller" -> { }
            default -> forbidden();
        }
        return true;
    }

    // 생성기가 사용하는 작업만 허용한다. 규칙 수정·승인·폐기는 내부 호출에도 허용하지 않는다.
    private boolean internalOperation(String controller, String operation) {
        return switch (controller) {
            case "Users_Controller" -> operation.equals("findAll");
            case "Batches_Controller" -> Set.of("findOne", "save", "update").contains(operation);
            case "Material_dispensing_Controller", "Bulk_qc_Controller", "Filling_packaging_Controller" -> Set.of("findOne", "save").contains(operation);
            case "Sensor_telemetry_Controller" -> operation.equals("save");
            case "Process_execution_Controller" -> Set.of("findOne", "finish", "resume").contains(operation);
            default -> false;
        };
    }

    // JSON 내 담당자 번호 위조와 센서의 타 공정 실행번호 사용도 차단한다.
    @Override
    public boolean supports(MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converter) { return true; }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage input, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converter) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return body;
        HttpServletRequest request = attributes.getRequest();
        if (Boolean.TRUE.equals(request.getAttribute(INTERNAL))) return body;
        if (!(request.getAttribute(ACTOR) instanceof Users_Entity user)) return body; // 공개 로그인 요청
        Integer actingId = null;
        if (body instanceof Material_dispensing_Dto dto) actingId = dto.getUserId();
        if (body instanceof Sensor_telemetry_Dto dto) {
            actingId = dto.getUserId();
            allowExecution(user.getRole(), dto.getExecution_id());
        }
        if (body instanceof Bulk_qc_Dto dto) actingId = dto.getUserId();
        if (body instanceof Filling_packaging_Save_Dto dto) actingId = dto.getUserId();
        if (body instanceof Filling_packaging_Result_Dto dto) actingId = dto.getUserId();
        if (body instanceof Process_execution_QcApproval_Dto dto) actingId = dto.getUserId();
        if (actingId != null && !actingId.equals(user.getUserId())) forbidden();
        return body;
    }

    private void allowExecution(String role, Long executionId) {
        if (executionId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "실행번호가 필요합니다.");
        var execution = executions.findById(executionId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "공정 실행이 없습니다."));
        String required = PROCESS_ROLES.get(execution.getProcess_code());
        if (required == null || !required.equals(role)) forbidden();
    }

    private static void allow(String role, String... allowed) {
        if (!Set.of(allowed).contains(role)) forbidden();
    }

    private static void forbidden() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 기능을 사용할 권한이 없습니다.");
    }
}
