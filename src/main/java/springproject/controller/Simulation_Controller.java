package springproject.controller;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springproject.service.Simulation_Service;
import springproject.service.Simulation_Service.SimulationSample;

@RestController
@RequestMapping("/mask/simulation")
@RequiredArgsConstructor
public class Simulation_Controller {
    private final Simulation_Service simulationService;

    // 현재 가상 LOT, 공정, 생성 건수와 실행 설정을 조회한다.
    @GetMapping("/status")
    public Map<String, Object> status() {
        return simulationService.status();
    }

    // 메모리에 보관된 최근 생성 데이터를 오래된 순서로 조회한다.
    @GetMapping("/samples")
    public List<SimulationSample> samples() {
        return simulationService.samples();
    }
}
