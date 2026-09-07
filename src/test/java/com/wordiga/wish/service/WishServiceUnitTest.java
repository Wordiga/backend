package com.wordiga.wish.service;

import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentSnapshotService;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.SigunguItem;
import com.wordiga.wish.dto.WishRequest;
import com.wordiga.wish.dto.WishResponse;
import com.wordiga.wish.repository.WishRepository;
import com.wordiga.wish.Wish;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishServiceUnitTest {

    @Mock
    private WishRepository wishRepository;

    @Mock
    private TourismContentDetailService tourismContentDetailService;

    @Mock
    private TourismApiClient tourismApiClient;

    @Mock
    private TourismContentSnapshotRepository snapshotRepository;

    @Mock
    private WishPreferenceCacheService preferenceCacheService;

    @Mock
    private TourismContentSnapshotService snapshotService;

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

        when(tourismContentDetailService.getCommonDetail("126508")).thenReturn(content);
        SigunguItem sigungu = new SigunguItem();
        sigungu.setCode("200");
        sigungu.setName("아산시");
        when(tourismApiClient.fetchSigunguList()).thenReturn(List.of(sigungu));
        when(snapshotRepository.save(any(TourismContentSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(wishRepository.save(any(Wish.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishResponse response = wishService.addWish(1L, request);

        ArgumentCaptor<Wish> captor = ArgumentCaptor.forClass(Wish.class);
        verify(wishRepository).save(captor.capture());
        assertThat(captor.getValue().getFolderName()).isEqualTo("아산시");
        assertThat(response.getLDongSignguCd()).isEqualTo("200");
        assertThat(response.getMapx()).isEqualByComparingTo(new BigDecimal("126.9891281"));
        assertThat(response.getMapy()).isEqualByComparingTo(new BigDecimal("36.8051452"));
    }

    @Test
    void returnsExistingWishWithoutCallingTourismApi() {
        Wish existing = Wish.create(
                1L, snapshot("126508", "현충사", null), "아산시");
        WishRequest request = new WishRequest();
        request.setContentId("126508");
        when(wishRepository.existsByMemberIdAndContent_ContentId(1L, "126508")).thenReturn(true);
        when(wishRepository.findByMemberIdAndContent_ContentId(1L, "126508"))
                .thenReturn(Optional.of(existing));

        WishResponse response = wishService.addWish(1L, request);

        assertThat(response.getTitle()).isEqualTo("현충사");
        verifyNoInteractions(tourismContentDetailService);
        verify(wishRepository, never()).save(any());
    }

    @Test
    void usesDefaultFolderAndIgnoresInvalidCoordinates() {
        WishRequest request = new WishRequest();
        request.setContentId("126508");
        ContentDetailDto content = new ContentDetailDto();
        content.setContentid("126508");
        content.setTitle("미분류 장소");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd("999");
        content.setMapx("invalid");
        content.setMapy("");
        when(tourismContentDetailService.getCommonDetail("126508")).thenReturn(content);
        when(snapshotRepository.save(any(TourismContentSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(wishRepository.save(any(Wish.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishResponse response = wishService.addWish(1L, request);

        assertThat(response.getFolderName()).isEqualTo("기본 위시리스트");
        assertThat(response.getMapx()).isNull();
        assertThat(response.getMapy()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = " ")
    void usesDefaultFolderForBlankRegionAndNullCoordinates(String sigunguCode) {
        WishRequest request = new WishRequest();
        request.setContentId("blank");
        ContentDetailDto content = new ContentDetailDto();
        content.setContentid("blank");
        content.setTitle("미분류");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd(sigunguCode);
        when(tourismContentDetailService.getCommonDetail("blank")).thenReturn(content);
        when(snapshotRepository.save(any(TourismContentSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(wishRepository.save(any(Wish.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishResponse response = wishService.addWish(1L, request);

        assertThat(response.getFolderName()).isEqualTo("기본 위시리스트");
        assertThat(response.getMapx()).isNull();
    }

    @Test
    void delegatesFolderQueriesAndDelete() {
        Wish wish = Wish.create(
                1L, snapshot("126508", "현충사", "https://example.com/latest.jpg"), "아산시");
        when(wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(wish));
        when(wishRepository.findByMemberIdAndFolderNameOrderByCreatedAtDescIdDesc(1L, "아산시"))
                .thenReturn(List.of(wish));

        assertThat(wishService.getWishFolders(1L)).singleElement()
                .satisfies(folder -> {
                    assertThat(folder.getCount()).isEqualTo(1);
                    assertThat(folder.getThumbnailUrl()).isEqualTo("https://example.com/latest.jpg");
                });
        assertThat(wishService.getWishesByFolder(1L, "아산시")).hasSize(1);
        wishService.removeWish(1L, "126508");

        verify(wishRepository).deleteByMemberIdAndContent_ContentId(1L, "126508");
    }

    private TourismContentSnapshot snapshot(String contentId, String title, String image) {
        return TourismContentSnapshot.builder()
                .contentId(contentId)
                .contentTypeId("12")
                .title(title)
                .firstimage(image)
                .sigunguCode("200")
                .sigunguName("아산시")
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }
}
