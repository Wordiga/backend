package com.wordiga.service;

import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.wish.WishRequest;
import com.wordiga.repository.WishRepository;
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
    private WorkshopDetailService workshopDetailService;

    @InjectMocks
    private WishService wishService;

    @Test
    void rejectsMissingTourismContent() {
        WishRequest request = request();
        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(null);

        assertThatThrownBy(() -> wishService.addWish(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void rejectsContentOutsideChungnam() {
        WishRequest request = request();
        ContentDetailDto content = new ContentDetailDto();
        content.setLDongRegnCd("11");
        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(content);

        assertThatThrownBy(() -> wishService.addWish(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
    }

    private WishRequest request() {
        WishRequest request = new WishRequest();
        request.setContentId("126508");
        return request;
    }
}
