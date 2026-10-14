package springproject.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Users_Dto;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Users_Repository;

@Service 
@RequiredArgsConstructor
public class Users_Service {
    private final Users_Repository ur;

    // 전체조회
    @Transactional(readOnly = true)
    public List<Users_Dto> findAll(){
        return ur.findAll().stream()    
            .map(Users_Dto::from)
            .toList();
    }


    // 단건조회
    @Transactional(readOnly = true)
    public Users_Dto findOne(Integer userId) {
        return ur.findById(userId)
                .map(Users_Dto::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 ID"));
    }

    //로그인
    @Transactional(readOnly = true)
    public Users_Dto login(String userName, String password, jakarta.servlet.http.HttpSession session) {
        // 1. 이름으로 유저 조회
        Users_Entity user = ur.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 이름입니다."));

        // 2. 계정 활성화 여부 확인 (isActive가 true/1 인지)
        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new IllegalStateException("활성화되지 않은 계정입니다. 관리자에게 문의하세요.");
        }

        // 3. 비밀번호 검증 (DB의 passwordHash와 비교)
        if (!user.getPasswordHash().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 4. 세션에 로그인 정보 저장
        session.setAttribute("LOGIN_USER_ID", user.getUserId());
        session.setAttribute("LOGIN_USER_NAME", user.getUserName());
        session.setAttribute("LOGIN_USER_ROLE", user.getRole()); // "최고관리자" 또는 "1~5공정관리자"

        // 5. 민감정보가 제외된 기존 Users_Dto 반환
        return Users_Dto.from(user);
    }
}
