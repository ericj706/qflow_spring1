package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.service.Process_execution_Service;

@RestController
@RequestMapping("/mask/process-executions")
@RequiredArgsConstructor
@CrossOrigin (origins = "http://localhost:5173")
public class Process_execution_Controller {
    private final Process_execution_Service ps;

    // 전체조회 + 조건검색 + 페이징
    @GetMapping
    public Page_response<Process_execution_Dto> findAll(
            @ModelAttribute Process_execution_SearchDto searchDto,
            @RequestParam(name = "page", defaultValue = "0")
            int page
    ) {
        Page<Process_execution_Dto> result = ps.search(searchDto, page);
        return Page_response.from(result, Map.of());
    }

    // PK 개별조회
    @GetMapping("/{execution_id}")
    public Process_execution_Dto findOne(@PathVariable(name = "execution_id") Long execution_id) {
        return ps.findOne(execution_id);
    }
}
// Entity 직접 반환 시 연관관계 데이터까지 포함될 수 있으므로 DTO로 변환하여 반환
