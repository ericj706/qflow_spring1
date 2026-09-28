package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.entity.Process_execution_Entity;
import springproject.service.Process_execution_Service;

@RestController 
@RequestMapping("/mask/process-executions")
@RequiredArgsConstructor 

public class Process_execution_Controller {

    private final Process_execution_Service ps;
    
    // 전체조회
    @GetMapping 
    public List<Process_execution_Entity> findAll(){
        return ps.findAll();
    }

    // PK로 개별조회
     @GetMapping("/findOne")
    public Process_execution_Entity findOne(Long execution_id) {
        return ps.findOne(execution_id);
    }
}
