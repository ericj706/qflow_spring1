package springproject.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
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
            // 프론트엔드에서 숫자로 넘어온 userId 처리 (안전하게 Integer로 변환)
            Object rawUserId = requestBody.get("userId");
            Integer userId = rawUserId != null ? Integer.valueOf(rawUserId.toString()) : null;
            String password = (String) requestBody.get("password");

            if (userId == null) {
                return ResponseEntity.badRequest().body("사용자 번호를 입력해주세요.");
            }

            // 서비스의 login 호출
            Users_Dto loginUser = us.login(userId, password, session);
            return ResponseEntity.ok(loginUser);
            
        } catch (Exception e) {
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
        String role = (String) session.getAttribute("LOGIN_USER_ROLE");

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인되어 있지 않습니다.");
        }

        // Key는 String, Value는 다양한 타입(Integer, String 등)을 담을 수 있도록 Object로 지정
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("role", role);

        return ResponseEntity.ok(userInfo);
    }
}