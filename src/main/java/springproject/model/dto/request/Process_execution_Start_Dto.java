package springproject.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Process_execution_Start_Dto {
    private String batchId; // 공정을 시작할
    private String processCode; // 시작할 공정 
    
}
