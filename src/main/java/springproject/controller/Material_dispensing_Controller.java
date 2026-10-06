package springproject.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Material_dispensing_Dto;
import springproject.model.dto.search.Material_dispensing_SearchDto;
import springproject.service.Material_dispensing_Service;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mask/material-dispensing")
@CrossOrigin(origins = "http://localhost:5173")
public class Material_dispensing_Controller {

    private final Material_dispensing_Service ms;


    // ==========================================
    // 전체조회 + 조건검색 + 페이징
    // ==========================================
    @GetMapping
    public Page<Material_dispensing_Dto> findAll(
            @ModelAttribute Material_dispensing_SearchDto searchDto,
            Pageable pageable) {

        return ms.search(searchDto, pageable);
    }


    // ==========================================
    // 개별조회
    // ==========================================
    @GetMapping("/{dispense_id}")
    public Material_dispensing_Dto findOne(
            @PathVariable(name = "dispense_id") String dispenseId) {

        return ms.findOne(dispenseId);
    }
}