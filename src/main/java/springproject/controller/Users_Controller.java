package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Users_Dto;
import springproject.service.Users_Service;

@RestController 
@RequiredArgsConstructor 
@RequestMapping ("/mask/users")
@CrossOrigin (origins = "http://localhost:5173")
public class Users_Controller {
    private final Users_Service us;

    // 전체조회
    @GetMapping ("")
    public List<Users_Dto> findAll(){
        return us.findAll();
    }

    // 개별조회
    @GetMapping ("/{userId}")
    public Users_Dto findOne(@PathVariable(name = "userId") Integer userId){
        return us.findOne(userId);
    }
}
