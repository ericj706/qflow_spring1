package springproject.datagenerate;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class Scheduler {
    private final Simulation_Service simulationService;

    // 설정된 간격마다 가상 공정을 한 단계 진행하고 데이터를 생성한다.
    @Scheduled(fixedDelayString = "${simulation.tick-delay-ms:10000}")
    public void generateRealtimeData() {
        simulationService.tick();
    }
    /*
        fixedDelay의 작동 방식: 
            이전 실행이 완전히 끝난 시점을 기준으로 지정된 시간(ms)만큼 대기한 후 다음 실행을 시작합니다.
        simulationService.tick() :
            스케줄러 스레드가 10초(기본값)마다 호출하여 데이터 생성·API 저장·공정 상태 제어를 1회 진행합니다.
    */
}
