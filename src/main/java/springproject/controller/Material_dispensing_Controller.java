package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Material_dispensing_Dto;
import springproject.service.Material_dispensing_Service;

@RestController 
@RequiredArgsConstructor 
@RequestMapping ("/mask/material-dispensing")
@CrossOrigin(origins = "http://localhost:5173")

public class Material_dispensing_Controller {
    private final Material_dispensing_Service ms;

    @GetMapping 
    public List<Material_dispensing_Dto> findAll(){
        return ms.findAll();
    }

    @GetMapping ("/{dispense_id}")
    public Material_dispensing_Dto findOnd(String dispenseId){
        return ms.findOne(dispenseId);
    }
}
