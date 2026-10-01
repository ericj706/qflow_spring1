package springproject.model.dto.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor @Builder @Data 
public class Bulk_qc_SearchDto {
    @DateTimeFormat (iso = DateTimeFormat.ISO.DATE)
    private  LocalDate starDate;
    @DateTimeFormat (iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private String batchId;
    private String overallQcResult;
    private Integer userId;
    
}
