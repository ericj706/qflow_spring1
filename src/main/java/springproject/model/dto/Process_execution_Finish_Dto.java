package springproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Process_execution_Finish_Dto {
    private  Process_execution_Dto finishedExecution; // 종료된 공정
    private Process_execution_Dto nextExecution; // 실행할 다음 공정, 시작되지 않으면 null
    
}
