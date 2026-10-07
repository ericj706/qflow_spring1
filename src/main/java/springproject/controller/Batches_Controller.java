package springproject.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Batches_Dto;
import springproject.model.dto.search.Batches_SearchDto;
import springproject.service.Batches_Service;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mask/batches")
@CrossOrigin(origins = "http://localhost:5173")

public class Batches_Controller {

    private final Batches_Service bs;


    // ==========================================
    // 전체조회 + 조건검색 + 페이징
    // ==========================================
    @GetMapping
    public Page<Batches_Dto> findAll(
            @ModelAttribute Batches_SearchDto searchDto,
            Pageable pageable) {

        return bs.search(searchDto, pageable);
    }


    // ==========================================
    // 개별조회
    // ==========================================
    @GetMapping("/{batchId}")
    public Batches_Dto findOne(
            @PathVariable(name = "batchId") String batchId) {

        return bs.findOne(batchId);
    }
    // ==========================================
    // LOT 등록
    // ==========================================
    @PostMapping
    public Batches_Dto save(
            @RequestBody Batches_Dto dto) {

             return bs.save(dto);
       }
    // ==========================================
    // LOT 수정
    // ==========================================
    @PutMapping ("/{batchId}")
    public Batches_Dto update(
            @PathVariable(name = "batchId") String batchId,
            @RequestBody Batches_Dto dto) {

        return bs.update(batchId, dto);
    }
}