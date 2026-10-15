package springproject.controller;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springproject.datagenerate.Simulation_Service;
import springproject.datagenerate.Simulation_Service.SimulationSample;

@RestController
@RequestMapping("/mask/simulation")
@RequiredArgsConstructor
public class Simulation_Controller {
    private final Simulation_Service simulationService;

    // 생성기를 시작하거나 중지된 저장 요청을 재시도한다.
    @PostMapping("/start")
    public Map<String, Object> start() { return simulationService.start(); }

    // 현재 LOT·공정은 유지하고 추가 데이터 생성을 중지한다.
    @PostMapping ("/stop")
    public Map<String, Object> stop() { return simulationService.stop(); }

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
