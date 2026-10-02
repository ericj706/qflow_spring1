package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Data_change_log_Dto;
import springproject.service.Data_change_log_Service;

@CrossOrigin (origins = "http://localhost:5173")
@RestController @RequiredArgsConstructor 
@RequestMapping ("/mask/data-change-logs")
public class Data_change_log_Controller {
    private final Data_change_log_Service data_change_log_Service;

    // 1) 개별조회
    @GetMapping ("/{change_id}")
    public Data_change_log_Dto findOne(@PathVariable (name = "change_id") Long change_id){
        return data_change_log_Service.findOne(change_id);
    }
    
    // 2) 전체조회
    @GetMapping ("")
    public List<Data_change_log_Dto> findAll(){
        return data_change_log_Service.findAll();
    }
}
