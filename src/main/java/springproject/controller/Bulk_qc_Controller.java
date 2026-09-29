package springproject.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.service.Bulk_qc_Service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin (origins = "http://localhost:5173")
@RestController @RequestMapping ("/mask/bulk-qc")
@RequiredArgsConstructor 
public class Bulk_qc_Controller {
    private final Bulk_qc_Service bulk_qc_Service;

    // 전체조회 + 조건검색 + 페이징
    @GetMapping
    public BulkQcPageResponse findAll(
            @RequestParam(name = "batchId", required = false)
            String batchId,

            @RequestParam(name = "userId", required = false)
            Integer userId,

            @RequestParam(name = "page", defaultValue = "0")
            int page,

            @RequestParam(name = "size", defaultValue = "10")
            int size) {

        Page<Bulk_qc_Dto> result =
                bulk_qc_Service.search(batchId, userId, page, size);

        return new BulkQcPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // 목록과 페이지 정보를 담는 응답 DTO
    public record BulkQcPageResponse(
            List<Bulk_qc_Dto> content, 
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
    

    // 개별 조회 (PK) 
    @GetMapping("/{qc_id}")
    public Bulk_qc_Dto findOne(@PathVariable (name = "qc_id") String qc_id) {
        return bulk_qc_Service.findOne(qc_id);
    }
    

    
}
