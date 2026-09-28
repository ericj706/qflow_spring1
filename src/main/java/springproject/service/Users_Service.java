package springproject.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Users_Dto;
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
}
