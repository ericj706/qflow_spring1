package springproject;

import java.lang.reflect.*;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.server.ResponseStatusException;
import springproject.config.*;
import springproject.model.dto.*;
import springproject.model.entity.*;
import springproject.model.repository.*;

/** DB·서버를 시작하지 않고 실제 공통 권한 정책을 검증한다. */
public class AccessPolicyCheck {
    private static final String[] ROLES = {"1공정관리자", "2공정관리자", "3공정관리자", "4공정관리자", "5공정관리자", "최고관리자"};
    private static final String[] PROCESSES = {"원료 칭량", "가열/혼합", "냉각", "벌크 검사", "최종 포장 검사"};
    private static final Map<Integer, Users_Entity> users = new HashMap<>();
    private static final SimulationRequestAuth internalAuth = new SimulationRequestAuth();
    private static ApiAccessPolicy policy;
    private static int checks;

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static class Request {
        final Map<String, Object> attributes = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        final HttpServletRequest servlet;
        Request(Integer userId, String verb, long executionId) {
            HttpSession session = userId == null ? null : proxy(HttpSession.class, (p, m, a) ->
                m.getName().equals("getAttribute") ? ("LOGIN_USER_ID".equals(a[0]) ? userId : "최고관리자") : null);
            attributes.put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("execution_id", String.valueOf(executionId)));
            servlet = proxy(HttpServletRequest.class, (p, m, a) -> switch (m.getName()) {
                case "getMethod" -> verb;
                case "getSession" -> session;
                case "getHeader" -> headers.get(a[0]);
                case "getAttribute" -> attributes.get(a[0]);
                case "setAttribute" -> { attributes.put((String)a[0], a[1]); yield null; }
                case "removeAttribute" -> attributes.remove(a[0]);
                case "getAttributeNames" -> Collections.enumeration(attributes.keySet());
                default -> m.getReturnType() == boolean.class ? false : null;
            });
        }
    }

    private static HandlerMethod handler(String controller, String operation) throws Exception {
        Class<?> type = Class.forName("springproject.controller." + controller);
        Constructor<?> constructor = type.getConstructors()[0];
        Object bean = constructor.newInstance(new Object[constructor.getParameterCount()]);
        Method method = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().equals(operation)).findFirst().orElseThrow();
        return new HandlerMethod(bean, method);
    }

    private static void expect(Request request, String controller, String operation, int status) throws Exception {
        int actual = 200;
        try { policy.preHandle(request.servlet, null, handler(controller, operation)); }
        catch (ResponseStatusException e) { actual = e.getStatusCode().value(); }
        if (actual != status) throw new AssertionError(controller + "." + operation + ": expected " + status + ", got " + actual);
        checks++;
    }

    private static void body(Request request, Object value, int status) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request.servlet));
        int actual = 200;
        try { policy.afterBodyRead(value, null, null, value.getClass(), null); }
        catch (ResponseStatusException e) { actual = e.getStatusCode().value(); }
        finally { RequestContextHolder.resetRequestAttributes(); }
        if (actual != status) throw new AssertionError("body authority: " + actual + " != " + status);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        for (int i=0; i<ROLES.length; i++) users.put(i+1, Users_Entity.builder().userId(i+1).userName("manager"+(i+1)).role(ROLES[i]).isActive(true).build());
        Users_Repository userRepo = proxy(Users_Repository.class, (p,m,a) -> m.getName().equals("findById") ? Optional.ofNullable(users.get(a[0])) : null);
        Process_execution_Repository executionRepo = proxy(Process_execution_Repository.class, (p,m,a) -> {
            long id = (Long)a[0];
            return id > 0 && id <= 5 ? Optional.of(Process_execution_Entity.builder().execution_id(id).process_code(PROCESSES[(int)id-1]).build()) : Optional.empty();
        });
        policy = new ApiAccessPolicy(userRepo, executionRepo, internalAuth);
        Field origin = ApiAccessPolicy.class.getDeclaredField("frontendOrigin"); origin.setAccessible(true); origin.set(policy,"http://localhost:5173");

        expect(new Request(null,"POST",1),"Users_Controller","login",200);
        expect(new Request(null,"GET",1),"Batches_Controller","findAll",401);
        expect(new Request(null,"PUT",1),"Anomaly_rule_Controller","update",401);
        for(int id=1;id<=6;id++) {
            expect(new Request(id,"GET",1),"Batches_Controller","findAll",200);
            expect(new Request(id,"GET",1),"Anomaly_rule_Controller","findAll",id==6?200:403);
            expect(new Request(id,"PUT",1),"Anomaly_rule_Controller","update",id==6?200:403);
            expect(new Request(id,"POST",1),"Simulation_Controller","start",id==6?200:403);
            expect(new Request(id,"GET",1),"Simulation_Controller","status",200);
            expect(new Request(id,"POST",1),"Process_execution_Controller","save",id==1?200:403);
            expect(new Request(id,"PATCH",4),"Process_execution_Controller","approveBulkQc",id==4?200:403);
            expect(new Request(id,"PATCH",5),"Filling_packaging_Controller","updateResult",id==5?200:403);
            for(int executionId=1;executionId<=5;executionId++) {
                expect(new Request(id,"PATCH",executionId),"Process_execution_Controller","finish",id==executionId?200:403);
                expect(new Request(id,"PATCH",executionId),"Process_execution_Controller","resume",id==executionId?200:403);
            }
        }
        // 세션의 가짜 역할, 비활성·삭제된 계정도 서버 DB 검증을 우회하지 못한다.
        expect(new Request(2,"PUT",1),"Anomaly_rule_Controller","update",403);
        users.get(6).setIsActive(false);expect(new Request(6,"GET",1),"Users_Controller","findAll",403);users.get(6).setIsActive(true);
        expect(new Request(99,"GET",1),"Users_Controller","findAll",401);
        Request sensor = new Request(2,"POST",2);expect(sensor,"Sensor_telemetry_Controller","save",200);
        Sensor_telemetry_Dto value = new Sensor_telemetry_Dto();value.setExecution_id(3L);value.setUserId(2);body(sensor,value,403);
        value.setExecution_id(2L);value.setUserId(3);body(sensor,value,403);
        value.setUserId(2);body(sensor,value,200);
        Request forged = new Request(2,"PATCH",2);forged.headers.put("X-Simulation-Request","true");expect(forged,"Process_execution_Controller","finish",403);
        Request anonymousForged = new Request(null,"PATCH",2);anonymousForged.headers.put("X-Simulation-Request","true");anonymousForged.headers.put("X-Simulation-Token","guessed");expect(anonymousForged,"Process_execution_Controller","finish",401);
        HttpHeaders internalHeaders = new HttpHeaders();internalAuth.addHeader(internalHeaders);
        Request internal = new Request(null,"PATCH",2);internalHeaders.forEach((key,values)->internal.headers.put(key,values.get(0)));internal.headers.put("X-Simulation-Request","true");
        expect(internal,"Process_execution_Controller","finish",200);
        expect(internal,"Anomaly_rule_Controller","update",403);
        expect(internal,"Process_execution_Controller","approveBulkQc",403);
        Request staleToken = new Request(null,"PATCH",2);new SimulationRequestAuth().addHeader(internalHeaders);internalHeaders.forEach((key,values)->staleToken.headers.put(key,values.get(0)));expect(staleToken,"Process_execution_Controller","finish",401);
        Request foreign = new Request(6,"POST",1);foreign.headers.put("Origin","http://foreign.invalid");expect(foreign,"Simulation_Controller","start",403);
        System.out.println("PASS: " + checks + " access checks; six roles, anonymous/disabled users, process IDs, actor spoofing, internal authentication and cross-origin writes.");
    }
}
