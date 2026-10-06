package springproject.model.dto.page;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

// 목록과 페이지 정보, 검색 결과 전체의 요약 건수를 반환
public record Page_response<T> (
    List<T> content,  // 현재 페이지에 표시할 목록, 최대 20건
    int page,
    int size, // 페이지당 개수. Service에서 20으로 고정
    long totalElements,
    int totalPages,
    Map<String, Long> summary //전체·확인됨·미확인 등의 요약 건수
){
    // Spring의 Page 객체를 화면에 전달할 응답 DTO로 변환
    public static <T> Page_response<T> from(
            Page<T> result,
            Map<String, Long> summary
    ) { return new Page_response<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                summary
        );
    }
}
