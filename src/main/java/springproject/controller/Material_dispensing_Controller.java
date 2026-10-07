package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Material_dispensing_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Material_dispensing_SearchDto;
import springproject.service.Material_dispensing_Service;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mask/material-dispensing")
@CrossOrigin(origins = "http://localhost:5173")
public class Material_dispensing_Controller {

    private final Material_dispensing_Service ms;

     // 전체조회 + 조건검색 + 페이징
    @GetMapping
    public Page_response<Material_dispensing_Dto> findAll(
            @ModelAttribute Material_dispensing_SearchDto searchDto,
            // 검색조건 DTO와 별도로 받는 페이지 번호
            @RequestParam(name = "page", defaultValue = "0")
            int page
    ) {
        Page<Material_dispensing_Dto> result = ms.search(searchDto, page);
        return Page_response.from(result, Map.of());
    }


    // ==========================================
    // 개별조회
    // ==========================================
    @GetMapping("/{dispense_id}")
    public Material_dispensing_Dto findOne(
            @PathVariable(name = "dispense_id") String dispenseId) {

        return ms.findOne(dispenseId);
    }

    // ==========================================
    // 원료 칭량 등록
    // ==========================================
    @PostMapping
    public Material_dispensing_Dto save(
            @RequestBody Material_dispensing_Dto dto) {

        return ms.save(dto);
    }

    // ==========================================
    // 원료 칭량 수정
    // ==========================================
   @PutMapping("/{dispenseId}")
    public Material_dispensing_Dto update(
            @PathVariable(name = "dispenseId") String dispenseId,
            @RequestBody Material_dispensing_Dto dto) {

        return ms.update(dispenseId, dto);
    }
}