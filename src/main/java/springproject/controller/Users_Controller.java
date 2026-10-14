package springproject.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import springproject.model.dto.Users_Dto;
import springproject.service.Users_Service;

@RestController 
@RequiredArgsConstructor 
@RequestMapping("/mask/users")
public class Users_Controller {

    private final Users_Service us;

    // 전체조회
    @GetMapping("")
    public List<Users_Dto> findAll(){
        return us.findAll();
    }

    // 개별조회
    @GetMapping("/{userId}")
    public Users_Dto findOne(@PathVariable(name = "userId") Integer userId){
        return us.findOne(userId);
    }

    // ── 로그인 API ──
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> requestBody, HttpSession session) {
        try {
            String userIdValue = requestBody.get("userId");
            if (userIdValue == null || userIdValue.isBlank()) {
                return ResponseEntity.badRequest().body("사원 번호를 입력해 주세요.");
            }
            Integer userId = Integer.valueOf(userIdValue);
            String password = requestBody.get("password");

            // 서비스의 login 호출 (세션에 정보 저장 및 민감정보가 제외된 DTO 반환)
            Users_Dto loginUser = us.login(userId, password, session);
            return ResponseEntity.ok(loginUser);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── 로그아웃 API ──
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // 세션 무효화
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    // ── 현재 로그인된 사용자 확인 API ──
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("LOGIN_USER_ID");
        String userName = (String) session.getAttribute("LOGIN_USER_NAME");
        String role = (String) session.getAttribute("LOGIN_USER_ROLE");

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인되어 있지 않습니다.");
        }

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("userName", userName);
        userInfo.put("role", role);

        return ResponseEntity.ok(userInfo);
    }
}
