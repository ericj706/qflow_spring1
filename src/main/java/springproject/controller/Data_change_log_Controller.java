package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Data_change_log_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Data_change_log_SearchDto;
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
    
    // 2) 전체조회 + 조건검색 + 페이징
    @GetMapping
    public Page_response<Data_change_log_Dto> findAll(
            @ModelAttribute Data_change_log_SearchDto searchDto,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<Data_change_log_Dto> result = data_change_log_Service.search(searchDto, page);
        return Page_response.from(result, Map.of());
    }
}
