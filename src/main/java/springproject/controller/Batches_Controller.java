package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Batches_Dto;
import springproject.service.Batches_Service;

@RestController 
@RequiredArgsConstructor 
@RequestMapping ("/mask/batches")
@CrossOrigin (origins = "http://localhost:5173/")
public class Batches_Controller {
    private final Batches_Service bs;

    // 전체조회
    @GetMapping 
    public List<Batches_Dto> findAll(){
        return bs.findAll();
    }

    // 개별조회
    @GetMapping ("/{batchId}")
    public Batches_Dto findOne(String batchId){
        return bs.findOne(batchId);
    }
}
