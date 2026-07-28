package com.wordiga.service;

import com.wordiga.domain.Wish;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.wish.WishRequest;
import com.wordiga.dto.wish.WishResponse;
import com.wordiga.repository.WishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    private WishRepository wishRepository;

    @Mock
    private WorkshopDetailService workshopDetailService;

    @InjectMocks
    private WishService wishService;

    @Test
    void addWishUsesTourismContentForFolderAndCoordinates() {
        WishRequest request = new WishRequest();
        request.setContentId("126508");

        ContentDetailDto content = new ContentDetailDto();
        content.setContentid("126508");
        content.setContenttypeid("12");
        content.setTitle("현충사");
        content.setFirstimage("https://example.com/main.jpg");
        content.setAddr1("충청남도 아산시 염치읍 현충사길 126");
        content.setMapx("126.9891281");
        content.setMapy("36.8051452");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd("200");

        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(content);
        when(wishRepository.save(any(Wish.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishResponse response = wishService.addWish(1L, request);

        ArgumentCaptor<Wish> captor = ArgumentCaptor.forClass(Wish.class);
        verify(wishRepository).save(captor.capture());
        assertThat(captor.getValue().getFolderName()).isEqualTo("아산시");
        assertThat(response.getLDongSignguCd()).isEqualTo("200");
        assertThat(response.getMapx()).isEqualByComparingTo(new BigDecimal("126.9891281"));
        assertThat(response.getMapy()).isEqualByComparingTo(new BigDecimal("36.8051452"));
    }
}
