package springproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import springproject.service.Simulation_Service;

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
            스케줄러 스레드가 10초(설정값)마다 호출하여 실제 더미 데이터 생성, 공정 상태 진행, DB 저장 및 이상 감지 판정 로직을 1회 단위(1 Tick)로 구동합니다.
    */
}
