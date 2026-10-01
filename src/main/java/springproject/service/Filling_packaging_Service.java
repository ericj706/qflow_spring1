package springproject.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Chart_Dto;
import springproject.model.dto.Filling_packaging_Dto;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.repository.Filling_packaging_Repository;

@Service 
@RequiredArgsConstructor 
public class Filling_packaging_Service {
    private final Filling_packaging_Repository filling_packaging_Repository;

    // 전체조회
    public List<Filling_packaging_Dto> findAll(){
        List<Filling_packaging_Entity> filling_packaging_Entities = filling_packaging_Repository.findAll();
        List<Filling_packaging_Dto> dtos = new ArrayList<>();

        for(Filling_packaging_Entity entity: filling_packaging_Entities){
            Filling_packaging_Dto dto= Filling_packaging_Dto.from(entity);
            dtos.add(dto);
        }
        return  dtos;
    }

    // 개별조회
    public Filling_packaging_Dto findOne(String pouch_id){
        Optional<Filling_packaging_Entity> optional = filling_packaging_Repository.findById(pouch_id);
        if(optional.isPresent()){
            Filling_packaging_Entity entity = optional.get();
            return Filling_packaging_Dto.from(entity);
        }
        return null;
    }

    // 차트조회

    // Filling_packaging_Service.java 내 메서드 수정/추가

    public List<Chart_Dto> getChartSummary(String groupBy, String batchId, String startDate, String endDate) {
        if ("15min".equalsIgnoreCase(groupBy) || "15분별".equalsIgnoreCase(groupBy)) {
            if (batchId == null || batchId.isEmpty()) {
                return Collections.emptyList(); // batchId가 없으면 빈 리스트 반환
            }
            return filling_packaging_Repository.find15minSummary(batchId, startDate, endDate);
    } else if ("hourly".equalsIgnoreCase(groupBy)) {
        return filling_packaging_Repository.findHourlySummary(startDate, endDate);
    } else if ("lot".equalsIgnoreCase(groupBy)) {
        return filling_packaging_Repository.findLotSummary(startDate, endDate);
    } else {
        return filling_packaging_Repository.findDailySummary(startDate, endDate);
    }
}
    
}
