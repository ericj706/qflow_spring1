package springproject.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.service.Bulk_qc_Service;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController @RequestMapping ("/mask/bulk-qc")
@RequiredArgsConstructor 
public class Bulk_qc_Controller {
    private final Bulk_qc_Service bulk_qc_Service;

    // 전체 조회
    @GetMapping("")
    public List<Bulk_qc_Dto> findAll() {
        return bulk_qc_Service.findAll();
    }
    

    // 개별 조회 (PK) 
    @GetMapping("/{qc_id}")
    public Bulk_qc_Dto findOne(@PathVariable (name = "qc_id") String qc_id) {
        return bulk_qc_Service.findOne(qc_id);
    }
    

    
}
