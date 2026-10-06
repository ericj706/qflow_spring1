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
import springproject.model.dto.Sensor_telemetry_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Sensor_telemetry_SearchDto;
import springproject.service.Sensor_telemetry_Service;

@RestController 
@RequestMapping("/mask/sensor-telemetries")
@RequiredArgsConstructor 
@CrossOrigin (origins = "http://localhost:5173")
public class Sensor_telemetry_Controller {
    private final Sensor_telemetry_Service ss;

    // 전체조회 + 조건검색 + 페이징
    @GetMapping
    public Page_response<Sensor_telemetry_Dto> findAll(
            @ModelAttribute Sensor_telemetry_SearchDto searchDto,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<Sensor_telemetry_Dto> result =ss.search(searchDto, page);
        return Page_response.from(result, Map.of());
    }

    // PK로 개별조회
    @GetMapping("/{sensor_id}")
    public Sensor_telemetry_Dto findOne(@PathVariable(name = "sensor_id") Long sensor_id) {
        return ss.findOne(sensor_id);
    }
}
