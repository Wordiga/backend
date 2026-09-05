package com.wordiga.wish.service;

import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentSnapshotService;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.wish.dto.WishRequest;
import com.wordiga.wish.repository.WishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishServiceUnitExceptionTest {

    @Mock
    private WishRepository wishRepository;

    @Mock
    private TourismContentDetailService tourismContentDetailService;

    @Mock
    private TourismApiClient tourismApiClient;

    @Mock
    private com.wordiga.tourism.domain.TourismContentSnapshotRepository snapshotRepository;

    @Mock
    private WishPreferenceCacheService preferenceCacheService;

    @Mock
    private TourismContentSnapshotService snapshotService;

    @InjectMocks
    private WishService wishService;

    @Test
    void rejectsMissingTourismContent() {
        WishRequest request = request();
        when(tourismContentDetailService.getCommonDetail("126508"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> wishService.addWish(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void rejectsContentOutsideChungnam() {
        WishRequest request = request();
        when(tourismContentDetailService.getCommonDetail("126508"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> wishService.addWish(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void propagatesTourismApiFailureWithoutSaving() {
        WishRequest request = request();
        when(tourismContentDetailService.getCommonDetail("126508"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "관광공사 API를 사용할 수 없습니다."));

        assertThatThrownBy(() -> wishService.addWish(1L, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
        org.mockito.Mockito.verify(wishRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    private WishRequest request() {
        WishRequest request = new WishRequest();
        request.setContentId("126508");
        return request;
    }
}
