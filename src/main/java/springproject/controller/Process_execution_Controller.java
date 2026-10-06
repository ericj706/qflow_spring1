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
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.service.Process_execution_Service;

@RestController
@RequestMapping("/mask/process-executions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class Process_execution_Controller {

    private final Process_execution_Service ps;

    // 전체조회 + 조건검색 + 페이징
    @GetMapping
    public Page<Process_execution_Dto> findAll(
            @ModelAttribute Process_execution_SearchDto searchDto,
            Pageable pageable) {

        return ps.search(searchDto, pageable);
    }


    // PK로 개별조회
    @GetMapping("/{execution_id}")
    public Process_execution_Dto findOne(
            @PathVariable(name = "execution_id") Long execution_id) {

        return ps.findOne(execution_id);
    }
}