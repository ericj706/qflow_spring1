package springproject.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Filling_packaging_Dto;
import springproject.service.Filling_packaging_Service;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@CrossOrigin (origins = "http://localhost:5173")
@RestController @RequestMapping ("/mask/filling-packagings")
@RequiredArgsConstructor 
public class Filling_packaging_Controller {
    private final Filling_packaging_Service filling_packaging_Service;
    
    // 전체조회
    @GetMapping("")
    public List<Filling_packaging_Dto> findAll() {
        return filling_packaging_Service.findAll();
    }
    

    // 개별조회
    @GetMapping("/{pouch_id}")
    public Filling_packaging_Dto findOne(@PathVariable (name = "pouch_id") String pouch_id) {
        return filling_packaging_Service.findOne(pouch_id);
    }
    
    
}
